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

class IdempotencyPropertiesTest {
    private val runner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration::class.java))
            .withUserConfiguration(EnablingConfig::class.java)

    @EnableConfigurationProperties(IdempotencyProperties::class)
    class EnablingConfig

    @Test
    fun `should bind the default retry ceiling to three`() {
        runner.run { context ->
            context.getBean(IdempotencyProperties::class.java).maxAttempts shouldBe 3
        }
    }

    @Test
    fun `should bind an overridden retry ceiling from configuration`() {
        runner.withPropertyValues("fincore.ledger.idempotency.max-attempts=7").run { context ->
            context.getBean(IdempotencyProperties::class.java).maxAttempts shouldBe 7
        }
    }

    @Test
    fun `should reject a retry ceiling below one`() {
        runner.withPropertyValues("fincore.ledger.idempotency.max-attempts=0").run { context ->
            context.startupFailure.shouldNotBeNull()
        }
    }
}
