// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.kafka.core.KafkaAdmin

class EventBusTopicsTest {
    private val runner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EventBusAutoConfiguration::class.java))
            .withPropertyValues("fincore.eventbus.bootstrap-servers=localhost:9092")

    @Test
    fun `should map topic specs to new topics preserving name partitions replicas and configs`() {
        val properties =
            EventBusProperties(
                bootstrapServers = "localhost:9092",
                topics =
                    listOf(
                        EventBusProperties.TopicSpec(
                            name = "fincore.transaction",
                            partitions = PARTITIONS,
                            replicas = REPLICAS,
                            configs = mapOf("retention.ms" to RETENTION_MS),
                        ),
                    ),
            )

        val topics = EventBusAutoConfiguration.newTopics(properties)

        topics shouldHaveSize 1
        topics[0].name() shouldBe "fincore.transaction"
        topics[0].numPartitions() shouldBe PARTITIONS
        topics[0].replicationFactor() shouldBe REPLICAS.toShort()
        topics[0].configs()["retention.ms"] shouldBe RETENTION_MS
    }

    @Test
    fun `should bind a topic list with a dotted config key when supplied`() {
        runner
            .withPropertyValues(
                "fincore.eventbus.topics[0].name=fincore.transaction",
                "fincore.eventbus.topics[0].partitions=$PARTITIONS",
                "fincore.eventbus.topics[0].configs.[retention.ms]=$RETENTION_MS",
            ).run { context ->
                val spec = context.getBean(EventBusProperties::class.java).topics.single()
                spec.name shouldBe "fincore.transaction"
                spec.partitions shouldBe PARTITIONS
                spec.configs["retention.ms"] shouldBe RETENTION_MS
            }
    }

    @Test
    fun `should default partitions and replicas when only a name is supplied`() {
        runner.withPropertyValues("fincore.eventbus.topics[0].name=t").run { context ->
            val spec = context.getBean(EventBusProperties::class.java).topics.single()
            spec.partitions shouldBe DEFAULT_PARTITIONS
            spec.replicas shouldBe DEFAULT_REPLICAS
        }
    }

    @Test
    fun `should reject a blank topic name`() {
        runner
            .withPropertyValues(
                "fincore.eventbus.topics[0].name=",
                "fincore.eventbus.topics[0].partitions=$PARTITIONS",
            ).run { context ->
                context.startupFailure.shouldNotBeNull()
            }
    }

    @Test
    fun `should reject non-positive partitions`() {
        runner
            .withPropertyValues(
                "fincore.eventbus.topics[0].name=t",
                "fincore.eventbus.topics[0].partitions=0",
            ).run { context ->
                context.startupFailure.shouldNotBeNull()
            }
    }

    @Test
    fun `should reject non-positive replicas`() {
        runner
            .withPropertyValues(
                "fincore.eventbus.topics[0].name=t",
                "fincore.eventbus.topics[0].replicas=0",
            ).run { context ->
                context.startupFailure.shouldNotBeNull()
            }
    }

    @Test
    fun `should provision no topics when none are declared`() {
        runner.run { context ->
            val properties = context.getBean(EventBusProperties::class.java)
            properties.topics.shouldBeEmpty()
            EventBusAutoConfiguration.newTopics(properties).shouldBeEmpty()
            context.getBean(KafkaAdmin.NewTopics::class.java).shouldNotBeNull()
        }
    }

    private companion object {
        const val PARTITIONS = 6
        const val REPLICAS = 2
        const val DEFAULT_PARTITIONS = 3
        const val DEFAULT_REPLICAS = 1
        const val RETENTION_MS = "604800000"
    }
}
