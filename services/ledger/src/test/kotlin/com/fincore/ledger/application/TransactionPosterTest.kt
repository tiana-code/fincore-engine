// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.exception.AccountNotFoundException
import com.fincore.ledger.domain.exception.DomainException
import com.fincore.ledger.domain.exception.DoubleEntryViolationException
import com.fincore.ledger.domain.exception.DuplicateTransactionException
import com.fincore.ledger.infrastructure.persistence.AccountBalanceRepository
import com.fincore.ledger.infrastructure.persistence.AccountEntity
import com.fincore.ledger.infrastructure.persistence.AccountRepository
import com.fincore.ledger.infrastructure.persistence.EntryRepository
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import com.fincore.ledger.infrastructure.persistence.TransactionPersistenceAdapter
import com.fincore.ledger.infrastructure.persistence.TransactionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import java.util.UUID

class TransactionPosterTest {
    private val accountRepository = mockk<AccountRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val entryRepository = mockk<EntryRepository>()
    private val balanceRepository = mockk<AccountBalanceRepository>()
    private val outboxRepository = mockk<OutboxEventRepository>()
    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val poster =
        TransactionPoster(
            accountRepository,
            transactionRepository,
            entryRepository,
            balanceRepository,
            outboxRepository,
            TransactionPersistenceAdapter(),
            objectMapper,
        )

    private val now = Instant.parse("2026-06-05T12:00:00Z")
    private val accountA = UUID.randomUUID()
    private val accountB = UUID.randomUUID()

    private fun account(
        id: UUID,
        status: AccountStatus = AccountStatus.ACTIVE,
        currency: String = "USD",
    ) = AccountEntity(id, "Account", AccountType.USER_WALLET, currency, status, "{}", 0, now, "a", now, "a")

    private fun command(
        reference: String = "ref-1",
        currency: Currency = Currency.USD,
        debit: BigDecimal = BigDecimal("100.00"),
        credit: BigDecimal = BigDecimal("-100.00"),
    ) = PostTransactionCommand(
        reference = reference,
        description = null,
        currency = currency,
        entries =
            listOf(
                EntryLine(AccountId(accountA), EntryDirection.DEBIT, debit),
                EntryLine(AccountId(accountB), EntryDirection.CREDIT, credit),
            ),
        actor = "op",
        correlationId = "corr-1",
    )

    private fun stubActiveAccounts() {
        every { accountRepository.findById(accountA) } returns Optional.of(account(accountA))
        every { accountRepository.findById(accountB) } returns Optional.of(account(accountB))
    }

    @Test
    fun `should persist transaction entries balances and a single outbox event`() {
        stubActiveAccounts()
        every { transactionRepository.existsByReference("ref-1") } returns false
        every { transactionRepository.saveAndFlush(any()) } answers { firstArg() }
        every { entryRepository.saveAndFlush(any()) } answers { firstArg() }
        every { balanceRepository.findById(any()) } returns Optional.empty()
        every { balanceRepository.saveAndFlush(any()) } answers { firstArg() }
        every { outboxRepository.saveAndFlush(any()) } answers { firstArg() }

        val result = poster.post(command())

        result.reference shouldBe "ref-1"
        verify(exactly = 2) { entryRepository.saveAndFlush(any()) }
        verify(exactly = 2) { balanceRepository.saveAndFlush(any()) }
        verify(exactly = 1) { outboxRepository.saveAndFlush(any()) }
    }

    @Test
    fun `should reject an unbalanced transaction before touching the database`() {
        shouldThrow<DoubleEntryViolationException> {
            poster.post(command(debit = BigDecimal("100.00"), credit = BigDecimal("-90.00")))
        }

        verify(exactly = 0) { transactionRepository.saveAndFlush(any()) }
    }

    @Test
    fun `should reject posting to a frozen account`() {
        every { accountRepository.findById(accountA) } returns Optional.of(account(accountA, AccountStatus.FROZEN))
        every { accountRepository.findById(accountB) } returns Optional.of(account(accountB))

        shouldThrow<DomainException> { poster.post(command()) }

        verify(exactly = 0) { transactionRepository.saveAndFlush(any()) }
    }

    @Test
    fun `should reject posting to a missing account`() {
        every { accountRepository.findById(accountA) } returns Optional.empty()
        every { accountRepository.findById(accountB) } returns Optional.of(account(accountB))

        shouldThrow<AccountNotFoundException> { poster.post(command()) }
    }

    @Test
    fun `should reject a currency mismatch between account and transaction`() {
        every { accountRepository.findById(accountA) } returns Optional.of(account(accountA, currency = "EUR"))
        every { accountRepository.findById(accountB) } returns Optional.of(account(accountB))

        shouldThrow<DomainException> { poster.post(command()) }
    }

    @Test
    fun `should reject a duplicate reference`() {
        stubActiveAccounts()
        every { transactionRepository.existsByReference("ref-1") } returns true

        shouldThrow<DuplicateTransactionException> { poster.post(command()) }

        verify(exactly = 0) { transactionRepository.saveAndFlush(any()) }
    }
}
