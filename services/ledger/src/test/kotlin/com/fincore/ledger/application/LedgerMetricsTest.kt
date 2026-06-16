// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test

class LedgerMetricsTest {
    private val registry = SimpleMeterRegistry()
    private val metrics = LedgerMetrics(registry)

    @Test
    fun `should increment the post-tagged counter on a recorded post`() {
        metrics.recordPost()

        registry.counter("ledger.transactions.posted", "type", "post").count() shouldBe 1.0
        registry.counter("ledger.transactions.posted", "type", "reversal").count() shouldBe 0.0
    }

    @Test
    fun `should increment the reversal-tagged counter on a recorded reversal`() {
        metrics.recordReversal()

        registry.counter("ledger.transactions.posted", "type", "reversal").count() shouldBe 1.0
        registry.counter("ledger.transactions.posted", "type", "post").count() shouldBe 0.0
    }

    @Test
    fun `should increment the balance reads counter once per recorded read`() {
        metrics.recordBalanceRead()
        metrics.recordBalanceRead()

        registry.counter("ledger.balance.reads").count() shouldBe 2.0
    }
}
