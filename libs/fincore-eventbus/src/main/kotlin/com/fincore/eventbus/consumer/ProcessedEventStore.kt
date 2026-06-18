// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.consumer

import java.util.UUID

interface ProcessedEventStore {
    /** Returns true the first time (envelopeId, consumerGroup) is claimed, false for a duplicate. */
    fun markIfFirstSeen(
        envelopeId: UUID,
        consumerGroup: String,
    ): Boolean
}

enum class EventProcessingOutcome {
    PROCESSED,
    DUPLICATE_SKIPPED,
}
