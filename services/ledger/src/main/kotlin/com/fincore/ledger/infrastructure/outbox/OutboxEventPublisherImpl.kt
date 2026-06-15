// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.events.EventEnvelope
import com.fincore.events.OutboxStatus
import com.fincore.ledger.infrastructure.persistence.OutboxEventEntity
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.util.UUID

@Component
class OutboxEventPublisherImpl(
    private val outboxRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
) : OutboxEventPublisher {
    override fun publish(
        envelope: EventEnvelope<*>,
        aggregateType: String,
        aggregateId: String,
        eventType: String,
        createdAt: Instant,
    ) {
        check(TransactionSynchronizationManager.isActualTransactionActive()) {
            "OutboxEventPublisher.publish must be called within an active transaction"
        }
        outboxRepository.saveAndFlush(
            OutboxEventEntity(
                id = UUID.randomUUID(),
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                eventType = eventType,
                payload = objectMapper.writeValueAsString(envelope),
                status = OutboxStatus.PENDING,
                createdAt = createdAt,
                publishedAt = null,
                attempts = 0,
                lastError = null,
            ),
        )
    }
}
