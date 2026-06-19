// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.aml

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

class AmlRollingWindowAggregatorTest {
    private val aggregator = RollingWindowAggregator()
    private val now = Instant.parse("2026-06-19T00:00:00Z")

    private fun obs(
        daysAgo: Long,
        amount: String,
    ) = AmlObservation(now.minus(Duration.ofDays(daysAgo)), BigDecimal(amount))

    @Test
    fun `should return zero count and total for empty observations`() {
        val result = aggregator.aggregate(emptyList(), AmlWindow.LIFETIME, now)

        result.count shouldBe 0L
        result.total shouldBe BigDecimal.ZERO
    }

    @Test
    fun `should sum all observations over the lifetime window`() {
        val result = aggregator.aggregate(listOf(obs(1, "10"), obs(400, "5")), AmlWindow.LIFETIME, now)

        result.count shouldBe 2L
        result.total shouldBe BigDecimal("15")
    }

    @Test
    fun `should include the boundary and exclude older over the month window`() {
        val result = aggregator.aggregate(listOf(obs(30, "10"), obs(31, "5")), AmlWindow.MONTH, now)

        result.count shouldBe 1L
        result.total shouldBe BigDecimal("10")
    }

    @Test
    fun `should take the most recent observation by time for per transaction regardless of order`() {
        val result = aggregator.aggregate(listOf(obs(5, "5"), obs(1, "9"), obs(10, "10")), AmlWindow.PER_TRANSACTION, now)

        result.count shouldBe 1L
        result.total shouldBe BigDecimal("9")
    }
}
