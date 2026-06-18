// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "fincore.payments.dispatcher")
data class PaymentDispatcherProperties(
    val enabled: Boolean = false,
    val pollDelay: Duration = defaultPollDelay,
    val batchSize: Int = DEFAULT_BATCH_SIZE,
    val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    val leaseTimeout: Duration = defaultLeaseTimeout,
    val sendTimeout: Duration = defaultSendTimeout,
    val topicPrefix: String = "fincore",
) {
    init {
        require(batchSize > 0) { "fincore.payments.dispatcher.batch-size must be positive" }
        require(maxAttempts > 0) { "fincore.payments.dispatcher.max-attempts must be positive" }
        require(!pollDelay.isNegative && !pollDelay.isZero) { "fincore.payments.dispatcher.poll-delay must be positive" }
        require(!leaseTimeout.isNegative && !leaseTimeout.isZero) {
            "fincore.payments.dispatcher.lease-timeout must be positive"
        }
        require(!sendTimeout.isNegative && !sendTimeout.isZero) { "fincore.payments.dispatcher.send-timeout must be positive" }
    }

    private companion object {
        const val DEFAULT_BATCH_SIZE = 100
        const val DEFAULT_MAX_ATTEMPTS = 10
        val defaultPollDelay: Duration = Duration.ofSeconds(1)
        val defaultLeaseTimeout: Duration = Duration.ofMinutes(5)
        val defaultSendTimeout: Duration = Duration.ofSeconds(10)
    }
}
