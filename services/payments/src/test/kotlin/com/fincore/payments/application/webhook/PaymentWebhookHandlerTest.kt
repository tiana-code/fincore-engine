// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.webhook

import com.fincore.payments.application.PaymentService
import com.fincore.payments.domain.enum.PaymentStatus
import com.fincore.payments.infrastructure.persistence.PaymentEntity
import com.fincore.payments.infrastructure.persistence.PaymentRepository
import com.fincore.payments.infrastructure.persistence.ProcessedWebhookRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class PaymentWebhookHandlerTest {
    private val verifier = mockk<WebhookSignatureVerifier>()
    private val paymentRepository = mockk<PaymentRepository>()
    private val processedWebhookRepository = mockk<ProcessedWebhookRepository>(relaxed = true)
    private val paymentService = mockk<PaymentService>(relaxed = true)
    private val handler = PaymentWebhookHandler(verifier, paymentRepository, processedWebhookRepository, paymentService)

    private val id = UUID.randomUUID()

    @Test
    fun `should reject and touch no repository when the signature is invalid`() {
        every { verifier.verify(any(), any()) } returns false

        shouldThrow<WebhookSignatureException> { handler.handle("body", "sig", notification(WebhookOutcome.SETTLED)) }

        verify(exactly = 0) { paymentRepository.findByProviderReference(any()) }
        verify(exactly = 0) { processedWebhookRepository.insertIfAbsent(any()) }
    }

    @Test
    fun `should ignore when no payment matches the provider reference`() {
        every { verifier.verify(any(), any()) } returns true
        every { paymentRepository.findByProviderReference("prov-1") } returns null

        handler.handle("body", "sig", notification(WebhookOutcome.SETTLED)) shouldBe WebhookResult.Ignored("no matching payment")

        verify(exactly = 0) { processedWebhookRepository.insertIfAbsent(any()) }
    }

    @Test
    fun `should ignore a conflicting webhook for an already terminal payment`() {
        every { verifier.verify(any(), any()) } returns true
        every { paymentRepository.findByProviderReference("prov-1") } returns entity(PaymentStatus.SETTLED)

        val result = handler.handle("body", "sig", notification(WebhookOutcome.SETTLED))

        (result is WebhookResult.Ignored) shouldBe true
        verify(exactly = 0) { processedWebhookRepository.insertIfAbsent(any()) }
        verify(exactly = 0) { paymentService.markSettled(any()) }
    }

    @Test
    fun `should settle a submitted payment on a fresh delivery`() {
        every { verifier.verify(any(), any()) } returns true
        every { paymentRepository.findByProviderReference("prov-1") } returns entity(PaymentStatus.SUBMITTED)
        every { processedWebhookRepository.insertIfAbsent("d-1") } returns 1

        handler.handle("body", "sig", notification(WebhookOutcome.SETTLED)) shouldBe WebhookResult.Processed

        verify { paymentService.markSettled(any()) }
    }

    @Test
    fun `should fail a submitted payment when the outcome is failed`() {
        every { verifier.verify(any(), any()) } returns true
        every { paymentRepository.findByProviderReference("prov-1") } returns entity(PaymentStatus.SUBMITTED)
        every { processedWebhookRepository.insertIfAbsent("d-1") } returns 1

        handler.handle("body", "sig", notification(WebhookOutcome.FAILED)) shouldBe WebhookResult.Processed

        verify { paymentService.markFailed(any(), any()) }
    }

    @Test
    fun `should report duplicate and not transition when the delivery was already seen`() {
        every { verifier.verify(any(), any()) } returns true
        every { paymentRepository.findByProviderReference("prov-1") } returns entity(PaymentStatus.SUBMITTED)
        every { processedWebhookRepository.insertIfAbsent("d-1") } returns 0

        handler.handle("body", "sig", notification(WebhookOutcome.SETTLED)) shouldBe WebhookResult.Duplicate

        verify(exactly = 0) { paymentService.markSettled(any()) }
    }

    private fun notification(outcome: WebhookOutcome): PaymentWebhookNotification = PaymentWebhookNotification("d-1", "prov-1", outcome)

    private fun entity(status: PaymentStatus): PaymentEntity =
        PaymentEntity(id, "order-1", BigDecimal("100.00"), "USD", status, Instant.now(), 0L, "prov-1")
}
