// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.observability

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.springframework.boot.logging.logback.StructuredLogEncoder
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment

class PiiMaskingMembersCustomizerTest {
    private val uuid = "123e4567-e89b-12d3-a456-426614174000"

    private val encoder: StructuredLogEncoder =
        StructuredLogEncoder().apply {
            setFormat("logstash")
            val environment =
                StandardEnvironment().apply {
                    propertySources.addFirst(
                        MapPropertySource(
                            "test",
                            mapOf("logging.structured.json.customizer" to PiiMaskingMembersCustomizer::class.java.name),
                        ),
                    )
                }
            context = LoggerContext().apply { putObject(Environment::class.java.name, environment) }
            start()
        }

    @Test
    fun `should mask pii in the message while keeping the correlation id and json shape`() {
        val event =
            LoggingEvent().apply {
                loggerName = "com.fincore.observability.test"
                level = Level.INFO
                setMessage("login user@example.test card 4111111111111111")
                timeStamp = System.currentTimeMillis()
                mdcPropertyMap = mapOf("correlation_id" to uuid, "trace_id" to uuid, "span_id" to uuid)
            }

        val json = jacksonObjectMapper().readTree(encoder.encode(event))

        json.get("message").asText() shouldNotContain "user@example.test"
        json.get("message").asText() shouldNotContain "4111111111111111"
        json.get("message").asText() shouldContain PiiMasker.REDACTION
        json.get("correlation_id").asText() shouldBe uuid
        json.get("trace_id").asText() shouldBe uuid
        json.get("span_id").asText() shouldBe uuid
        json.hasNonNull("@timestamp") shouldBe true
        json.get("level").asText() shouldBe "INFO"
        json.get("logger_name").asText() shouldBe "com.fincore.observability.test"
    }
}
