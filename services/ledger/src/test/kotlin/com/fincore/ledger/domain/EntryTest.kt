// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.domain

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.core.EntryId
import com.fincore.core.Money
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.exception.DomainException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class EntryTest {
    private val accountId = AccountId.generate()
    private val usd = Currency.USD

    @Test
    fun `should carry accountId direction and money when constructed with positive amount`() {
        val money = Money.of(BigDecimal("100.00"), usd)
        val entry =
            Entry(
                id = EntryId.generate(),
                accountId = accountId,
                direction = EntryDirection.DEBIT,
                amount = money,
            )

        entry.accountId shouldBe accountId
        entry.direction shouldBe EntryDirection.DEBIT
        entry.amount shouldBe money
    }

    @Test
    fun `should carry negative amount entry for CREDIT direction`() {
        val money = Money.of(BigDecimal("-100.00"), usd)
        val entry =
            Entry(
                id = EntryId.generate(),
                accountId = accountId,
                direction = EntryDirection.CREDIT,
                amount = money,
            )

        entry.amount.amount.compareTo(BigDecimal("-100.00")) shouldBe 0
        entry.direction shouldBe EntryDirection.CREDIT
    }

    @Test
    fun `should reject zero amount when constructing Entry`() {
        val zeroMoney = Money.zero(usd)

        val exception =
            shouldThrow<DomainException> {
                Entry(
                    id = EntryId.generate(),
                    accountId = accountId,
                    direction = EntryDirection.DEBIT,
                    amount = zeroMoney,
                )
            }
        exception.message.shouldNotBeBlank()
    }

    @Test
    fun `should reject zero amount expressed as BigDecimal with explicit scale`() {
        val zeroWithScale = Money.of(BigDecimal("0.0000000000000000"), usd)

        shouldThrow<DomainException> {
            Entry(
                id = EntryId.generate(),
                accountId = accountId,
                direction = EntryDirection.CREDIT,
                amount = zeroWithScale,
            )
        }
    }

    @Test
    fun `should expose EntryId from id field`() {
        val id = EntryId.generate()
        val entry =
            Entry(
                id = id,
                accountId = accountId,
                direction = EntryDirection.DEBIT,
                amount = Money.of(BigDecimal("1.00"), usd),
            )
        entry.id shouldBe id
    }
}
