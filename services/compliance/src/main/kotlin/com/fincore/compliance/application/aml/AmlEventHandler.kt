// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.aml

import com.fincore.compliance.domain.enum.AmlAlertStatus
import com.fincore.compliance.infrastructure.messaging.LedgerTransactionPosted
import com.fincore.compliance.infrastructure.persistence.AmlAlertEntity
import com.fincore.compliance.infrastructure.persistence.AmlAlertRepository
import com.fincore.eventbus.consumer.IdempotentEventProcessor
import com.fincore.events.EventEnvelope
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Handles a ledger TransactionPosted event for AML: deduplicated by envelope id within one transaction, so a thrown
 * handler rolls back the claim and the event is retried. Raises an AmlAlert when the evaluator flags the transaction.
 * No external call runs inside the transaction.
 */
@Service
class AmlEventHandler(
    private val processor: IdempotentEventProcessor,
    private val evaluator: AmlEvaluator,
    private val amlAlerts: AmlAlertRepository,
) {
    @Transactional
    fun handle(envelope: EventEnvelope<LedgerTransactionPosted>) {
        processor.process(envelope.id, CONSUMER_GROUP) {
            val view = toView(envelope.data)
            val decision = evaluator.evaluate(view)
            if (decision is AmlDecision.Flagged) {
                amlAlerts.saveAndFlush(
                    AmlAlertEntity(
                        id = UUID.randomUUID(),
                        ruleKey = decision.reasonCodes.firstOrNull()?.takeIf { it.isNotBlank() } ?: DEFAULT_RULE_KEY,
                        subjectReference = view.subjectReference,
                        status = AmlAlertStatus.OPEN,
                        createdAt = Instant.now(),
                        version = 0,
                    ),
                )
            }
        }
    }

    private fun toView(payload: LedgerTransactionPosted): AmlTransactionView =
        AmlTransactionView(
            subjectReference = payload.transactionId,
            amount =
                payload.entries
                    .filter { it.direction == DEBIT }
                    .fold(BigDecimal.ZERO) { acc, line -> acc + BigDecimal(line.amount) },
            currency = payload.currency,
            occurredAt = payload.postedAt,
        )

    private companion object {
        const val CONSUMER_GROUP = "compliance-aml"
        const val DEBIT = "DEBIT"
        const val DEFAULT_RULE_KEY = "aml.rule"
    }
}
