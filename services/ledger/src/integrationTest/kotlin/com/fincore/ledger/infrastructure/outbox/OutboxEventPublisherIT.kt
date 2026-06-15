// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fincore.events.EventEnvelope
import com.fincore.events.LedgerEvents
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestComponent
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(PostgresContainerExtension::class)
@Import(
    OutboxEventPublisherImpl::class,
    OutboxEventPublisherIT.JacksonConfig::class,
    OutboxEventPublisherIT.PublishHelper::class,
)
class OutboxEventPublisherIT(
    @Autowired private val publisher: OutboxEventPublisher,
    @Autowired private val outboxRepository: OutboxEventRepository,
    @Autowired private val helper: OutboxEventPublisherIT.PublishHelper,
) {
    @TestConfiguration
    class JacksonConfig {
        @Bean
        fun objectMapper(): ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    @TestComponent
    class PublishHelper(
        @Autowired private val publisher: OutboxEventPublisher,
    ) {
        private val envelope: EventEnvelope<Map<String, String>> =
            EventEnvelope.of(
                source = "ledger",
                type = LedgerEvents.TransactionPosted,
                data = mapOf("ref" to "ref-1"),
                subject = "tx_01",
                correlationId = "corr-1",
            )

        @Transactional
        fun commit(aggregateId: String) {
            publisher.publish(
                envelope,
                "Transaction",
                aggregateId,
                LedgerEvents.TransactionPosted.fullType,
                Instant.parse("2026-06-15T10:00:00Z"),
            )
        }

        @Transactional
        fun rollback(aggregateId: String) {
            publisher.publish(
                envelope,
                "Transaction",
                aggregateId,
                LedgerEvents.TransactionPosted.fullType,
                Instant.parse("2026-06-15T10:00:00Z"),
            )
            throw RuntimeException("forced rollback")
        }
    }

    @AfterEach
    fun cleanUp() {
        outboxRepository.deleteAll()
    }

    @Test
    fun `should commit one outbox row when the surrounding transaction commits`() {
        helper.commit("tx-commit-1")

        val rows = outboxRepository.findAll()
        rows.size shouldBe 1
        rows.first().aggregateId shouldBe "tx-commit-1"
        rows.first().aggregateType shouldBe "Transaction"
        rows.first().eventType shouldBe LedgerEvents.TransactionPosted.fullType
        rows.first().attempts shouldBe 0
        rows.first().publishedAt shouldBe null
    }

    @Test
    fun `should leave no outbox row when the surrounding transaction rolls back`() {
        shouldThrow<RuntimeException> {
            helper.rollback("tx-rollback-1")
        }

        outboxRepository.findAll().size shouldBe 0
    }

    @Test
    fun `should throw IllegalStateException when publish runs with no active transaction`() {
        val envelope: EventEnvelope<Map<String, String>> =
            EventEnvelope.of(
                source = "ledger",
                type = LedgerEvents.TransactionPosted,
                data = mapOf("ref" to "direct"),
                correlationId = null,
            )

        shouldThrow<IllegalStateException> {
            publisher.publish(
                envelope,
                "Transaction",
                "tx-no-tx",
                LedgerEvents.TransactionPosted.fullType,
                Instant.parse("2026-06-15T10:00:00Z"),
            )
        }

        outboxRepository.findAll().size shouldBe 0
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { PostgresContainerExtension.jdbcUrl }
            registry.add("spring.datasource.username") { PostgresContainerExtension.username }
            registry.add("spring.datasource.password") { PostgresContainerExtension.password }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
        }
    }
}
