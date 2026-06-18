// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus

import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

class EventBusAutoConfigurationTest {
    private val runner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EventBusAutoConfiguration::class.java))
            .withPropertyValues("fincore.eventbus.bootstrap-servers=localhost:9092")

    @Test
    fun `should assemble an idempotent producer config from defaults`() {
        val config =
            EventBusAutoConfiguration.producerConfig(
                EventBusProperties(bootstrapServers = "localhost:9092", clientId = "ledger"),
            )

        config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] shouldBe "localhost:9092"
        config[ProducerConfig.CLIENT_ID_CONFIG] shouldBe "ledger"
        config[ProducerConfig.ACKS_CONFIG] shouldBe "all"
        config[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] shouldBe true
        config[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] shouldBe StringSerializer::class.java
        config[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] shouldBe StringSerializer::class.java
    }

    @Test
    fun `should add sasl keys when a secured protocol is configured`() {
        val secured =
            EventBusProperties(
                bootstrapServers = "localhost:9092",
                security =
                    EventBusProperties.Security(
                        protocol = "SASL_SSL",
                        saslMechanism = "SCRAM-SHA-512",
                        saslJaasConfig = "org.apache.kafka.common.security.scram.ScramLoginModule required;",
                    ),
            )

        val config = EventBusAutoConfiguration.producerConfig(secured)

        config[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] shouldBe "SASL_SSL"
        config[SaslConfigs.SASL_MECHANISM] shouldBe "SCRAM-SHA-512"
        config shouldContainKey SaslConfigs.SASL_JAAS_CONFIG
    }

    @Test
    fun `should omit sasl keys when the default plaintext protocol is used`() {
        val config = EventBusAutoConfiguration.producerConfig(EventBusProperties(bootstrapServers = "localhost:9092"))

        config[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] shouldBe "PLAINTEXT"
        config shouldNotContainKey SaslConfigs.SASL_MECHANISM
        config shouldNotContainKey SaslConfigs.SASL_JAAS_CONFIG
    }

    @Test
    fun `should register producer template and admin beans when bootstrap servers are set`() {
        runner.run { context ->
            context.getBean(ProducerFactory::class.java)
            context.getBean(KafkaTemplate::class.java)
            context.getBean(KafkaAdmin::class.java)
        }
    }

    @Test
    fun `should back off the template bean when the consumer defines its own`() {
        runner.withUserConfiguration(CustomTemplateConfig::class.java).run { context ->
            context.getBean(KafkaTemplate::class.java) shouldBe CustomTemplateConfig.CUSTOM
        }
    }

    @Configuration
    private class CustomTemplateConfig {
        @Bean
        fun kafkaTemplate(): KafkaTemplate<String, String> = CUSTOM

        companion object {
            val CUSTOM: KafkaTemplate<String, String> =
                KafkaTemplate(
                    DefaultKafkaProducerFactory(
                        mapOf<String, Any>(
                            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
                            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                        ),
                    ),
                )
        }
    }
}
