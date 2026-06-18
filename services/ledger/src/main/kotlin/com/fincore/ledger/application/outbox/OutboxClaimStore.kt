// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application.outbox

import com.fincore.eventbus.outbox.ClaimedEvent
import com.fincore.eventbus.outbox.OutboxStore
import com.fincore.events.OutboxStatus
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Ledger-backed [OutboxStore]. Claim and settle are separate short transactions so the broker publish
 * (in the dispatcher) never runs inside a DB transaction.
 */
@Component
class OutboxClaimStore(
    private val repository: OutboxEventRepository,
) : OutboxStore {
    @Transactional
    override fun claim(
        maxAttempts: Int,
        leaseTimeout: Duration,
        batchSize: Int,
    ): List<ClaimedEvent> {
        val now = Instant.now()
        val orphanDeadline = now.minus(leaseTimeout)
        return repository.lockClaimableBatch(maxAttempts, orphanDeadline, batchSize).map { entity ->
            entity.status = OutboxStatus.PUBLISHING
            entity.leasedAt = now
            ClaimedEvent(
                id = entity.id,
                aggregateType = entity.aggregateType,
                aggregateId = entity.aggregateId,
                eventType = entity.eventType,
                payload = entity.payload,
                attempts = entity.attempts,
            )
        }
    }

    @Transactional
    override fun markPublished(id: UUID) {
        repository.markPublished(id, OutboxStatus.PUBLISHED, Instant.now())
    }

    @Transactional
    override fun markFailed(
        id: UUID,
        attempts: Int,
        maxAttempts: Int,
        error: String?,
    ) {
        val status = if (attempts >= maxAttempts) OutboxStatus.PERMANENTLY_FAILED else OutboxStatus.FAILED
        repository.markSettledFailure(id, status, attempts, error)
    }
}
