// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.core.TransactionId
import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.enum.TransactionStatus
import com.fincore.ledger.domain.exception.AccountNotFoundException
import com.fincore.ledger.domain.exception.DomainException
import com.fincore.ledger.domain.exception.DoubleEntryViolationException
import com.fincore.ledger.domain.exception.DuplicateTransactionException
import com.fincore.ledger.domain.exception.TransactionAlreadyReversedException
import com.fincore.ledger.domain.exception.TransactionNotFoundException
import com.fincore.ledger.infrastructure.outbox.OutboxEventPublisher
import com.fincore.ledger.infrastructure.persistence.AccountBalanceRepository
import com.fincore.ledger.infrastructure.persistence.AccountEntity
import com.fincore.ledger.infrastructure.persistence.AccountRepository
import com.fincore.ledger.infrastructure.persistence.EntryEntity
import com.fincore.ledger.infrastructure.persistence.EntryKey
import com.fincore.ledger.infrastructure.persistence.EntryRepository
import com.fincore.ledger.infrastructure.persistence.TransactionEntity
import com.fincore.ledger.infrastructure.persistence.TransactionPersistenceAdapter
import com.fincore.ledger.infrastructure.persistence.TransactionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.justRun
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
    private val outboxEventPublisher = mockk<OutboxEventPublisher>()
    private val auditWriter = mockk<AuditTrailWriter>(relaxed = true)
    private val registry = SimpleMeterRegistry()
    private val ledgerMetrics = LedgerMetrics(registry)
    private val poster =
        TransactionPoster(
            accountRepository,
            transactionRepository,
            entryRepository,
            balanceRepository,
            outboxEventPublisher,
            auditWriter,
            TransactionPersistenceAdapter(),
            ledgerMetrics,
        )

    private fun postedCount(type: String) = registry.counter("ledger.transactions.posted", "type", type).count()

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
        justRun { balanceRepository.upsertBalance(any(), any(), any(), any()) }
        justRun { outboxEventPublisher.publish(any(), any(), any(), any(), any()) }

        val result = poster.post(command())

        result.reference shouldBe "ref-1"
        verify(exactly = 2) { entryRepository.saveAndFlush(any()) }
        verify(exactly = 2) { balanceRepository.upsertBalance(any(), any(), any(), any()) }
        verify(exactly = 1) { outboxEventPublisher.publish(any(), any(), any(), any(), any()) }
        postedCount("post") shouldBe 1.0
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
        postedCount("post") shouldBe 0.0
    }

    private fun transactionEntity(
        id: UUID,
        status: TransactionStatus,
    ) = TransactionEntity(id, "ref-orig", null, status, null, "{}", now, now, "op")

    private fun entryEntity(
        account: UUID,
        amount: BigDecimal,
        direction: EntryDirection,
        transactionId: UUID,
    ) = EntryEntity(EntryKey(UUID.randomUUID(), now), transactionId, account, amount, "USD", direction, now)

    @Test
    fun `should post a compensating reversal that inverts entries and links and reverses the original`() {
        val originalId = TransactionId.generate()
        val original = transactionEntity(originalId.value, TransactionStatus.POSTED)
        every { transactionRepository.findById(originalId.value) } returns Optional.of(original)
        every { entryRepository.findByTransactionId(originalId.value) } returns
            listOf(
                entryEntity(accountA, BigDecimal("100.00"), EntryDirection.DEBIT, originalId.value),
                entryEntity(accountB, BigDecimal("-100.00"), EntryDirection.CREDIT, originalId.value),
            )
        val txSlots = mutableListOf<TransactionEntity>()
        val entrySlots = mutableListOf<EntryEntity>()
        every { transactionRepository.saveAndFlush(capture(txSlots)) } answers { firstArg() }
        every { entryRepository.saveAndFlush(capture(entrySlots)) } answers { firstArg() }
        justRun { balanceRepository.upsertBalance(any(), any(), any(), any()) }
        justRun { outboxEventPublisher.publish(any(), any(), any(), any(), any()) }

        val result = poster.postReversal(originalId, "op", "corr-1", null, null)

        result.reference shouldBe "reversal-of-$originalId"
        original.status shouldBe TransactionStatus.REVERSED
        txSlots.any { it.reversesId == originalId.value } shouldBe true
        entrySlots[0].direction shouldBe EntryDirection.CREDIT
        entrySlots[0].amount.compareTo(BigDecimal("-100.00")) shouldBe 0
        entrySlots[1].direction shouldBe EntryDirection.DEBIT
        entrySlots[1].amount.compareTo(BigDecimal("100.00")) shouldBe 0
        verify(exactly = 1) { outboxEventPublisher.publish(any(), any(), any(), any(), any()) }
        postedCount("reversal") shouldBe 1.0
    }

    @Test
    fun `should reject reversing a transaction that is not posted`() {
        val originalId = TransactionId.generate()
        every { transactionRepository.findById(originalId.value) } returns
            Optional.of(transactionEntity(originalId.value, TransactionStatus.REVERSED))

        shouldThrow<TransactionAlreadyReversedException> { poster.postReversal(originalId, "op", null, null, null) }

        verify(exactly = 0) { entryRepository.saveAndFlush(any()) }
    }

    @Test
    fun `should reject reversing an unknown transaction`() {
        val originalId = TransactionId.generate()
        every { transactionRepository.findById(originalId.value) } returns Optional.empty()

        shouldThrow<TransactionNotFoundException> { poster.postReversal(originalId, "op", null, null, null) }
    }
}
