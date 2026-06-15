// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.outbox

import com.fincore.events.EventEnvelope
import java.time.Instant

interface OutboxEventPublisher {
    fun publish(
        envelope: EventEnvelope<*>,
        aggregateType: String,
        aggregateId: String,
        eventType: String,
        createdAt: Instant,
    )
}
