// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.observability

import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class CorrelationIdLogPatternTest {
    @Test
    fun `console log pattern renders the correlation id with an empty default`() {
        val logback = this::class.java.getResource("/logback-spring.xml")?.readText()
        requireNotNull(logback) { "logback-spring.xml not found on the classpath" }
        logback shouldContain "%X{${CorrelationIdAttributes.MDC_KEY}:-}"
    }
}
