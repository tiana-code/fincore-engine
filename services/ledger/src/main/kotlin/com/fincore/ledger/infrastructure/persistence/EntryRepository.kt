// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

interface EntryRepository : JpaRepository<EntryEntity, EntryKey> {
    @Query(
        "select coalesce(sum(e.amount), 0) from EntryEntity e " +
            "where e.accountId = :accountId and e.currency = :currency and e.postedAt <= :asOf",
    )
    fun sumAmount(
        @Param("accountId") accountId: UUID,
        @Param("currency") currency: String,
        @Param("asOf") asOf: Instant,
    ): BigDecimal

    @Query("select e from EntryEntity e where e.transactionId = :transactionId order by e.key.id")
    fun findByTransactionId(
        @Param("transactionId") transactionId: UUID,
    ): List<EntryEntity>

    @Query(
        "select e from EntryEntity e where e.accountId = :accountId " +
            "and e.postedAt >= :from and e.postedAt <= :to " +
            "and (:cursorPostedAt is null or e.postedAt < :cursorPostedAt " +
            "or (e.postedAt = :cursorPostedAt and e.key.id < :cursorId)) " +
            "order by e.postedAt desc, e.key.id desc",
    )
    fun findAccountEntries(
        @Param("accountId") accountId: UUID,
        @Param("from") from: Instant,
        @Param("to") to: Instant,
        @Param("cursorPostedAt") cursorPostedAt: Instant?,
        @Param("cursorId") cursorId: UUID?,
        pageable: Pageable,
    ): List<EntryEntity>
}
