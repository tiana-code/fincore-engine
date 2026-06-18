// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.outbox

import java.time.Duration

data class OutboxDispatchSettings(
    val batchSize: Int = DEFAULT_BATCH_SIZE,
    val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    val leaseTimeout: Duration = defaultLeaseTimeout,
    val sendTimeout: Duration = defaultSendTimeout,
    val topicPrefix: String = DEFAULT_TOPIC_PREFIX,
) {
    private companion object {
        const val DEFAULT_BATCH_SIZE = 100
        const val DEFAULT_MAX_ATTEMPTS = 10
        const val DEFAULT_TOPIC_PREFIX = "fincore"
        val defaultLeaseTimeout: Duration = Duration.ofMinutes(5)
        val defaultSendTimeout: Duration = Duration.ofSeconds(10)
    }
}
