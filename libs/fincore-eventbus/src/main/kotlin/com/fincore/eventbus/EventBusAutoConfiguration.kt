// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@AutoConfiguration
@ConditionalOnProperty(prefix = "fincore.eventbus", name = ["bootstrap-servers"])
@EnableConfigurationProperties(EventBusProperties::class)
class EventBusAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun producerFactory(properties: EventBusProperties): ProducerFactory<String, String> =
        DefaultKafkaProducerFactory(producerConfig(properties))

    @Bean
    @ConditionalOnMissingBean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, String>): KafkaTemplate<String, String> = KafkaTemplate(producerFactory)

    @Bean
    @ConditionalOnMissingBean
    fun kafkaAdmin(properties: EventBusProperties): KafkaAdmin = KafkaAdmin(adminConfig(properties))

    companion object {
        // Durability is a correctness invariant for ledger events, not a deployment knob:
        // acks=all + enable.idempotence=true are fixed so the producer can never be silently
        // weakened into dropping or duplicating events. Connection/security stay configurable.
        private const val DURABLE_ACKS = "all"

        fun producerConfig(properties: EventBusProperties): Map<String, Any> =
            buildMap {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.bootstrapServers)
                put(ProducerConfig.CLIENT_ID_CONFIG, properties.clientId)
                put(ProducerConfig.ACKS_CONFIG, DURABLE_ACKS)
                put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true)
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
                putAll(securityConfig(properties.security))
            }

        fun adminConfig(properties: EventBusProperties): Map<String, Any> =
            buildMap {
                put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, properties.bootstrapServers)
                put(AdminClientConfig.CLIENT_ID_CONFIG, properties.clientId)
                putAll(securityConfig(properties.security))
            }

        private fun securityConfig(security: EventBusProperties.Security): Map<String, Any> =
            buildMap {
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, security.protocol)
                security.saslMechanism?.let { put(SaslConfigs.SASL_MECHANISM, it) }
                security.saslJaasConfig?.let { put(SaslConfigs.SASL_JAAS_CONFIG, it) }
            }
    }
}
