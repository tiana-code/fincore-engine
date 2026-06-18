// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.retry

/**
 * Suffix-based naming for the retry and dead-letter topics of a base topic. Generic - the caller
 * supplies the base topic; no business names are encoded here.
 */
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
