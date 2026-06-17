// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

interface AccountBalanceRepository : JpaRepository<AccountBalanceEntity, AccountBalanceKey> {
    fun findByKeyAccountId(accountId: UUID): List<AccountBalanceEntity>

    @Modifying(clearAutomatically = true)
    @Query(
        value =
            "INSERT INTO ledger.account_balances(account_id,currency,balance,last_posted_at,version) " +
                "VALUES (:accountId,:currency,:delta,:postedAt,0) " +
                "ON CONFLICT (account_id,currency) DO UPDATE SET " +
                "balance = account_balances.balance + EXCLUDED.balance, " +
                "last_posted_at = EXCLUDED.last_posted_at, " +
                "version = account_balances.version + 1",
        nativeQuery = true,
    )
    fun upsertBalance(
        @Param("accountId") accountId: UUID,
        @Param("currency") currency: String,
        @Param("delta") delta: BigDecimal,
        @Param("postedAt") postedAt: Instant,
    )
}
