// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.events.OutboxStatus
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.Instant

class OutboxMetricsTest {
    private val registry = SimpleMeterRegistry()
    private val repository = mockk<OutboxEventRepository>()

    @Test
    fun `should expose one bounded gauge per status reflecting its count`() {
        OutboxStatus.entries.forEachIndexed { index, status ->
            every { repository.countByStatus(status) } returns (index + 1).toLong()
        }
        every { repository.oldestCreatedAt(OutboxStatus.PENDING) } returns null

        OutboxMetrics(registry, repository)

        OutboxStatus.entries.forEachIndexed { index, status ->
            registry
                .get(OutboxMetrics.METRIC_EVENTS)
                .tag(OutboxMetrics.TAG_STATUS, status.name)
                .gauge()
                .value() shouldBe
                (index + 1).toDouble()
        }
        registry.find(OutboxMetrics.METRIC_EVENTS).gauges().size shouldBe OutboxStatus.entries.size
    }

    @Test
    fun `should report zero lag when there is no pending row`() {
        every { repository.countByStatus(any()) } returns NONE
        every { repository.oldestCreatedAt(OutboxStatus.PENDING) } returns null

        OutboxMetrics(registry, repository)

        registry.get(OutboxMetrics.METRIC_LAG_SECONDS).gauge().value() shouldBe NO_LAG
    }

    @Test
    fun `should compute lag as the age of the oldest pending row`() {
        val now = Instant.parse("2026-06-18T00:01:00Z")
        val oldest = now.minusSeconds(AGE_SECONDS)

        OutboxMetrics.lagSeconds(oldest, now) shouldBe AGE_SECONDS.toDouble()
        OutboxMetrics.lagSeconds(null, now) shouldBe NO_LAG
    }

    private companion object {
        const val NONE = 0L
        const val NO_LAG = 0.0
        const val AGE_SECONDS = 60L
    }
}
