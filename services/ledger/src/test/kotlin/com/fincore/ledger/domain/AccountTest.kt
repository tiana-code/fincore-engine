// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.domain

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.ledger.domain.enum.AccountStatus.ACTIVE
import com.fincore.ledger.domain.enum.AccountStatus.CLOSED
import com.fincore.ledger.domain.enum.AccountStatus.FROZEN
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.exception.DomainException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test

class AccountTest {
    private fun activeAccount(
        name: String = "Test Account",
        type: AccountType = AccountType.ASSET,
        currency: Currency = Currency.USD,
    ): Account =
        Account(
            id = AccountId.generate(),
            name = name,
            type = type,
            currency = currency,
            status = ACTIVE,
        )

    @Test
    fun `should reject empty name when constructing Account`() {
        shouldThrow<DomainException> {
            activeAccount(name = "")
        }
    }

    @Test
    fun `should reject name longer than 255 characters when constructing Account`() {
        val tooLong = "x".repeat(256)
        shouldThrow<DomainException> {
            activeAccount(name = tooLong)
        }
    }

    @Test
    fun `should accept name of exactly 1 character`() {
        val account = activeAccount(name = "X")
        account.name shouldBe "X"
    }

    @Test
    fun `should accept name of exactly 255 characters`() {
        val maxName = "a".repeat(255)
        val account = activeAccount(name = maxName)
        account.name shouldBe maxName
    }

    @Test
    fun `should expose immutable type from construction`() {
        val account = activeAccount(type = AccountType.LIABILITY)
        account.type shouldBe AccountType.LIABILITY
    }

    @Test
    fun `should expose immutable currency from construction`() {
        val account = activeAccount(currency = Currency.EUR)
        account.currency shouldBe Currency.EUR
    }

    @Test
    fun `should transition from ACTIVE to FROZEN`() {
        val account = activeAccount()
        account.transitionStatus(FROZEN)
        account.status shouldBe FROZEN
    }

    @Test
    fun `should transition from FROZEN to ACTIVE`() {
        val account = activeAccount()
        account.transitionStatus(FROZEN)
        account.transitionStatus(ACTIVE)
        account.status shouldBe ACTIVE
    }

    @Test
    fun `should transition from ACTIVE to CLOSED`() {
        val account = activeAccount()
        account.transitionStatus(CLOSED)
        account.status shouldBe CLOSED
    }

    @Test
    fun `should transition from FROZEN to CLOSED`() {
        val account = activeAccount()
        account.transitionStatus(FROZEN)
        account.transitionStatus(CLOSED)
        account.status shouldBe CLOSED
    }

    @Test
    fun `should throw DomainException when transitioning CLOSED to ACTIVE`() {
        val account = activeAccount()
        account.transitionStatus(CLOSED)

        val exception =
            shouldThrow<DomainException> {
                account.transitionStatus(ACTIVE)
            }
        exception.message.shouldNotBeBlank()
    }

    @Test
    fun `should throw DomainException when transitioning CLOSED to FROZEN`() {
        val account = activeAccount()
        account.transitionStatus(CLOSED)

        shouldThrow<DomainException> {
            account.transitionStatus(FROZEN)
        }
    }

    @Test
    fun `should throw DomainException when transitioning CLOSED to CLOSED`() {
        val account = activeAccount()
        account.transitionStatus(CLOSED)

        shouldThrow<DomainException> {
            account.transitionStatus(CLOSED)
        }
    }

    @Test
    fun `should throw DomainException when transitioning ACTIVE to ACTIVE`() {
        val account = activeAccount()

        shouldThrow<DomainException> {
            account.transitionStatus(ACTIVE)
        }
    }

    @Test
    fun `should throw DomainException when transitioning FROZEN to FROZEN`() {
        val account = activeAccount()
        account.transitionStatus(FROZEN)

        shouldThrow<DomainException> {
            account.transitionStatus(FROZEN)
        }
    }

    @Test
    fun `should throw DomainException when renaming a CLOSED account`() {
        val account = activeAccount()
        account.transitionStatus(CLOSED)

        shouldThrow<DomainException> {
            account.rename("New Name")
        }
    }

    @Test
    fun `should allow renaming an ACTIVE account`() {
        val account = activeAccount(name = "Old Name")
        account.rename("New Name")
        account.name shouldBe "New Name"
    }

    @Test
    fun `should allow renaming a FROZEN account`() {
        val account = activeAccount(name = "Old Name")
        account.transitionStatus(FROZEN)
        account.rename("Frozen Name")
        account.name shouldBe "Frozen Name"
    }

    @Test
    fun `should throw DomainException when renaming a CLOSED account to empty string`() {
        val account = activeAccount()
        account.transitionStatus(CLOSED)

        shouldThrow<DomainException> {
            account.rename("")
        }
    }

    @Test
    fun `should report isPostable as true when ACTIVE`() {
        val account = activeAccount()
        account.isPostable() shouldBe true
    }

    @Test
    fun `should report isPostable as false when FROZEN`() {
        val account = activeAccount()
        account.transitionStatus(FROZEN)
        account.isPostable() shouldBe false
    }

    @Test
    fun `should report isPostable as false when CLOSED`() {
        val account = activeAccount()
        account.transitionStatus(CLOSED)
        account.isPostable() shouldBe false
    }

    @Test
    fun `should default to ACTIVE status on construction`() {
        val account = activeAccount()
        account.status shouldBe ACTIVE
    }

    @Test
    fun `should expose AccountId from id field`() {
        val id = AccountId.generate()
        val account =
            Account(
                id = id,
                name = "Test",
                type = AccountType.ASSET,
                currency = Currency.USD,
                status = ACTIVE,
            )
        account.id shouldBe id
    }
}
