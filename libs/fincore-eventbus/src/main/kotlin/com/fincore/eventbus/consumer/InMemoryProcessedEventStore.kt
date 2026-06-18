// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.consumer

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// Non-persistent (state lost on restart): tests/dev only, use JdbcProcessedEventStore in production.
class InMemoryProcessedEventStore : ProcessedEventStore {
    private val seen: MutableSet<Pair<UUID, String>> = ConcurrentHashMap.newKeySet()

    override fun markIfFirstSeen(
        envelopeId: UUID,
        consumerGroup: String,
    ): Boolean = seen.add(envelopeId to consumerGroup)
}
