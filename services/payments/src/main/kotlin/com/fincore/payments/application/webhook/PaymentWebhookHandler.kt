// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.webhook

import com.fincore.core.PaymentId
import com.fincore.payments.application.PaymentService
import com.fincore.payments.domain.enum.PaymentStatus
import com.fincore.payments.infrastructure.persistence.PaymentRepository
import com.fincore.payments.infrastructure.persistence.ProcessedWebhookRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Handles a verified inbound payment webhook: correlate by provider reference, dedup by delivery id, and settle/fail
 * the payment. All database work commits in one transaction; there is no outbound call. A late or conflicting webhook
 * (illegal transition) is ignored as reconciliation, not an error.
 */
@Service
class PaymentWebhookHandler(
    private val verifier: WebhookSignatureVerifier,
    private val paymentRepository: PaymentRepository,
    private val processedWebhookRepository: ProcessedWebhookRepository,
    private val paymentService: PaymentService,
) {
    @Transactional
    fun handle(
        rawPayload: String,
        signatureHex: String,
        notification: PaymentWebhookNotification,
    ): WebhookResult {
        if (!verifier.verify(rawPayload, signatureHex)) {
            throw WebhookSignatureException("invalid webhook signature")
        }
        val entity =
            paymentRepository.findByProviderReference(notification.providerReference)
                ?: return WebhookResult.Ignored("no matching payment")
        if (!entity.status.canTransitionTo(targetStatus(notification.outcome))) {
            return WebhookResult.Ignored("payment not in a settleable state")
        }
        return dedupAndApply(PaymentId(entity.id), notification)
    }

    private fun dedupAndApply(
        id: PaymentId,
        notification: PaymentWebhookNotification,
    ): WebhookResult {
        if (processedWebhookRepository.insertIfAbsent(notification.deliveryId) == 0) {
            return WebhookResult.Duplicate
        }
        when (notification.outcome) {
            WebhookOutcome.SETTLED -> paymentService.markSettled(id)
            WebhookOutcome.FAILED -> paymentService.markFailed(id, FAILURE_REASON)
        }
        return WebhookResult.Processed
    }

    private fun targetStatus(outcome: WebhookOutcome): PaymentStatus =
        when (outcome) {
            WebhookOutcome.SETTLED -> PaymentStatus.SETTLED
            WebhookOutcome.FAILED -> PaymentStatus.FAILED
        }

    private companion object {
        const val FAILURE_REASON = "provider reported failure"
    }
}
