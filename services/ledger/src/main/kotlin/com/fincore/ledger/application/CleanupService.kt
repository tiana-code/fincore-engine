// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

interface CleanupService {
    fun purge(): CleanupResult
}

data class CleanupResult(
    val idempotencyKeysDeleted: Int,
    val outboxEventsDeleted: Int,
)
