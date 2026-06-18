// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application.outbox

import com.fincore.eventbus.EventBusAutoConfiguration
import com.fincore.eventbus.outbox.DispatchSummary
import com.fincore.eventbus.outbox.OutboxDispatchSettings
import com.fincore.eventbus.outbox.OutboxDispatcher
import com.fincore.events.OutboxStatus
import com.fincore.ledger.infrastructure.persistence.OutboxEventEntity
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import com.fincore.test.containers.PostgresContainerExtension
import com.fincore.test.containers.RedpandaContainerExtension
import io.kotest.matchers.shouldBe
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.DefaultKafkaProducerFactory
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
class OutboxDispatcherFailureIT(
    @Autowired private val claimStore: OutboxClaimStore,
    @Autowired private val repository: OutboxEventRepository,
    @Autowired private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    private val liveDispatcher = OutboxDispatcher(claimStore, kafkaTemplate, props(DEFAULT_MAX_ATTEMPTS))
    private val outageFactory = DefaultKafkaProducerFactory<String, String>(outageConfig())
    private val outageTemplate = KafkaTemplate(outageFactory)

    @AfterEach
    fun cleanUp() {
        repository.deleteAll()
        outageFactory.destroy()
    }

    @Test
    fun `should mark failed on broker outage then publish and deliver on recovery`() {
        val id = seed(OutboxStatus.PENDING, leasedAt = null, aggregateType = "OutageRecovery", aggregateId = "tx_outage")

        outageDispatcher(DEFAULT_MAX_ATTEMPTS).dispatch() shouldBe DispatchSummary(published = 0, failed = 1)
        repository.findById(id).orElseThrow().let { row ->
            row.status shouldBe OutboxStatus.FAILED
            row.attempts shouldBe 1
        }

        liveDispatcher.dispatch() shouldBe DispatchSummary(published = 1, failed = 0)
        repository.findById(id).orElseThrow().status shouldBe OutboxStatus.PUBLISHED
        consume("OutageRecovery").key() shouldBe "tx_outage"
    }

    @Test
    fun `should reclaim an orphaned publishing row and deliver it`() {
        val id =
            seed(
                OutboxStatus.PUBLISHING,
                leasedAt = Instant.now().minus(Duration.ofMinutes(ORPHAN_AGE_MINUTES)),
                aggregateType = "OrphanReclaim",
                aggregateId = "tx_orphan",
            )

        liveDispatcher.dispatch() shouldBe DispatchSummary(published = 1, failed = 0)
        repository.findById(id).orElseThrow().status shouldBe OutboxStatus.PUBLISHED
        consume("OrphanReclaim").key() shouldBe "tx_orphan"
    }

    @Test
    fun `should park a permanently failed event after exhausting retries and never reclaim it`() {
        val id = seed(OutboxStatus.PENDING, leasedAt = null, aggregateType = "Poison", aggregateId = "tx_poison")
        val outage = outageDispatcher(EXHAUST_ATTEMPTS)

        outage.dispatch()
        repository.findById(id).orElseThrow().status shouldBe OutboxStatus.FAILED
        outage.dispatch()
        repository.findById(id).orElseThrow().status shouldBe OutboxStatus.PERMANENTLY_FAILED

        liveDispatcher.dispatch() shouldBe DispatchSummary(published = 0, failed = 0)
        repository.findById(id).orElseThrow().status shouldBe OutboxStatus.PERMANENTLY_FAILED
    }

    private fun outageDispatcher(maxAttempts: Int): OutboxDispatcher = OutboxDispatcher(claimStore, outageTemplate, props(maxAttempts))

    private fun consume(aggregateType: String): org.apache.kafka.clients.consumer.ConsumerRecord<String, String> {
        val topic = "fincore.${aggregateType.lowercase()}"
        consumer().use { consumer ->
            consumer.subscribe(listOf(topic))
            val deadline = Instant.now().plusSeconds(POLL_TIMEOUT_SECONDS)
            while (Instant.now().isBefore(deadline)) {
                val records = consumer.poll(Duration.ofSeconds(1))
                if (!records.isEmpty) return records.records(topic).first()
            }
            error("no record received on $topic within ${POLL_TIMEOUT_SECONDS}s")
        }
    }

    private fun seed(
        status: OutboxStatus,
        leasedAt: Instant?,
        aggregateType: String,
        aggregateId: String,
    ): UUID =
        repository
            .save(
                OutboxEventEntity(
                    id = UUID.randomUUID(),
                    aggregateType = aggregateType,
                    aggregateId = aggregateId,
                    eventType = "com.fincore.ledger.transaction.posted.v1",
                    payload = "{\"id\":\"$aggregateId\"}",
                    status = status,
                    createdAt = Instant.now(),
                    publishedAt = null,
                    attempts = 0,
                    lastError = null,
                    leasedAt = leasedAt,
                ),
            ).id

    private fun consumer(): KafkaConsumer<String, String> {
        val props = Properties()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = RedpandaContainerExtension.bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = "dispatcher-failure-it"
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        return KafkaConsumer(props)
    }

    private companion object {
        const val DEFAULT_MAX_ATTEMPTS = 10
        const val EXHAUST_ATTEMPTS = 2
        const val MAX_BLOCK_MS = 500
        const val ORPHAN_AGE_MINUTES = 10L
        const val SEND_TIMEOUT_SECONDS = 10L
        const val POLL_TIMEOUT_SECONDS = 15L
        const val UNREACHABLE_BOOTSTRAP = "localhost:65000"

        fun props(maxAttempts: Int): OutboxDispatchSettings =
            OutboxDispatchSettings(
                maxAttempts = maxAttempts,
                sendTimeout = Duration.ofSeconds(SEND_TIMEOUT_SECONDS),
            )

        fun outageConfig(): Map<String, Any> =
            mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to UNREACHABLE_BOOTSTRAP,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.MAX_BLOCK_MS_CONFIG to MAX_BLOCK_MS,
            )

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
