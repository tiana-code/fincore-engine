// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application

import com.fincore.payments.application.event.PaymentInitiatedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Drives orchestration once an initiated payment's transaction has committed, off the request thread. AFTER_COMMIT
 * guarantees the payment row is durably visible before [PaymentOrchestrator.process] screens and submits it (the
 * external bank call must never run inside a transaction). A [com.fincore.payments.application.bank.BankProviderException]
 * leaves the payment in SCREENING for the scheduled retry, so it is caught here rather than failing the async worker.
 */
@Component
class PaymentOrchestrationTrigger(
    private val orchestrator: PaymentOrchestrator,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("paymentOrchestrationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Suppress("TooGenericExceptionCaught") // a failed orchestration must not crash the async worker; it falls to retry
    fun onPaymentInitiated(event: PaymentInitiatedEvent) {
        try {
            orchestrator.process(event.paymentId)
        } catch (ex: Exception) {
            log
                .atWarn()
                .addKeyValue("paymentId", event.paymentId.toString())
                .setCause(ex)
                .log("orchestration after initiate failed; left for the scheduled retry")
        }
    }
}
