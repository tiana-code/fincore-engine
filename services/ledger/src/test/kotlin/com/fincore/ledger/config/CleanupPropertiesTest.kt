// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.config

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Duration

class CleanupPropertiesTest {
    private val runner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration::class.java))
            .withUserConfiguration(EnablingConfig::class.java)

    @EnableConfigurationProperties(CleanupProperties::class)
    class EnablingConfig

    @Test
    fun `should bind the defaults`() {
        runner.run { context ->
            val properties = context.getBean(CleanupProperties::class.java)
            properties.enabled shouldBe false
            properties.cron shouldBe "0 0 * * * *"
            properties.outboxRetention shouldBe Duration.ofDays(30)
        }
    }

    @Test
    fun `should bind overridden values from configuration`() {
        runner
            .withPropertyValues(
                "fincore.ledger.cleanup.enabled=true",
                "fincore.ledger.cleanup.outbox-retention=7d",
            ).run { context ->
                val properties = context.getBean(CleanupProperties::class.java)
                properties.enabled shouldBe true
                properties.outboxRetention shouldBe Duration.ofDays(7)
            }
    }

    @Test
    fun `should reject a negative outbox retention`() {
        runner.withPropertyValues("fincore.ledger.cleanup.outbox-retention=-1d").run { context ->
            context.startupFailure.shouldNotBeNull()
        }
    }
}
