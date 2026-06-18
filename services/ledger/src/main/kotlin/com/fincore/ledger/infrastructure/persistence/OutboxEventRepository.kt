// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import com.fincore.events.OutboxStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface OutboxEventRepository : JpaRepository<OutboxEventEntity, UUID> {
    @Modifying(clearAutomatically = true)
    @Query("delete from OutboxEventEntity e where e.status = :status and e.publishedAt < :cutoff")
    fun deletePublishedBefore(
        @Param("status") status: OutboxStatus,
        @Param("cutoff") cutoff: Instant,
    ): Int

    @Query(
        nativeQuery = true,
        value = """
            SELECT id, aggregate_type, aggregate_id, event_type, payload, status,
                   attempts, last_error, created_at, published_at, leased_at
            FROM platform.outbox_events
            WHERE status = 'PENDING'
               OR (status = 'FAILED' AND attempts < :maxAttempts)
               OR (status = 'PUBLISHING' AND leased_at < :orphanDeadline)
            ORDER BY created_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
        """,
    )
    fun lockClaimableBatch(
        @Param("maxAttempts") maxAttempts: Int,
        @Param("orphanDeadline") orphanDeadline: Instant,
        @Param("batchSize") batchSize: Int,
    ): List<OutboxEventEntity>

    @Modifying(clearAutomatically = true)
    @Query(
        "update OutboxEventEntity e set e.status = :status, e.publishedAt = :publishedAt, e.leasedAt = null " +
            "where e.id = :id",
    )
    fun markPublished(
        @Param("id") id: UUID,
        @Param("status") status: OutboxStatus,
        @Param("publishedAt") publishedAt: Instant,
    ): Int

    @Modifying(clearAutomatically = true)
    @Query(
        "update OutboxEventEntity e set e.status = :status, e.attempts = :attempts, " +
            "e.lastError = :lastError, e.leasedAt = null where e.id = :id",
    )
    fun markSettledFailure(
        @Param("id") id: UUID,
        @Param("status") status: OutboxStatus,
        @Param("attempts") attempts: Int,
        @Param("lastError") lastError: String?,
    ): Int
}
