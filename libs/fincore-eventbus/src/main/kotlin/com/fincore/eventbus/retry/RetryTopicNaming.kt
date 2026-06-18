// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.retry

class RetryTopicNaming(
    private val retrySuffix: String,
    private val deadLetterSuffix: String,
) {
    fun deadLetterTopic(baseTopic: String): String {
        require(baseTopic.isNotBlank()) { "baseTopic must not be blank" }
        return "$baseTopic$deadLetterSuffix"
    }

    fun retryTopic(
        baseTopic: String,
        tier: Int,
    ): String {
        require(baseTopic.isNotBlank()) { "baseTopic must not be blank" }
        return "$baseTopic$retrySuffix-$tier"
    }
}
