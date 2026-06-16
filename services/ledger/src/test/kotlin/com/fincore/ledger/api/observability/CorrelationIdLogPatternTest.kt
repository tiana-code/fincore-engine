// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.observability

import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class CorrelationIdLogPatternTest {
    private val logback: String =
        requireNotNull(this::class.java.getResource("/logback-spring.xml")?.readText()) {
            "logback-spring.xml not found on the classpath"
        }

    @Test
    fun `dev profile keeps the human-readable correlation id pattern`() {
        logback shouldContain "<springProfile name=\"dev\">"
        logback shouldContain "%X{${CorrelationIdAttributes.MDC_KEY}:-}"
    }

    @Test
    fun `non-dev profiles delegate to the boot console appender for structured output`() {
        logback shouldContain "<springProfile name=\"!dev\">"
        logback shouldContain "org/springframework/boot/logging/logback/console-appender.xml"
    }
}
