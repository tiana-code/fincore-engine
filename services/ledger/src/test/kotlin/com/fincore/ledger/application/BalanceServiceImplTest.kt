// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.ledger.infrastructure.persistence.AccountBalanceEntity
import com.fincore.ledger.infrastructure.persistence.AccountBalanceKey
import com.fincore.ledger.infrastructure.persistence.AccountBalanceRepository
import com.fincore.ledger.infrastructure.persistence.EntryRepository
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional

class BalanceServiceImplTest {
    private val balanceRepository = mockk<AccountBalanceRepository>()
    private val entryRepository = mockk<EntryRepository>()
    private val registry = SimpleMeterRegistry()
    private val service = BalanceServiceImpl(balanceRepository, entryRepository, LedgerMetrics(registry))
    private val accountId = AccountId.generate()

    private fun balanceReads() = registry.counter("ledger.balance.reads").count()

    @Test
    fun `should return zero when no balance row exists`() {
        every { balanceRepository.findById(any()) } returns Optional.empty()

        val balance = service.current(accountId, Currency.USD)

        balance.amount.isZero() shouldBe true
        balance.lastPostedAt shouldBe null
    }

    @Test
    fun `should return the stored balance`() {
        val key = AccountBalanceKey(accountId.value, "USD")
        val postedAt = Instant.parse("2026-06-05T12:00:00Z")
        every { balanceRepository.findById(key) } returns
            Optional.of(AccountBalanceEntity(key, BigDecimal("250.50"), postedAt, 2))

        val balance = service.current(accountId, Currency.USD)

        balance.amount.amount.compareTo(BigDecimal("250.50")) shouldBe 0
        balance.lastPostedAt shouldBe postedAt
    }

    @Test
    fun `should sum entries up to the asOf instant`() {
        val asOf = Instant.parse("2026-06-05T12:00:00Z")
        every { entryRepository.sumAmount(accountId.value, "USD", asOf) } returns BigDecimal("75.00")

        val balance = service.asOf(accountId, Currency.USD, asOf)

        balance.amount.amount.compareTo(BigDecimal("75.00")) shouldBe 0
    }

    @Test
    fun `should count one balance read per current and asOf call`() {
        val key = AccountBalanceKey(accountId.value, "USD")
        every { balanceRepository.findById(key) } returns Optional.empty()
        every { entryRepository.sumAmount(accountId.value, "USD", any()) } returns BigDecimal.ZERO

        service.current(accountId, Currency.USD)
        service.asOf(accountId, Currency.USD, Instant.parse("2026-06-05T12:00:00Z"))

        balanceReads() shouldBe 2.0
    }
}
