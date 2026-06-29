// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface TransactionRepository : JpaRepository<TransactionEntity, UUID> {
    fun existsByReference(reference: String): Boolean

    @Query(
        nativeQuery = true,
        value = """
            SELECT CAST(t.id AS varchar)                                              AS id,
                   COALESCE(t.description, t.reference)                               AS label,
                   (t.reverses_id IS NOT NULL)                                        AS reversal,
                   t.posted_at                                                         AS postedat,
                   COALESCE((SELECT SUM(e.amount)
                             FROM ledger.entries e
                             WHERE e.transaction_id = t.id AND e.amount > 0), 0)      AS amount,
                   (SELECT MIN(e.currency)
                    FROM ledger.entries e
                    WHERE e.transaction_id = t.id)                                    AS currency
            FROM   ledger.transactions t
            ORDER  BY t.posted_at DESC, t.id DESC
            LIMIT  :limit
        """,
    )
    fun findRecentActivity(
        @Param("limit") limit: Int,
    ): List<TransactionActivityRow>

    @Query(
        nativeQuery = true,
        // Truncate to the UTC hour and cast back to timestamptz so the bucket maps to a
        // UTC-anchored Instant regardless of the JDBC session/JVM timezone.
        value = """
            SELECT date_trunc('hour', posted_at AT TIME ZONE 'UTC') AT TIME ZONE 'UTC' AS bucket,
                   COUNT(*)                                                            AS cnt
            FROM   ledger.transactions
            WHERE  posted_at >= :since
            GROUP  BY bucket
            ORDER  BY bucket
        """,
    )
    fun countByHourSince(
        @Param("since") since: Instant,
    ): List<HourlyTransactionCountRow>
}
