// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.retry

import com.fincore.core.Currency
import com.fincore.core.Money
import com.fincore.core.PaymentId
import com.fincore.payments.application.PaymentOrchestrator
import com.fincore.payments.application.PaymentService
import com.fincore.payments.domain.Payment
import com.fincore.payments.domain.enum.PaymentStatus
import com.fincore.payments.infrastructure.persistence.PaymentEntity
import com.fincore.payments.infrastructure.persistence.PaymentPersistenceAdapter
import com.fincore.payments.infrastructure.persistence.PaymentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID

class PaymentRetryServiceTest {
    private val paymentRepository = mockk<PaymentRepository>()
    private val orchestrator = mockk<PaymentOrchestrator>(relaxed = true)
    private val paymentService = mockk<PaymentService>(relaxed = true)
    private val service =
        PaymentRetryService(
            paymentRepository,
            PaymentPersistenceAdapter(),
            orchestrator,
            paymentService,
            PaymentRetryProperties(enabled = true, stuckAfter = Duration.ofMinutes(5), maxAge = Duration.ofHours(1)),
        )

    @Test
    fun `should resume a payment stuck within the retry window`() {
        every { paymentRepository.findByStatusAndCreatedAtBefore(any(), any()) } returns listOf(stuck(Duration.ofMinutes(10)))

        service.retryStuck()

        verify { orchestrator.resume(any()) }
        verify(exactly = 0) { paymentService.markFailed(any(), any()) }
    }

    @Test
    fun `should fail a payment past the retry deadline without resuming`() {
        every { paymentRepository.findByStatusAndCreatedAtBefore(any(), any()) } returns listOf(stuck(Duration.ofHours(2)))

        service.retryStuck()

        verify { paymentService.markFailed(any(), "retry deadline exceeded") }
        verify(exactly = 0) { orchestrator.resume(any()) }
    }

    @Test
    fun `should continue the batch when resuming one payment throws`() {
        every { paymentRepository.findByStatusAndCreatedAtBefore(any(), any()) } returns
            listOf(stuck(Duration.ofMinutes(10)), stuck(Duration.ofMinutes(20)))
        every { orchestrator.resume(any()) } throws RuntimeException("bank down") andThen
            Payment(PaymentId.generate(), Money(BigDecimal("100.00"), Currency.USD), "order-1", PaymentStatus.SUBMITTED)

        service.retryStuck()

        verify(exactly = 2) { orchestrator.resume(any()) }
    }

    private fun stuck(age: Duration): PaymentEntity =
        PaymentEntity(
            UUID.randomUUID(),
            "order-1",
            BigDecimal("100.00"),
            "USD",
            PaymentStatus.SCREENING,
            Instant.now().minus(age),
            0L,
        )
}
