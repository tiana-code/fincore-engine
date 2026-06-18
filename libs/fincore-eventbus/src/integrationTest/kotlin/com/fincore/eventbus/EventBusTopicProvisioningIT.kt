// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus

import io.kotest.matchers.shouldBe
import org.apache.kafka.clients.admin.AdminClient
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.kafka.core.KafkaAdmin
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.redpanda.RedpandaContainer

@Testcontainers
class EventBusTopicProvisioningIT {
    private val runner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EventBusAutoConfiguration::class.java))
            .withPropertyValues(
                "fincore.eventbus.bootstrap-servers=${redpanda.bootstrapServers}",
                "fincore.eventbus.topics[0].name=$TOPIC",
                "fincore.eventbus.topics[0].partitions=$PARTITIONS",
                "fincore.eventbus.topics[0].replicas=1",
            )

    @Test
    fun `should provision a declared topic with the requested partition count when the context starts`() {
        runner.run { context ->
            partitionCount(context.getBean(KafkaAdmin::class.java)) shouldBe PARTITIONS
        }
    }

    @Test
    fun `should re-provision an already existing topic without failing`() {
        runner.run { context ->
            partitionCount(context.getBean(KafkaAdmin::class.java)) shouldBe PARTITIONS
        }
        // Second context start re-runs provisioning against the same broker and topic: KafkaAdmin
        // treats the existing same-partition topic as a no-op (create-only, idempotent, INV-1).
        runner.run { context ->
            partitionCount(context.getBean(KafkaAdmin::class.java)) shouldBe PARTITIONS
        }
    }

    private fun partitionCount(kafkaAdmin: KafkaAdmin): Int =
        AdminClient.create(kafkaAdmin.configurationProperties).use { client ->
            val description =
                client.describeTopics(listOf(TOPIC)).allTopicNames().get()[TOPIC]
                    ?: error("topic $TOPIC was not provisioned on the broker")
            description.partitions().size
        }

    companion object {
        private const val TOPIC = "fincore.it.provisioned"
        private const val PARTITIONS = 4

        @Container
        @JvmStatic
        val redpanda: RedpandaContainer = RedpandaContainer("redpandadata/redpanda:v24.2.4")
    }
}
