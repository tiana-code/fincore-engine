// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.retry

import com.fincore.eventbus.EventBusAutoConfiguration
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.kafka.listener.DefaultErrorHandler
import java.time.Duration

class RetryDlqPropertiesTest {
    private val runner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EventBusAutoConfiguration::class.java))
            .withPropertyValues("fincore.eventbus.bootstrap-servers=localhost:9092")

    @Test
    fun `should expose safe defaults when no overrides are supplied`() {
        runner.run { context ->
            val props = context.getBean(RetryDlqProperties::class.java)
            props.maxAttempts shouldBe 3
            props.backoffMultiplier shouldBe 2.0
            props.retrySuffix shouldBe "-retry"
            props.deadLetterSuffix shouldBe "-dlt"
        }
    }

    @Test
    fun `should bind explicit overrides when supplied`() {
        runner
            .withPropertyValues(
                "fincore.eventbus.retry.max-attempts=5",
                "fincore.eventbus.retry.initial-backoff=250ms",
                "fincore.eventbus.retry.backoff-multiplier=3.0",
                "fincore.eventbus.retry.dead-letter-suffix=.DLT",
            ).run { context ->
                val props = context.getBean(RetryDlqProperties::class.java)
                props.maxAttempts shouldBe 5
                props.initialBackoff shouldBe Duration.ofMillis(250)
                props.backoffMultiplier shouldBe 3.0
                props.deadLetterSuffix shouldBe ".DLT"
            }
    }

    @Test
    fun `should reject a non-positive max attempts`() {
        runner.withPropertyValues("fincore.eventbus.retry.max-attempts=0").run { context ->
            context.startupFailure.shouldNotBeNull()
        }
    }

    @Test
    fun `should reject a backoff multiplier below one`() {
        runner.withPropertyValues("fincore.eventbus.retry.backoff-multiplier=0.5").run { context ->
            context.startupFailure.shouldNotBeNull()
        }
    }

    @Test
    fun `should register a default error handler when a kafka template is present`() {
        runner.run { context ->
            context.getBean(DefaultErrorHandler::class.java).shouldNotBeNull()
        }
    }
}
