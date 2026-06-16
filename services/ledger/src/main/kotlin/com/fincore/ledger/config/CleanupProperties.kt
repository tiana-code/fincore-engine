// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "fincore.ledger.cleanup")
data class CleanupProperties(
    val enabled: Boolean = false,
    val cron: String = "0 0 * * * *",
    val outboxRetention: Duration = defaultRetention,
) {
    init {
        require(!outboxRetention.isNegative) {
            "fincore.ledger.cleanup.outbox-retention must not be negative"
        }
    }

    private companion object {
        val defaultRetention: Duration = Duration.ofDays(30)
    }
}
