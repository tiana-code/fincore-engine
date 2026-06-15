// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.IdempotencyKey
import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
class IdempotencyServiceImpl(
    private val store: IdempotencyStore,
) : IdempotencyService {
    @Suppress("SwallowedException") // race is recovered by retry in a fresh transaction, not an error
    override fun execute(
        key: IdempotencyKey,
        requestBody: String,
        action: (String) -> StoredResponse,
    ): IdempotentResult {
        val keyHash = sha256Hex(key.value)
        val requestHash = sha256Hex(requestBody)
        return try {
            store.runOrReplay(keyHash, requestHash, action)
        } catch (race: IdempotencyRaceException) {
            // The winner committed its response; a fresh transaction now replays it.
            store.runOrReplay(keyHash, requestHash, action)
        }
    }

    private fun sha256Hex(value: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
