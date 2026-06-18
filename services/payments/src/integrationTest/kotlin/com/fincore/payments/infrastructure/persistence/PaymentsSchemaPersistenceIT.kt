// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.infrastructure.persistence

import com.fincore.events.OutboxStatus
import com.fincore.payments.domain.enum.PaymentStatus
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ExtendWith(PostgresContainerExtension::class)
class PaymentsSchemaPersistenceIT(
    @Autowired private val payments: PaymentRepository,
    @Autowired private val paymentEvents: PaymentEventRepository,
    @Autowired private val webhooks: ProcessedWebhookRepository,
    @Autowired private val outbox: PaymentOutboxEventRepository,
) {
    @AfterEach
    fun cleanUp() {
        paymentEvents.deleteAll()
        webhooks.deleteAll()
        outbox.deleteAll()
        payments.deleteAll()
    }

    @Test
    fun `should round-trip a payment preserving amount precision status and version`() {
        val id = UUID.randomUUID()
        val amount = BigDecimal("100.123456789012345678")
        payments.saveAndFlush(
            PaymentEntity(id, "order-1", amount, "USD", PaymentStatus.INITIATED, Instant.now(), 0L),
        )

        val reloaded = payments.findById(id).orElseThrow()
        reloaded.reference shouldBe "order-1"
        reloaded.amount.compareTo(amount) shouldBe 0
        reloaded.currency.trim() shouldBe "USD"
        reloaded.status shouldBe PaymentStatus.INITIATED
        reloaded.version shouldBe 0L
    }

    @Test
    fun `should round-trip a payment event with a json payload`() {
        val id = UUID.randomUUID()
        paymentEvents.saveAndFlush(
            PaymentEventEntity(id, UUID.randomUUID(), "com.fincore.payment.initiated.v1", "{\"k\":\"v\"}", Instant.now()),
        )

        paymentEvents.findById(id).orElseThrow().eventType shouldBe "com.fincore.payment.initiated.v1"
    }

    @Test
    fun `should round-trip an outbox row with pending status and no lease`() {
        val id = UUID.randomUUID()
        outbox.saveAndFlush(
            PaymentOutboxEventEntity(
                id,
                "Payment",
                "pay_1",
                "com.fincore.payment.initiated.v1",
                "{}",
                OutboxStatus.PENDING,
                Instant.now(),
                null,
                0,
                null,
                null,
            ),
        )

        val reloaded = outbox.findById(id).orElseThrow()
        reloaded.status shouldBe OutboxStatus.PENDING
        reloaded.attempts shouldBe 0
        reloaded.leasedAt.shouldBeNull()
    }

    @Test
    fun `should keep a single row per delivery id`() {
        webhooks.saveAndFlush(ProcessedWebhookEntity("delivery-1", Instant.now()))
        webhooks.saveAndFlush(ProcessedWebhookEntity("delivery-1", Instant.now()))

        webhooks.count() shouldBe 1L
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
