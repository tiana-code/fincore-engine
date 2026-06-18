// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.retry

import com.fincore.eventbus.EventBusAutoConfiguration
import io.kotest.matchers.shouldBe
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.redpanda.RedpandaContainer
import java.time.Duration
import java.time.Instant
import java.util.Properties

@Testcontainers
class RetryDlqIT {
    private val runner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EventBusAutoConfiguration::class.java))
            .withPropertyValues("fincore.eventbus.bootstrap-servers=${redpanda.bootstrapServers}")

    @Test
    fun `should route a recovered record to its dead-letter topic`() {
        runner.run { context ->
            @Suppress("UNCHECKED_CAST")
            val template = context.getBean(KafkaTemplate::class.java) as KafkaTemplate<String, String>
            val recoverer = context.getBean(DeadLetterPublishingRecoverer::class.java)
            val record = ConsumerRecord("orders", 0, 0L, "acc_1", "payload-1")

            recoverer.accept(record, RuntimeException("boom"))
            template.flush()

            val dead = consume("orders-dlt", expected = 1)
            dead.single().key() shouldBe "acc_1"
            dead.single().value() shouldBe "payload-1"
        }
    }

    @Test
    fun `should replay dead-lettered records back to the target topic preserving keys`() {
        runner.run { context ->
            @Suppress("UNCHECKED_CAST")
            val template = context.getBean(KafkaTemplate::class.java) as KafkaTemplate<String, String>
            val replayer = context.getBean(DeadLetterReplayer::class.java)
            template.send("payments-dlt", "p_1", "amount-1").get()
            template.send("payments-dlt", "p_2", "amount-2").get()
            template.flush()

            val replayed =
                dltConsumer("payments-dlt").use { consumer ->
                    replayer.replay(consumer, "payments", Duration.ofSeconds(POLL_TIMEOUT_SECONDS))
                }

            replayed shouldBe 2
            consume("payments", expected = 2).map { it.key() }.toSet() shouldBe setOf("p_1", "p_2")
        }
    }

    private fun dltConsumer(topic: String): KafkaConsumer<String, String> {
        val consumer = KafkaConsumer<String, String>(consumerProps("replay"))
        val partition = TopicPartition(topic, 0)
        consumer.assign(listOf(partition))
        consumer.seekToBeginning(listOf(partition))
        return consumer
    }

    private fun consume(
        topic: String,
        expected: Int,
    ): List<ConsumerRecord<String, String>> =
        KafkaConsumer<String, String>(consumerProps("verify")).use { consumer ->
            val partition = TopicPartition(topic, 0)
            consumer.assign(listOf(partition))
            consumer.seekToBeginning(listOf(partition))
            val collected = mutableListOf<ConsumerRecord<String, String>>()
            val deadline = Instant.now().plusSeconds(POLL_TIMEOUT_SECONDS)
            while (collected.size < expected && Instant.now().isBefore(deadline)) {
                consumer.poll(Duration.ofSeconds(1)).forEach { collected.add(it) }
            }
            collected
        }

    private fun consumerProps(group: String): Properties =
        Properties().apply {
            this[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = redpanda.bootstrapServers
            this[ConsumerConfig.GROUP_ID_CONFIG] = "retry-dlq-it-$group"
            this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        }

    companion object {
        private const val POLL_TIMEOUT_SECONDS = 15L

        @Container
        @JvmStatic
        val redpanda: RedpandaContainer = RedpandaContainer("redpandadata/redpanda:v24.2.4")
    }
}
