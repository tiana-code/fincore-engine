// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "fincore.decision.api")
data class DecisionApiProperties(
    val maxDslChars: Int = DEFAULT_MAX_DSL_CHARS,
) {
    private companion object {
        const val DEFAULT_MAX_DSL_CHARS = 8192
    }
}
