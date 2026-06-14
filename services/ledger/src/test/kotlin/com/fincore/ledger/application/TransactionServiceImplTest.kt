// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.core.TransactionId
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.enum.TransactionStatus
import com.fincore.ledger.domain.exception.ConcurrencyConflictException
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
import java.util.UUID

class TransactionServiceImplTest {
    private val poster = mockk<TransactionPoster>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val service = TransactionServiceImpl(poster, transactionRepository)

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
