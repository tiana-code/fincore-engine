// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.consumer

import java.util.UUID

/**
 * Runs a handler exactly once per (envelope id, consumer group). Claim-then-handle: the dedup claim
 * is taken first, then the handler runs. Call this inside the consumer's transaction so a thrown
 * handler rolls back the claim and the event is retried (at-least-once, exactly-once effect).
 */
class IdempotentEventProcessor(
    private val store: ProcessedEventStore,
) {
    fun process(
        envelopeId: UUID,
        consumerGroup: String,
        handler: () -> Unit,
    ): EventProcessingOutcome =
        if (store.markIfFirstSeen(envelopeId, consumerGroup)) {
            handler()
            EventProcessingOutcome.PROCESSED
        } else {
            EventProcessingOutcome.DUPLICATE_SKIPPED
        }
}
