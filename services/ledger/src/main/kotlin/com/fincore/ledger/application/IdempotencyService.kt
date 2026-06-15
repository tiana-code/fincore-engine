// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.IdempotencyKey

data class StoredResponse(
    val statusCode: Int,
    val responseBody: String,
)

data class IdempotentResult(
    val statusCode: Int?,
    val responseBody: String?,
    val replayed: Boolean,
)

interface IdempotencyService {
    fun execute(
        key: IdempotencyKey,
        requestBody: String,
        action: (String) -> StoredResponse,
    ): IdempotentResult
}
