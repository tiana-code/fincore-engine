// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.core.TransactionId
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.enum.TransactionStatus
import com.fincore.ledger.domain.exception.ConcurrencyConflictException
import com.fincore.ledger.domain.exception.TransactionNotFoundException
import com.fincore.ledger.infrastructure.persistence.EntryEntity
import com.fincore.ledger.infrastructure.persistence.EntryKey
import com.fincore.ledger.infrastructure.persistence.EntryRepository
import com.fincore.ledger.infrastructure.persistence.TransactionEntity
import com.fincore.ledger.infrastructure.persistence.TransactionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import java.util.UUID

class TransactionServiceImplTest {
    private val poster = mockk<TransactionPoster>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val entryRepository = mockk<EntryRepository>()
    private val service = TransactionServiceImpl(poster, transactionRepository, entryRepository)

    private val command =
        PostTransactionCommand(
            reference = "ref-1",
            description = null,
            currency = Currency.USD,
            entries =
                listOf(
                    EntryLine(AccountId.generate(), EntryDirection.DEBIT, BigDecimal("100.00")),
                    EntryLine(AccountId.generate(), EntryDirection.CREDIT, BigDecimal("-100.00")),
                ),
            actor = "op",
            correlationId = null,
        )

    @Test
    fun `should post through the poster`() {
        every { poster.post(command) } returns PostedTransaction(TransactionId.generate(), "ref-1", TransactionStatus.POSTED, Instant.now())

        service.post(command).reference shouldBe "ref-1"
    }

    @Test
    fun `should retry on an optimistic lock failure then succeed`() {
        var calls = 0
        every { poster.post(command) } answers {
            calls++
            if (calls < 2) throw OptimisticLockingFailureException("version conflict")
            PostedTransaction(TransactionId.generate(), "ref-1", TransactionStatus.POSTED, Instant.now())
        }

        service.post(command)

        verify(exactly = 2) { poster.post(command) }
    }

    @Test
    fun `should fail with a concurrency conflict after three attempts`() {
        every { poster.post(command) } throws OptimisticLockingFailureException("version conflict")

        shouldThrow<ConcurrencyConflictException> { service.post(command) }

        verify(exactly = 3) { poster.post(command) }
    }

    @Test
    fun `should reverse through the poster`() {
        val id = TransactionId.generate()
        val compensating = PostedTransaction(TransactionId.generate(), "reversal-of-$id", TransactionStatus.POSTED, Instant.now())
        every { poster.postReversal(id, "op", "corr-1") } returns compensating

        service.reverse(id, "op", "corr-1") shouldBe compensating

        verify(exactly = 1) { poster.postReversal(id, "op", "corr-1") }
    }

    @Test
    fun `should retry reverse on an optimistic lock failure then succeed`() {
        val id = TransactionId.generate()
        var calls = 0
        every { poster.postReversal(id, "op", null) } answers {
            calls++
            if (calls < 2) throw OptimisticLockingFailureException("version conflict")
            PostedTransaction(TransactionId.generate(), "reversal-of-$id", TransactionStatus.POSTED, Instant.now())
        }

        service.reverse(id, "op", null)

        verify(exactly = 2) { poster.postReversal(id, "op", null) }
    }

    private val postedAt = Instant.parse("2026-06-12T08:00:00Z")

    private fun transactionEntity(
        id: UUID,
        reversesId: UUID?,
    ) = TransactionEntity(id, "ref-7", "a tx", TransactionStatus.REVERSED, reversesId, "{}", postedAt, postedAt, "op")

    private fun entryEntity(
        transactionId: UUID,
        account: UUID,
    ) = EntryEntity(
        EntryKey(UUID.randomUUID(), postedAt),
        transactionId,
        account,
        BigDecimal("100.00"),
        "USD",
        EntryDirection.DEBIT,
        postedAt,
    )

    @Test
    fun `should assemble a transaction detail with its entries`() {
        val id = UUID.randomUUID()
        val account = UUID.randomUUID()
        val reverses = UUID.randomUUID()
        every { transactionRepository.findById(id) } returns Optional.of(transactionEntity(id, reverses))
        every { entryRepository.findByTransactionId(id) } returns listOf(entryEntity(id, account))

        val detail = service.get(TransactionId(id))

        detail.id shouldBe TransactionId(id)
        detail.status shouldBe TransactionStatus.REVERSED
        detail.reversesId shouldBe TransactionId(reverses)
        detail.entries.single().let {
            it.accountId shouldBe AccountId(account)
            it.direction shouldBe EntryDirection.DEBIT
            it.amount.compareTo(BigDecimal("100.00")) shouldBe 0
            it.currency shouldBe "USD"
        }
    }

    @Test
    fun `should fail to get an unknown transaction`() {
        val id = UUID.randomUUID()
        every { transactionRepository.findById(id) } returns Optional.empty()

        shouldThrow<TransactionNotFoundException> { service.get(TransactionId(id)) }
    }

    @Test
    fun `should list transactions mapped to summaries ordered by posted_at desc then id desc`() {
        val id = UUID.randomUUID()
        val postedAt = Instant.parse("2026-06-10T09:00:00Z")
        val entity =
            TransactionEntity(
                id = id,
                reference = "ref-9",
                description = null,
                status = TransactionStatus.POSTED,
                reversesId = null,
                metadata = "{}",
                postedAt = postedAt,
                createdAt = postedAt,
                createdBy = "op",
            )
        val pageableSlot = slot<Pageable>()
        every { transactionRepository.findAll(capture(pageableSlot)) } returns PageImpl(listOf(entity), PageRequest.of(0, 20), 1)

        val page = service.list(0, 20)

        page.totalElements shouldBe 1
        page.items.single().let {
            it.id shouldBe TransactionId(id)
            it.reference shouldBe "ref-9"
            it.status shouldBe TransactionStatus.POSTED
            it.postedAt shouldBe postedAt
        }
        pageableSlot.captured.sort shouldBe Sort.by(Sort.Order.desc("postedAt"), Sort.Order.desc("id"))
    }
}
