// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.domain

import com.fincore.core.AccountId
import com.fincore.core.TransactionId
import com.fincore.ledger.domain.exception.AccountNotFoundException
import com.fincore.ledger.domain.exception.ConcurrencyConflictException
import com.fincore.ledger.domain.exception.CurrencyConsistencyViolationException
import com.fincore.ledger.domain.exception.DomainException
import com.fincore.ledger.domain.exception.DoubleEntryViolationException
import com.fincore.ledger.domain.exception.DuplicateTransactionException
import com.fincore.ledger.domain.exception.IdempotencyConflictException
import com.fincore.ledger.domain.exception.TransactionAlreadyReversedException
import com.fincore.ledger.domain.exception.TransactionNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class DomainExceptionsTest {
    @Test
    fun `AccountNotFoundException should carry the id and be a DomainException`() {
        val id = AccountId.generate()
        val ex = AccountNotFoundException(id)

        ex.shouldBeInstanceOf<DomainException>()
        ex.message.shouldContain(id.toString())
    }

    @Test
    fun `TransactionNotFoundException should carry the id and be a DomainException`() {
        val id = TransactionId.generate()
        val ex = TransactionNotFoundException(id)

        ex.shouldBeInstanceOf<DomainException>()
        ex.message.shouldContain(id.toString())
    }

    @Test
    fun `IdempotencyConflictException should be a DomainException with a fixed message`() {
        val ex = IdempotencyConflictException()

        ex.shouldBeInstanceOf<DomainException>()
        ex.message shouldBe "Idempotency key reused with a different request payload"
    }

    @Test
    fun `ConcurrencyConflictException should wrap the cause and be a RuntimeException not a DomainException`() {
        val cause = IllegalStateException("optimistic lock")
        val ex: Throwable = ConcurrencyConflictException(cause)

        ex.shouldBeInstanceOf<RuntimeException>()
        (ex is DomainException) shouldBe false
        ex.cause shouldBe cause
    }

    @Test
    fun `DuplicateTransactionException should default its cause to null when none is supplied`() {
        val ex = DuplicateTransactionException("ref-1")

        ex.shouldBeInstanceOf<DomainException>()
        ex.message.shouldContain("ref-1")
        ex.cause shouldBe null
    }

    @Test
    fun `DuplicateTransactionException should keep the supplied cause`() {
        val cause = IllegalStateException("unique violation")
        val ex = DuplicateTransactionException("ref-2", cause)

        ex.cause shouldBe cause
    }

    @Test
    fun `TransactionAlreadyReversedException should default its cause to null when none is supplied`() {
        val id = TransactionId.generate()
        val ex = TransactionAlreadyReversedException(id)

        ex.shouldBeInstanceOf<DomainException>()
        ex.message.shouldContain(id.toString())
        ex.cause shouldBe null
    }

    @Test
    fun `TransactionAlreadyReversedException should keep the supplied cause`() {
        val cause = IllegalStateException("already reversed")
        val ex = TransactionAlreadyReversedException(TransactionId.generate(), cause)

        ex.cause shouldBe cause
    }

    @Test
    fun `CurrencyConsistencyViolationException should carry its message and be a DomainException`() {
        val ex = CurrencyConsistencyViolationException("currency mismatch")

        ex.shouldBeInstanceOf<DomainException>()
        ex.message shouldBe "currency mismatch"
    }

    @Test
    fun `DoubleEntryViolationException should carry its message and be a DomainException`() {
        val ex = DoubleEntryViolationException("net is not zero")

        ex.shouldBeInstanceOf<DomainException>()
        ex.message shouldBe "net is not zero"
    }

    @Test
    fun `DomainException should construct when the message is non-blank`() {
        val ex = DomainException("something went wrong")

        ex.message shouldBe "something went wrong"
        ex.cause shouldBe null
    }

    @Test
    fun `DomainException should reject a blank message`() {
        shouldThrow<IllegalArgumentException> {
            DomainException("   ")
        }
    }
}
