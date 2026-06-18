// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application.outbox

import com.fincore.events.OutboxStatus
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Transactional boundary for the dispatcher. Claim and settle are separate short transactions so the
 * broker publish (in [OutboxDispatcher]) never runs inside a DB transaction (CLAUDE.md 8.10).
 */
@Component
class OutboxClaimStore(
    private val repository: OutboxEventRepository,
) {
    @Transactional
    fun claim(
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
    fun markPublished(id: UUID) {
        repository.markPublished(id, OutboxStatus.PUBLISHED, Instant.now())
    }

    @Transactional
    fun markFailed(
        id: UUID,
        attempts: Int,
        maxAttempts: Int,
        error: String?,
    ) {
        val status = if (attempts >= maxAttempts) OutboxStatus.PERMANENTLY_FAILED else OutboxStatus.FAILED
        repository.markSettledFailure(id, status, attempts, error)
    }
}
