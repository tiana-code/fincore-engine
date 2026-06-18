// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application

import com.fincore.core.Currency
import com.fincore.core.Money
import com.fincore.core.PaymentId
import com.fincore.payments.application.bank.BankPaymentRequest
import com.fincore.payments.application.bank.BankProvider
import com.fincore.payments.application.bank.BankProviderException
import com.fincore.payments.application.bank.BankSubmissionResult
import com.fincore.payments.application.screening.ScreeningDecision
import com.fincore.payments.application.screening.ScreeningEvaluator
import com.fincore.payments.domain.Payment
import com.fincore.payments.domain.enum.PaymentStatus
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class PaymentOrchestratorTest {
    private val paymentService = mockk<PaymentService>(relaxed = true)
    private val screeningEvaluator = mockk<ScreeningEvaluator>()
    private val bankProvider = mockk<BankProvider>()
    private val orchestrator = PaymentOrchestrator(paymentService, screeningEvaluator, bankProvider)

    private val id = PaymentId.generate()

    init {
        every { paymentService.screen(id) } returns payment(PaymentStatus.SCREENING)
    }

    @Test
    fun `should submit to the bank and mark submitted when approved and accepted`() {
        every { screeningEvaluator.evaluate(any()) } returns ScreeningDecision.Approve
        every { bankProvider.submit(any()) } returns BankSubmissionResult.Accepted("ref-1")

        orchestrator.process(id)

        verify { bankProvider.submit(any<BankPaymentRequest>()) }
        verify { paymentService.markSubmitted(id, "ref-1") }
    }

    @Test
    fun `should mark failed when approved but the bank rejects`() {
        every { screeningEvaluator.evaluate(any()) } returns ScreeningDecision.Approve
        every { bankProvider.submit(any()) } returns BankSubmissionResult.Rejected("declined")

        orchestrator.process(id)

        verify { paymentService.markFailed(id, "declined") }
    }

    @Test
    fun `should mark failed without calling the bank when screening declines`() {
        every { screeningEvaluator.evaluate(any()) } returns ScreeningDecision.Decline("screening declined")

        orchestrator.process(id)

        verify { paymentService.markFailed(id, "screening declined") }
        verify(exactly = 0) { bankProvider.submit(any()) }
    }

    @Test
    fun `should leave the payment screening when the bank fails technically`() {
        every { screeningEvaluator.evaluate(any()) } returns ScreeningDecision.Approve
        every { bankProvider.submit(any()) } throws BankProviderException("timeout")

        shouldThrow<BankProviderException> { orchestrator.process(id) }

        verify(exactly = 0) { paymentService.markSubmitted(any(), any()) }
        verify(exactly = 0) { paymentService.markFailed(any(), any()) }
    }

    private fun payment(status: PaymentStatus): Payment = Payment(id, Money(BigDecimal("100.00"), Currency.USD), "order-1", status)
}
