// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.consumer

import java.util.UUID

/**
 * Dedup store for at-least-once consumers. Keyed by (envelope id, consumer group) so independent
 * consumers each process an event exactly once.
 */
interface ProcessedEventStore {
    /**
     * Atomically claims an envelope for a consumer group. Returns true the first time the pair is
     * seen (the caller should process it) and false on every subsequent call (a duplicate to skip).
     */
    fun markIfFirstSeen(
        envelopeId: UUID,
        consumerGroup: String,
    ): Boolean
}

enum class EventProcessingOutcome {
    PROCESSED,
    DUPLICATE_SKIPPED,
}
