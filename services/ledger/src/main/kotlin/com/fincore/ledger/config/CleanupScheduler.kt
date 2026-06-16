// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.config

import com.fincore.ledger.application.CleanupService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "fincore.ledger.cleanup", name = ["enabled"], havingValue = "true", matchIfMissing = false)
class CleanupScheduler(
    private val cleanupService: CleanupService,
) {
    @Scheduled(cron = "\${fincore.ledger.cleanup.cron}")
    fun purgeExpired() {
        cleanupService.purge()
    }
}
