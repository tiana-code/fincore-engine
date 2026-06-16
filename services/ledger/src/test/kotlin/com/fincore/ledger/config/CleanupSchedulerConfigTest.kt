// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.config

import com.fincore.ledger.application.CleanupResult
import com.fincore.ledger.application.CleanupService
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

class CleanupSchedulerConfigTest {
    private val runner =
        ApplicationContextRunner().withUserConfiguration(SchedulerTestConfig::class.java)

    @EnableConfigurationProperties(CleanupProperties::class)
    @Import(CleanupScheduler::class)
    class SchedulerTestConfig {
        @Bean
        fun cleanupService(): CleanupService =
            object : CleanupService {
                override fun purge(): CleanupResult = CleanupResult(0, 0)
            }
    }

    @Test
    fun `should not register the scheduler when cleanup is disabled`() {
        runner.withPropertyValues("fincore.ledger.cleanup.enabled=false").run { context ->
            context.getBeanNamesForType(CleanupScheduler::class.java).size shouldBe 0
        }
    }

    @Test
    fun `should register the scheduler when cleanup is enabled`() {
        runner
            .withPropertyValues(
                "fincore.ledger.cleanup.enabled=true",
                "fincore.ledger.cleanup.cron=0 0 * * * *",
            ).run { context ->
                context.getBeanNamesForType(CleanupScheduler::class.java).size shouldBe 1
            }
    }
}
