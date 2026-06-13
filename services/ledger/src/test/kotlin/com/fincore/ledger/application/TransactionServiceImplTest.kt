// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.core.TransactionId
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.exception.ConcurrencyConflictException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.dao.OptimisticLockingFailureException
import java.math.BigDecimal
import java.time.Instant

class TransactionServiceImplTest {
    private val poster = mockk<TransactionPoster>()
    private val service = TransactionServiceImpl(poster)

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
        every { poster.post(command) } returns PostedTransaction(TransactionId.generate(), "ref-1", Instant.now())

        service.post(command).reference shouldBe "ref-1"
    }

    @Test
    fun `should retry on an optimistic lock failure then succeed`() {
        var calls = 0
        every { poster.post(command) } answers {
            calls++
            if (calls < 2) throw OptimisticLockingFailureException("version conflict")
            PostedTransaction(TransactionId.generate(), "ref-1", Instant.now())
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
}
