// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.config

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class PaymentRetrySchedulerTest {
    @Test
    fun `should not register a retry scheduler bean when retry is disabled`() {
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of())
            .withUserConfiguration(PaymentRetryScheduler::class.java)
            .run { context ->
                context.getBeanNamesForType(PaymentRetryScheduler::class.java).size shouldBe 0
            }
    }
}
