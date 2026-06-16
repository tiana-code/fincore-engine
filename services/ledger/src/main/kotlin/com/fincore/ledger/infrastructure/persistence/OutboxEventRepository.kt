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
}
