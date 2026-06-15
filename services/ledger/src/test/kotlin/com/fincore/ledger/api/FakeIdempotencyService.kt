// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fincore.core.IdempotencyKey
import com.fincore.ledger.application.IdempotencyService
import com.fincore.ledger.application.IdempotentResult
import com.fincore.ledger.application.StoredResponse

// Hand fake, not MockK: MockK cannot build a call signature for execute() because IdempotencyKey is a
// value class with init validation (its constructor rejects MockK's generated dummy string).
class FakeIdempotencyService : IdempotencyService {
    var handler: (IdempotencyKey, String, (String) -> StoredResponse) -> IdempotentResult = RUN_ACTION

    override fun execute(
        key: IdempotencyKey,
        requestBody: String,
        action: (String) -> StoredResponse,
    ): IdempotentResult = handler(key, requestBody, action)

    fun reset() {
        handler = RUN_ACTION
    }

    companion object {
        val RUN_ACTION: (IdempotencyKey, String, (String) -> StoredResponse) -> IdempotentResult = { _, requestBody, action ->
            val response = action(requestBody)
            IdempotentResult(response.statusCode, response.responseBody, replayed = false)
        }
    }
}
