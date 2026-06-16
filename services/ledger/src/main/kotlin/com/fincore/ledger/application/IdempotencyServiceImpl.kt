// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.IdempotencyKey
import com.fincore.ledger.config.IdempotencyProperties
import com.fincore.ledger.domain.exception.ConcurrencyConflictException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service

@Service
class IdempotencyServiceImpl(
    private val store: IdempotencyStore,
    private val properties: IdempotencyProperties,
) : IdempotencyService {
    override fun execute(
        key: IdempotencyKey,
        requestBody: String,
        action: (String) -> StoredResponse,
    ): IdempotentResult {
        val keyHash = RequestHashing.sha256Hex(key.value)
        val requestHash = RequestHashing.sha256Hex(requestBody)
        return runWithRetry(keyHash, requestHash, action)
    }

    @Suppress("SwallowedException") // race is recovered by single-shot replay in a fresh transaction, not an error
    private fun runWithRetry(
        keyHash: String,
        requestHash: String,
        action: (String) -> StoredResponse,
    ): IdempotentResult {
        var attempt = 0
        while (true) {
            try {
                return store.runOrReplay(keyHash, requestHash, action)
            } catch (lock: OptimisticLockingFailureException) {
                attempt++
                if (attempt >= properties.maxAttempts) throw ConcurrencyConflictException(lock)
            } catch (race: IdempotencyRaceException) {
                return store.runOrReplay(keyHash, requestHash, action)
            }
        }
    }
}
