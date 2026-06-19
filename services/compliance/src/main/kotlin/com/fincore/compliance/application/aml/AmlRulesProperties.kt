// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.aml

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "fincore.compliance.aml")
data class AmlRulesProperties(
    val rule: String = NEUTRAL_RULE,
    val flagLabel: String = DEFAULT_FLAG_LABEL,
) {
    private companion object {
        const val DEFAULT_FLAG_LABEL = "FLAG"

        // Neutral default: always yields a non-flag label, so an unconfigured engine never flags. No business rule.
        const val NEUTRAL_RULE =
            """{"condition":{"attr":"amount","op":"gte","value":0},"outcome":{"label":"CLEAR"}}"""
    }
}
