// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.events.OutboxStatus
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class OutboxMetrics(
    registry: MeterRegistry,
    repository: OutboxEventRepository,
) {
    init {
        OutboxStatus.entries.forEach { status ->
            Gauge
                .builder(METRIC_EVENTS, repository) { it.countByStatus(status).toDouble() }
                .tag(TAG_STATUS, status.name)
                .register(registry)
        }
        Gauge
            .builder(METRIC_LAG_SECONDS, repository) { lagSeconds(it.oldestCreatedAt(OutboxStatus.PENDING), Instant.now()) }
            .register(registry)
    }

    companion object {
        const val METRIC_EVENTS = "fincore.outbox.events"
        const val METRIC_LAG_SECONDS = "fincore.outbox.lag.seconds"
        const val TAG_STATUS = "status"
        private const val NO_LAG = 0.0
        private const val MILLIS_PER_SECOND = 1000.0

        fun lagSeconds(
            oldestPending: Instant?,
            now: Instant,
        ): Double =
            if (oldestPending == null) {
                NO_LAG
            } else {
                Duration.between(oldestPending, now).toMillis().toDouble() / MILLIS_PER_SECOND
            }
    }
}
