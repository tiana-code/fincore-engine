// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.ledger.domain.exception.IdempotencyConflictException
import com.fincore.ledger.infrastructure.persistence.IdempotencyKeyEntity
import com.fincore.ledger.infrastructure.persistence.IdempotencyKeyRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant
import java.util.Optional

class IdempotencyStoreTest {
    private val repository = mockk<IdempotencyKeyRepository>()
    private val store = IdempotencyStore(repository)

    private fun keyEntity(
        requestHash: String,
        valid: Boolean,
        statusCode: Int = 201,
    ): IdempotencyKeyEntity {
        val now = Instant.now()
        val expiresAt = if (valid) now.plusSeconds(3600) else now.minusSeconds(3600)
        return IdempotencyKeyEntity("kh", requestHash, statusCode, "{\"id\":1}", now, expiresAt)
    }

    @Test
    fun `should replay a stored response on a matching request hash`() {
        every { repository.findById("kh") } returns Optional.of(keyEntity("rh", valid = true))

        val result = store.runOrReplay("kh", "rh") { error("action must not run on replay") }

        result.replayed shouldBe true
        result.statusCode shouldBe 201
    }

    @Test
    fun `should throw conflict on a different request hash`() {
        every { repository.findById("kh") } returns Optional.of(keyEntity("rh-a", valid = true))

        shouldThrow<IdempotencyConflictException> {
            store.runOrReplay("kh", "rh-b") { StoredResponse(200, "{}") }
        }
    }

    @Test
    fun `should run the action and store the response when absent`() {
        every { repository.findById("kh") } returns Optional.empty()
        every { repository.saveAndFlush(any()) } answers { firstArg() }

        val result = store.runOrReplay("kh", "rh") { StoredResponse(201, "{\"ok\":true}") }

        result.replayed shouldBe false
        result.statusCode shouldBe 201
        verify(exactly = 2) { repository.saveAndFlush(any()) }
    }

    @Test
    fun `should refresh an expired key`() {
        val expired = keyEntity("rh-old", valid = false)
        every { repository.findById("kh") } returns Optional.of(expired)
        every { repository.delete(expired) } answers { }
        every { repository.flush() } answers { }
        every { repository.saveAndFlush(any()) } answers { firstArg() }

        val result = store.runOrReplay("kh", "rh-new") { StoredResponse(202, "{}") }

        result.replayed shouldBe false
        verify { repository.delete(expired) }
    }

    @Test
    fun `should signal a race when the reservation insert hits a duplicate`() {
        every { repository.findById("kh") } returns Optional.empty()
        every { repository.saveAndFlush(any()) } throws DataIntegrityViolationException("duplicate key")

        shouldThrow<IdempotencyRaceException> {
            store.runOrReplay("kh", "rh") { StoredResponse(200, "{}") }
        }
    }
}
