// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.ledger.domain.Account
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.exception.AccountNotFoundException
import com.fincore.ledger.infrastructure.persistence.EntryEntity
import com.fincore.ledger.infrastructure.persistence.EntryKey
import com.fincore.ledger.infrastructure.persistence.EntryRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID

class EntryQueryServiceImplTest {
    private val accountService = mockk<AccountService>()
    private val entryRepository = mockk<EntryRepository>()
    private val service = EntryQueryServiceImpl(accountService, entryRepository)

    private val accountId = AccountId.generate()

    private fun stubAccount() {
        every { accountService.get(accountId) } returns Account(accountId, "Wallet", AccountType.USER_WALLET, Currency.EUR)
    }

    private fun row(postedAt: Instant) =
        EntryEntity(
            EntryKey(UUID.randomUUID(), postedAt),
            UUID.randomUUID(),
            accountId.value,
            BigDecimal("100.00"),
            "EUR",
            EntryDirection.DEBIT,
            postedAt,
        )

    @Test
    fun `should default to a 30 day window and request one more than the limit`() {
        stubAccount()
        val from = slot<Instant>()
        val to = slot<Instant>()
        val pageable = slot<Pageable>()
        every {
            entryRepository.findAccountEntries(accountId.value, capture(from), capture(to), capture(pageable))
        } returns emptyList()

        service.listAccountEntries(accountId, null, null, null, 50)

        Duration.between(from.captured, to.captured) shouldBe Duration.ofDays(30)
        pageable.captured.pageSize shouldBe 51
    }

    @Test
    fun `should emit a next cursor only when the page overflows`() {
        stubAccount()
        val newest = Instant.parse("2026-06-13T10:00:00Z")
        val rows = listOf(row(newest), row(newest.minusSeconds(1)), row(newest.minusSeconds(2)))
        every { entryRepository.findAccountEntries(any(), any(), any(), any()) } returns rows

        val page = service.listAccountEntries(accountId, null, null, null, 2)

        page.items.size shouldBe 2
        val decoded = EntryCursor.decode(page.nextCursor!!)
        decoded.id shouldBe rows[1].key.id
    }

    @Test
    fun `should return a null next cursor when the page does not overflow`() {
        stubAccount()
        every { entryRepository.findAccountEntries(any(), any(), any(), any()) } returns
            listOf(row(Instant.parse("2026-06-13T10:00:00Z")))

        service.listAccountEntries(accountId, null, null, null, 2).nextCursor shouldBe null
    }

    @Test
    fun `should pass a decoded cursor to the repository`() {
        stubAccount()
        val cursorPostedAt = Instant.parse("2026-06-10T08:00:00Z")
        val cursorId = UUID.randomUUID()
        every {
            entryRepository.findAccountEntriesAfter(accountId.value, any(), any(), cursorPostedAt, cursorId, any())
        } returns emptyList()

        service.listAccountEntries(accountId, null, null, EntryCursor(cursorPostedAt, cursorId).encode(), 50)
    }

    @Test
    fun `should reject a window wider than 90 days`() {
        stubAccount()
        val from = Instant.parse("2026-01-01T00:00:00Z")
        val to = Instant.parse("2026-06-01T00:00:00Z")

        shouldThrow<IllegalArgumentException> { service.listAccountEntries(accountId, from, to, null, 50) }
    }

    @Test
    fun `should reject a limit outside one to two hundred`() {
        shouldThrow<IllegalArgumentException> { service.listAccountEntries(accountId, null, null, null, 0) }
        shouldThrow<IllegalArgumentException> { service.listAccountEntries(accountId, null, null, null, 201) }
    }

    @Test
    fun `should propagate a missing account`() {
        every { accountService.get(accountId) } throws AccountNotFoundException(accountId)

        shouldThrow<AccountNotFoundException> { service.listAccountEntries(accountId, null, null, null, 50) }
    }
}
