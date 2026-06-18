// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application.outbox

import com.fincore.eventbus.EventBusAutoConfiguration
import com.fincore.events.OutboxStatus
import com.fincore.ledger.config.OutboxDispatcherProperties
import com.fincore.ledger.infrastructure.persistence.OutboxEventEntity
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import com.fincore.test.containers.PostgresContainerExtension
import com.fincore.test.containers.RedpandaContainerExtension
import io.kotest.matchers.shouldBe
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.Properties
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ExtendWith(PostgresContainerExtension::class, RedpandaContainerExtension::class)
@Import(OutboxClaimStore::class, EventBusAutoConfiguration::class)
class OutboxDispatcherIT(
    @Autowired private val claimStore: OutboxClaimStore,
    @Autowired private val repository: OutboxEventRepository,
    @Autowired private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    private val dispatcher =
        OutboxDispatcher(
            claimStore,
            kafkaTemplate,
            OutboxDispatcherProperties(enabled = true, sendTimeout = Duration.ofSeconds(SEND_TIMEOUT_SECONDS)),
        )

    @AfterEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `should publish a pending event to the broker and mark it published`() {
        val payload = "{\"id\":\"env-1\",\"type\":\"com.fincore.ledger.transaction.posted.v1\"}"
        val id = seedPending("tx_42", payload)

        val summary = dispatcher.dispatch()

        summary shouldBe DispatchSummary(published = 1, failed = 0)
        val row = repository.findById(id).orElseThrow()
        row.status shouldBe OutboxStatus.PUBLISHED

        consumer().use { consumer ->
            consumer.subscribe(listOf("fincore.transaction"))
            val record = poll(consumer)
            record.key() shouldBe "tx_42"
            record.value() shouldBe payload
        }
    }

    private fun poll(consumer: KafkaConsumer<String, String>): org.apache.kafka.clients.consumer.ConsumerRecord<String, String> {
        val deadline = Instant.now().plusSeconds(POLL_TIMEOUT_SECONDS)
        while (Instant.now().isBefore(deadline)) {
            val records = consumer.poll(Duration.ofSeconds(1))
            if (!records.isEmpty) return records.records("fincore.transaction").first()
        }
        error("no record received within ${POLL_TIMEOUT_SECONDS}s")
    }

    private fun seedPending(
        aggregateId: String,
        payload: String,
    ): UUID =
        repository
            .save(
                OutboxEventEntity(
                    id = UUID.randomUUID(),
                    aggregateType = "Transaction",
                    aggregateId = aggregateId,
                    eventType = "com.fincore.ledger.transaction.posted.v1",
                    payload = payload,
                    status = OutboxStatus.PENDING,
                    createdAt = Instant.now(),
                    publishedAt = null,
                    attempts = 0,
                    lastError = null,
                    leasedAt = null,
                ),
            ).id

    private fun consumer(): KafkaConsumer<String, String> {
        val props = Properties()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = RedpandaContainerExtension.bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = "dispatcher-it"
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        return KafkaConsumer(props)
    }

    companion object {
        private const val SEND_TIMEOUT_SECONDS = 10L
        private const val POLL_TIMEOUT_SECONDS = 15L

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { PostgresContainerExtension.jdbcUrl }
            registry.add("spring.datasource.username") { PostgresContainerExtension.username }
            registry.add("spring.datasource.password") { PostgresContainerExtension.password }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
            registry.add("fincore.eventbus.bootstrap-servers") { RedpandaContainerExtension.bootstrapServers }
        }
    }
}
