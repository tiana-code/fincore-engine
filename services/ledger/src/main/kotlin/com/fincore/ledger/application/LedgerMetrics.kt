// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class LedgerMetrics(
    registry: MeterRegistry,
) {
    private val postedCounter: Counter =
        Counter.builder(METRIC_POSTED).tag(TAG_TYPE, TYPE_POST).register(registry)
    private val reversalCounter: Counter =
        Counter.builder(METRIC_POSTED).tag(TAG_TYPE, TYPE_REVERSAL).register(registry)
    private val balanceReadsCounter: Counter =
        Counter.builder(METRIC_BALANCE_READS).register(registry)

    fun recordPost() = postedCounter.increment()

    fun recordReversal() = reversalCounter.increment()

    fun recordBalanceRead() = balanceReadsCounter.increment()

    private companion object {
        const val METRIC_POSTED = "ledger.transactions.posted"
        const val METRIC_BALANCE_READS = "ledger.balance.reads"
        const val TAG_TYPE = "type"
        const val TYPE_POST = "post"
        const val TYPE_REVERSAL = "reversal"
    }
}
