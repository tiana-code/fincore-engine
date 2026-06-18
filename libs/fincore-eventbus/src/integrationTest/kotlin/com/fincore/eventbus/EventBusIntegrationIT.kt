// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus

import io.kotest.matchers.shouldBe
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.core.KafkaTemplate
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.redpanda.RedpandaContainer
import java.time.Duration
import java.util.Properties

@Testcontainers
class EventBusIntegrationIT {
    private val runner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EventBusAutoConfiguration::class.java))
            .withPropertyValues("fincore.eventbus.bootstrap-servers=${redpanda.bootstrapServers}")

    @Test
    fun `should publish and consume a record through the configured producer`() {
        runner.run { context ->
            @Suppress("UNCHECKED_CAST")
            val template = context.getBean(KafkaTemplate::class.java) as KafkaTemplate<String, String>
            template.send(ROUND_TRIP_TOPIC, "acc_1", "posted").get(SEND_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)

            consumer().use { consumer ->
                consumer.subscribe(listOf(ROUND_TRIP_TOPIC))
                val records = consumer.poll(Duration.ofSeconds(POLL_TIMEOUT_SECONDS))
                val record = records.records(ROUND_TRIP_TOPIC).first()
                record.key() shouldBe "acc_1"
                record.value() shouldBe "posted"
            }
        }
    }

    @Test
    fun `should create and describe a topic through the admin client`() {
        runner.run { context ->
            val admin = context.getBean(KafkaAdmin::class.java)
            AdminClient.create(admin.configurationProperties).use { client ->
                client.createTopics(listOf(NewTopic(ADMIN_TOPIC, PARTITIONS, REPLICAS))).all().get()
                val described =
                    client
                        .describeTopics(listOf(ADMIN_TOPIC))
                        .allTopicNames()
                        .get()[ADMIN_TOPIC]
                described!!.partitions().size shouldBe PARTITIONS
            }
        }
    }

    private fun consumer(): KafkaConsumer<String, String> {
        val props = Properties()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = redpanda.bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = "eventbus-it"
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        return KafkaConsumer(props)
    }

    companion object {
        private const val ROUND_TRIP_TOPIC = "eventbus.it.roundtrip"
        private const val ADMIN_TOPIC = "eventbus.it.admin"
        private const val SEND_TIMEOUT_SECONDS = 10L
        private const val POLL_TIMEOUT_SECONDS = 10L
        private const val PARTITIONS = 3
        private const val REPLICAS: Short = 1

        @Container
        @JvmStatic
        val redpanda: RedpandaContainer = RedpandaContainer("redpandadata/redpanda:v24.2.4")
    }
}
