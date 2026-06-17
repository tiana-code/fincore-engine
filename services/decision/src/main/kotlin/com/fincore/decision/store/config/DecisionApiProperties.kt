// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "fincore.decision.api")
data class DecisionApiProperties(
    val maxDslChars: Int = DEFAULT_MAX_DSL_CHARS,
    val evaluationTimeoutMillis: Long = DEFAULT_EVALUATION_TIMEOUT_MILLIS,
    val maxInputAttributes: Int = DEFAULT_MAX_INPUT_ATTRIBUTES,
    val maxInputValueChars: Int = DEFAULT_MAX_INPUT_VALUE_CHARS,
    val maxLogPageSize: Int = DEFAULT_MAX_LOG_PAGE_SIZE,
    val maxReplayInputs: Int = DEFAULT_MAX_REPLAY_INPUTS,
) {
    private companion object {
        const val DEFAULT_MAX_DSL_CHARS = 8192
        const val DEFAULT_EVALUATION_TIMEOUT_MILLIS = 200L
        const val DEFAULT_MAX_INPUT_ATTRIBUTES = 64
        const val DEFAULT_MAX_INPUT_VALUE_CHARS = 4096
        const val DEFAULT_MAX_LOG_PAGE_SIZE = 100
        const val DEFAULT_MAX_REPLAY_INPUTS = 200
    }
}
