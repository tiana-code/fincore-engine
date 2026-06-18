// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.retry

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "fincore.eventbus.retry")
data class RetryDlqProperties(
    val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    val initialBackoff: Duration = defaultInitialBackoff,
    val backoffMultiplier: Double = DEFAULT_MULTIPLIER,
    val maxBackoff: Duration = defaultMaxBackoff,
    val retrySuffix: String = "-retry",
    val deadLetterSuffix: String = "-dlt",
) {
    init {
        require(maxAttempts >= 1) { "fincore.eventbus.retry.max-attempts must be at least 1" }
        require(backoffMultiplier >= MIN_MULTIPLIER) { "fincore.eventbus.retry.backoff-multiplier must be >= 1.0" }
        require(!initialBackoff.isNegative && !initialBackoff.isZero) {
            "fincore.eventbus.retry.initial-backoff must be positive"
        }
        require(!maxBackoff.isNegative && !maxBackoff.isZero) { "fincore.eventbus.retry.max-backoff must be positive" }
        require(retrySuffix.isNotBlank()) { "fincore.eventbus.retry.retry-suffix must not be blank" }
        require(deadLetterSuffix.isNotBlank()) { "fincore.eventbus.retry.dead-letter-suffix must not be blank" }
    }

    private companion object {
        const val DEFAULT_MAX_ATTEMPTS = 3
        const val DEFAULT_MULTIPLIER = 2.0
        const val MIN_MULTIPLIER = 1.0
        val defaultInitialBackoff: Duration = Duration.ofSeconds(1)
        val defaultMaxBackoff: Duration = Duration.ofSeconds(10)
    }
}
