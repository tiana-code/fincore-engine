// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.IdempotencyKey
import com.fincore.ledger.domain.exception.ConcurrencyConflictException
import com.fincore.ledger.domain.exception.IdempotencyConflictException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.dao.OptimisticLockingFailureException

class IdempotencyServiceImplTest {
    private val store = mockk<IdempotencyStore>()
    private val service = IdempotencyServiceImpl(store)
    private val key = IdempotencyKey.of("a".repeat(40))

    @Test
    fun `should delegate to the store and return its result`() {
        every { store.runOrReplay(any(), any(), any()) } returns IdempotentResult(201, "{}", replayed = false)

        val result = service.execute(key, "{\"x\":1}") { StoredResponse(201, "{}") }

        result.statusCode shouldBe 201
    }

    @Test
    fun `should retry in a fresh attempt after a race`() {
        var calls = 0
        every { store.runOrReplay(any(), any(), any()) } answers {
            calls++
            if (calls == 1) throw IdempotencyRaceException(RuntimeException("dup"))
            IdempotentResult(200, "{}", replayed = true)
        }

        val result = service.execute(key, "{}") { StoredResponse(200, "{}") }

        result.replayed shouldBe true
        verify(exactly = 2) { store.runOrReplay(any(), any(), any()) }
    }

    @Test
    fun `should propagate a conflict`() {
        every { store.runOrReplay(any(), any(), any()) } throws IdempotencyConflictException()

        shouldThrow<IdempotencyConflictException> {
            service.execute(key, "{}") { StoredResponse(200, "{}") }
        }
    }

    // AC-7 / FR-1: retry loop now lives in IdempotencyServiceImpl.execute
    @Test
    fun `should retry in a fresh transaction when the action hits an optimistic lock then succeed`() {
        var calls = 0
        every { store.runOrReplay(any(), any(), any()) } answers {
            calls++
            if (calls == 1) throw OptimisticLockingFailureException("version conflict")
            IdempotentResult(201, "{}", replayed = false)
        }

        val result = service.execute(key, "{}") { StoredResponse(201, "{}") }

        result.statusCode shouldBe 201
        result.replayed shouldBe false
        verify(exactly = 2) { store.runOrReplay(any(), any(), any()) }
    }

    // AC-7 / FR-2 / INV-4: exactly 3 total invocations (not 4) on full failure
    @Test
    fun `should fail with a concurrency conflict after exhausting optimistic retries`() {
        every { store.runOrReplay(any(), any(), any()) } throws OptimisticLockingFailureException("version conflict")

        shouldThrow<ConcurrencyConflictException> {
            service.execute(key, "{}") { StoredResponse(201, "{}") }
        }

        verify(exactly = 3) { store.runOrReplay(any(), any(), any()) }
    }

    // OQ-4: IdempotencyRaceException does not consume an OLF attempt; race is single-shot
    @Test
    fun `should not consume an optimistic attempt when an idempotency race is resolved`() {
        var calls = 0
        every { store.runOrReplay(any(), any(), any()) } answers {
            calls++
            if (calls == 1) throw IdempotencyRaceException(RuntimeException("race"))
            IdempotentResult(200, "{}", replayed = true)
        }

        val result = service.execute(key, "{}") { StoredResponse(200, "{}") }

        result.replayed shouldBe true
        // exactly 2 calls: original + race-replay; the race does not count toward MAX_ATTEMPTS
        verify(exactly = 2) { store.runOrReplay(any(), any(), any()) }
    }
}
