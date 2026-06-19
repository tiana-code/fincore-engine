// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.aml

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

/** Generic count and sum of observations within an [AmlWindow]. No thresholds; the caller interprets the result. */
@Component
class RollingWindowAggregator {
    fun aggregate(
        observations: List<AmlObservation>,
        window: AmlWindow,
        now: Instant,
    ): WindowAggregate {
        val relevant =
            when (window) {
                AmlWindow.PER_TRANSACTION -> listOfNotNull(observations.maxByOrNull { it.occurredAt })
                AmlWindow.MONTH -> {
                    val cutoff = now.minus(MONTH_LOOKBACK)
                    observations.filter { !it.occurredAt.isBefore(cutoff) }
                }
                AmlWindow.LIFETIME -> observations
            }
        return WindowAggregate(
            count = relevant.size.toLong(),
            total = relevant.fold(BigDecimal.ZERO) { acc, observation -> acc + observation.amount },
        )
    }

    private companion object {
        const val MONTH_LOOKBACK_DAYS = 30L
        val MONTH_LOOKBACK: Duration = Duration.ofDays(MONTH_LOOKBACK_DAYS)
    }
}
