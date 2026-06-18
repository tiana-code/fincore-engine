// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "fincore.eventbus")
data class EventBusProperties(
    val bootstrapServers: String,
    val clientId: String = "fincore-eventbus",
    val security: Security = Security(),
) {
    data class Security(
        val protocol: String = "PLAINTEXT",
        val saslMechanism: String? = null,
        val saslJaasConfig: String? = null,
    )
}
