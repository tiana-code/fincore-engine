// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.config

import com.fincore.decision.store.api.observability.CorrelationIdFilter
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.core.Ordered

class CorrelationIdFilterConfigTest {
    private val registration = CorrelationIdFilterConfig().correlationIdFilterRegistration()

    @Test
    fun `should register the filter at highest precedence`() {
        registration.order shouldBe Ordered.HIGHEST_PRECEDENCE
    }

    @Test
    fun `should apply the filter to every path`() {
        registration.urlPatterns shouldContain "/*"
    }

    @Test
    fun `should wrap the correlation id filter`() {
        registration.filter.shouldBeInstanceOf<CorrelationIdFilter>()
    }
}
