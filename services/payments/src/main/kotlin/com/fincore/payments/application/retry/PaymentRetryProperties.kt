// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.retry

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "fincore.payments.retry")
data class PaymentRetryProperties(
    val enabled: Boolean = false,
    val cron: String = "0 */5 * * * *",
    val stuckAfter: Duration = defaultStuckAfter,
    val maxAge: Duration = defaultMaxAge,
) {
    init {
        require(!stuckAfter.isNegative) { "fincore.payments.retry.stuck-after must not be negative" }
        require(!maxAge.isNegative) { "fincore.payments.retry.max-age must not be negative" }
        require(stuckAfter <= maxAge) { "fincore.payments.retry.stuck-after must not exceed max-age" }
    }

    private companion object {
        val defaultStuckAfter: Duration = Duration.ofMinutes(5)
        val defaultMaxAge: Duration = Duration.ofHours(1)
    }
}
