// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "fincore.eventbus")
data class EventBusProperties(
    val bootstrapServers: String,
    val clientId: String = "fincore-eventbus",
    val security: Security = Security(),
    val topics: List<TopicSpec> = emptyList(),
) {
    data class Security(
        val protocol: String = "PLAINTEXT",
        val saslMechanism: String? = null,
        val saslJaasConfig: String? = null,
    )

    data class TopicSpec(
        val name: String,
        val partitions: Int = DEFAULT_PARTITIONS,
        val replicas: Int = DEFAULT_REPLICAS,
        val configs: Map<String, String> = emptyMap(),
    ) {
        init {
            require(name.isNotBlank()) { "fincore.eventbus.topics[].name must not be blank" }
            require(partitions > 0) { "fincore.eventbus.topics[].partitions must be positive" }
            require(replicas > 0) { "fincore.eventbus.topics[].replicas must be positive" }
        }

        private companion object {
            const val DEFAULT_PARTITIONS = 3
            const val DEFAULT_REPLICAS = 1
        }
    }
}
