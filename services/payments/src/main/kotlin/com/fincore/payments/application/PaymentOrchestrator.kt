// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application

import com.fincore.core.PaymentId
import com.fincore.payments.application.bank.BankPaymentRequest
import com.fincore.payments.application.bank.BankProvider
import com.fincore.payments.application.bank.BankSubmissionResult
import com.fincore.payments.application.screening.ScreeningDecision
import com.fincore.payments.application.screening.ScreeningEvaluator
import com.fincore.payments.domain.Payment
import org.springframework.stereotype.Service

/**
 * Drives an initiated payment through screening and bank submission. NOT transactional: each persisted transition is
 * its own short transaction on [PaymentService], and the external [BankProvider.submit] runs between them, never
 * inside a transaction (CLAUDE.md 8.10). A [com.fincore.payments.application.bank.BankProviderException] from the bank
 * propagates and leaves the payment in SCREENING for the scheduled retry to re-attempt.
 */
@Service
class PaymentOrchestrator(
    private val paymentService: PaymentService,
    private val screeningEvaluator: ScreeningEvaluator,
    private val bankProvider: BankProvider,
) {
    fun process(id: PaymentId): Payment {
        val screened = paymentService.screen(id)
        return when (val decision = screeningEvaluator.evaluate(screened)) {
            ScreeningDecision.Approve -> route(id, screened)
            is ScreeningDecision.Decline -> paymentService.markFailed(id, decision.reason)
        }
    }

    private fun route(
        id: PaymentId,
        payment: Payment,
    ): Payment =
        when (val result = bankProvider.submit(toBankRequest(payment))) {
            is BankSubmissionResult.Accepted -> paymentService.markSubmitted(id, result.providerReference)
            is BankSubmissionResult.Rejected -> paymentService.markFailed(id, result.reason)
        }

    private fun toBankRequest(payment: Payment): BankPaymentRequest =
        BankPaymentRequest(payment.id.toString(), payment.amount, payment.reference)
}
