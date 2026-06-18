// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class EventBusPropertiesTest {
    private val runner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EventBusAutoConfiguration::class.java))
            .withPropertyValues("fincore.eventbus.bootstrap-servers=localhost:9092")

    @Test
    fun `should expose safe defaults when no overrides are supplied`() {
        runner.run { context ->
            val props = context.getBean(EventBusProperties::class.java)
            props.bootstrapServers shouldBe "localhost:9092"
            props.clientId shouldBe "fincore-eventbus"
            props.security.protocol shouldBe "PLAINTEXT"
        }
    }

    @Test
    fun `should bind explicit overrides when supplied`() {
        runner
            .withPropertyValues(
                "fincore.eventbus.bootstrap-servers=broker-a:9092,broker-b:9092",
                "fincore.eventbus.client-id=ledger-dispatcher",
                "fincore.eventbus.security.protocol=SASL_SSL",
            ).run { context ->
                val props = context.getBean(EventBusProperties::class.java)
                props.bootstrapServers shouldBe "broker-a:9092,broker-b:9092"
                props.clientId shouldBe "ledger-dispatcher"
                props.security.protocol shouldBe "SASL_SSL"
            }
    }
}
