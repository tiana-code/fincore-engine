// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.consumer

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Non-persistent dedup store for tests and local development. State is lost on restart, so it gives
 * no exactly-once effect across restarts - a persistent store (see [JdbcProcessedEventStore]) is
 * required in production.
 */
class InMemoryProcessedEventStore : ProcessedEventStore {
    private val seen: MutableSet<Pair<UUID, String>> = ConcurrentHashMap.newKeySet()

    override fun markIfFirstSeen(
        envelopeId: UUID,
        consumerGroup: String,
    ): Boolean = seen.add(envelopeId to consumerGroup)
}
