// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.screening

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "fincore.payments.screening")
data class PaymentScreeningProperties(
    val rule: String = DEFAULT_RULE,
    val approveLabel: String = DEFAULT_APPROVE_LABEL,
) {
    private companion object {
        const val DEFAULT_APPROVE_LABEL = "APPROVE"
        const val DEFAULT_RULE =
            """{"condition":{"attr":"amount","op":"gte","value":0},"outcome":{"label":"APPROVE"}}"""
    }
}
