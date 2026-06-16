// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.observability

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.logging.logback.StructuredLogEncoder
import org.springframework.core.env.Environment
import org.springframework.core.env.StandardEnvironment

class StructuredLogFormatTest {
    private val encoder =
        StructuredLogEncoder().apply {
            setFormat("logstash")
            context = LoggerContext().apply { putObject(Environment::class.java.name, StandardEnvironment()) }
            start()
        }

    @Test
    fun `logstash encoder renders one json object with the standard fields and mdc`() {
        val event =
            LoggingEvent().apply {
                loggerName = "com.fincore.ledger.test"
                level = Level.INFO
                setMessage("structured probe")
                timeStamp = System.currentTimeMillis()
                mdcPropertyMap = mapOf(CorrelationIdAttributes.MDC_KEY to "corr-123")
            }

        val json = jacksonObjectMapper().readTree(encoder.encode(event))

        json.hasNonNull("@timestamp") shouldBe true
        json.get("level").asText() shouldBe "INFO"
        json.get("logger_name").asText() shouldBe "com.fincore.ledger.test"
        json.get("message").asText() shouldBe "structured probe"
        json.get(CorrelationIdAttributes.MDC_KEY).asText() shouldBe "corr-123"
    }
}
