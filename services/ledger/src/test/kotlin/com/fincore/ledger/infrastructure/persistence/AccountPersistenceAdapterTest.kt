// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.ledger.domain.Account
import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.AccountType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class AccountPersistenceAdapterTest {
    private val adapter = AccountPersistenceAdapter()
    private val now = Instant.parse("2026-06-05T12:00:00Z")

    @Test
    fun `should map entity to domain preserving id currency and enums`() {
        val id = UUID.randomUUID()
        val entity =
            AccountEntity(
                id = id,
                name = "Wallet",
                type = AccountType.USER_WALLET,
                currency = "USD",
                status = AccountStatus.FROZEN,
                metadata = "{}",
                version = 3,
                createdAt = now,
                createdBy = "a",
                updatedAt = now,
                updatedBy = "a",
            )

        val account = adapter.toDomain(entity)

        account.id shouldBe AccountId(id)
        account.name shouldBe "Wallet"
        account.type shouldBe AccountType.USER_WALLET
        account.currency shouldBe Currency.USD
        account.status shouldBe AccountStatus.FROZEN
    }

    @Test
    fun `should build a new entity with audit fields and defaults`() {
        val account = Account(AccountId.generate(), "Wallet", AccountType.FEE, Currency.EUR)

        val entity = adapter.toNewEntity(account, "auth0|operator", now)

        entity.id shouldBe account.id.value
        entity.currency shouldBe "EUR"
        entity.metadata shouldBe "{}"
        entity.version shouldBe 0
        entity.createdBy shouldBe "auth0|operator"
        entity.updatedBy shouldBe "auth0|operator"
        entity.createdAt shouldBe now
    }
}
