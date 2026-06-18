// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.outbox

import java.time.Duration
import java.util.UUID

/**
 * Transactional boundary for the dispatcher. Each method is a separate short transaction so the broker
 * publish in [OutboxDispatcher] never runs inside a database transaction. Each service provides its own
 * implementation over its own outbox table.
 */
interface OutboxStore {
    fun claim(
        maxAttempts: Int,
        leaseTimeout: Duration,
        batchSize: Int,
    ): List<ClaimedEvent>

    fun markPublished(id: UUID)

    fun markFailed(
        id: UUID,
        attempts: Int,
        maxAttempts: Int,
        error: String?,
    )
}
