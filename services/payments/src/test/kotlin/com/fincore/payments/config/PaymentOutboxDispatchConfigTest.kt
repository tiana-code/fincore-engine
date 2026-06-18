// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.config

import com.fincore.eventbus.outbox.OutboxDispatcher
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class PaymentOutboxDispatchConfigTest {
    @Test
    fun `should not register a dispatcher bean when the dispatcher is disabled`() {
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of())
            .withUserConfiguration(PaymentOutboxDispatchConfig::class.java)
            .run { context ->
                context.getBeanNamesForType(OutboxDispatcher::class.java).size shouldBe 0
            }
    }
}
