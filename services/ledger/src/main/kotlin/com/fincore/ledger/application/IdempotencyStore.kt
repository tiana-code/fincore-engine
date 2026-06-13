// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.ledger.domain.exception.IdempotencyConflictException
import com.fincore.ledger.infrastructure.persistence.IdempotencyKeyEntity
import com.fincore.ledger.infrastructure.persistence.IdempotencyKeyRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

// Raised when a concurrent caller has already claimed the key in this transaction window. The
// orchestrator retries in a fresh transaction, where the winner's committed response is replayed.
internal class IdempotencyRaceException(
    cause: Throwable,
) : RuntimeException(cause)

@Component
class IdempotencyStore(
    private val repository: IdempotencyKeyRepository,
) {
    @Transactional
    fun runOrReplay(
        keyHash: String,
        requestHash: String,
        action: () -> StoredResponse,
    ): IdempotentResult {
        val now = Instant.now()
        val existing = repository.findById(keyHash).orElse(null)
        if (existing != null && existing.expiresAt.isAfter(now)) {
            if (existing.requestHash != requestHash) {
                throw IdempotencyConflictException()
            }
            return IdempotentResult(existing.statusCode, existing.responseBody, replayed = true)
        }
        if (existing != null) {
            repository.delete(existing)
            repository.flush()
        }
        val reservation = IdempotencyKeyEntity(keyHash, requestHash, null, null, now, now.plus(TTL))
        try {
            repository.saveAndFlush(reservation)
        } catch (duplicate: DataIntegrityViolationException) {
            throw IdempotencyRaceException(duplicate)
        }
        val response = action()
        reservation.statusCode = response.statusCode
        reservation.responseBody = response.responseBody
        repository.saveAndFlush(reservation)
        return IdempotentResult(response.statusCode, response.responseBody, replayed = false)
    }

    companion object {
        private val TTL = Duration.ofHours(24)
    }
}
