// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface IdempotencyKeyRepository : JpaRepository<IdempotencyKeyEntity, String> {
    @Modifying(clearAutomatically = true)
    @Query("delete from IdempotencyKeyEntity e where e.expiresAt < :cutoff")
    fun deleteExpiredBefore(
        @Param("cutoff") cutoff: Instant,
    ): Int
}
