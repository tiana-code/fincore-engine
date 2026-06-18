// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.config

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import java.time.Duration

class OutboxDispatcherPropertiesTest {
    private val runner =
        ApplicationContextRunner().withUserConfiguration(EnablePropertiesConfig::class.java)

    @Test
    fun `should expose safe opt-in-off defaults when no overrides are supplied`() {
        runner.run { context ->
            val props = context.getBean(OutboxDispatcherProperties::class.java)
            props.enabled shouldBe false
            props.batchSize shouldBe 100
            props.maxAttempts shouldBe 10
            props.pollDelay shouldBe Duration.ofSeconds(1)
            props.leaseTimeout shouldBe Duration.ofMinutes(5)
            props.sendTimeout shouldBe Duration.ofSeconds(10)
            props.topicPrefix shouldBe "fincore"
        }
    }

    @Test
    fun `should bind explicit overrides when supplied`() {
        runner
            .withPropertyValues(
                "fincore.ledger.dispatcher.enabled=true",
                "fincore.ledger.dispatcher.batch-size=50",
                "fincore.ledger.dispatcher.max-attempts=3",
                "fincore.ledger.dispatcher.lease-timeout=2m",
                "fincore.ledger.dispatcher.topic-prefix=ledger",
            ).run { context ->
                val props = context.getBean(OutboxDispatcherProperties::class.java)
                props.enabled shouldBe true
                props.batchSize shouldBe 50
                props.maxAttempts shouldBe 3
                props.leaseTimeout shouldBe Duration.ofMinutes(2)
                props.topicPrefix shouldBe "ledger"
            }
    }

    @Test
    fun `should reject a non-positive batch size`() {
        runner.withPropertyValues("fincore.ledger.dispatcher.batch-size=0").run { context ->
            context.startupFailure.shouldNotBeNull()
        }
    }

    @Test
    fun `should reject a non-positive max attempts`() {
        runner.withPropertyValues("fincore.ledger.dispatcher.max-attempts=0").run { context ->
            context.startupFailure.shouldNotBeNull()
        }
    }

    @Configuration
    @EnableConfigurationProperties(OutboxDispatcherProperties::class)
    private class EnablePropertiesConfig
}
