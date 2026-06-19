// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.core.Currency
import com.fincore.core.Money
import com.fincore.events.OutboxStatus
import com.fincore.events.PaymentEvents
import com.fincore.payments.domain.enum.PaymentStatus
import com.fincore.payments.infrastructure.outbox.PaymentOutboxEventPublisherImpl
import com.fincore.payments.infrastructure.persistence.PaymentEventRepository
import com.fincore.payments.infrastructure.persistence.PaymentIdempotencyKeyRepository
import com.fincore.payments.infrastructure.persistence.PaymentOutboxEventRepository
import com.fincore.payments.infrastructure.persistence.PaymentPersistenceAdapter
import com.fincore.payments.infrastructure.persistence.PaymentRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ExtendWith(PostgresContainerExtension::class)
@Import(
    PaymentServiceImpl::class,
    PaymentMetrics::class,
    PaymentIdempotencyStore::class,
    PaymentPersistenceAdapter::class,
    PaymentOutboxEventPublisherImpl::class,
    PaymentServiceIT.ObjectMapperConfig::class,
)
class PaymentServiceIT(
    @Autowired private val service: PaymentService,
    @Autowired private val payments: PaymentRepository,
    @Autowired private val paymentEvents: PaymentEventRepository,
    @Autowired private val outbox: PaymentOutboxEventRepository,
    @Autowired private val keys: PaymentIdempotencyKeyRepository,
) {
    @AfterEach
    fun cleanUp() {
        paymentEvents.deleteAll()
        outbox.deleteAll()
        keys.deleteAll()
        payments.deleteAll()
    }

    @Test
    fun `should persist a payment an event log row and a pending outbox event when initiating`() {
        val payment = service.initiate(InitiatePaymentCommand("key-1", money(), "order-1"))

        payments.findById(payment.id.value).isPresent shouldBe true
        paymentEvents.count() shouldBe 1L
        val outboxRows = outbox.findAll()
        outboxRows.size shouldBe 1
        outboxRows.first().status shouldBe OutboxStatus.PENDING
        outboxRows.first().eventType shouldBe PaymentEvents.PaymentInitiated.fullType
    }

    @Test
    fun `should not create a second payment when initiating twice with the same key`() {
        val first = service.initiate(InitiatePaymentCommand("key-1", money(), "order-1"))
        val second = service.initiate(InitiatePaymentCommand("key-1", money(), "order-1"))

        second.id shouldBe first.id
        payments.count() shouldBe 1L
        outbox.count() shouldBe 1L
    }

    @Test
    fun `should list initiated payments as a page newest first`() {
        service.initiate(InitiatePaymentCommand("key-1", money(), "order-1"))
        service.initiate(InitiatePaymentCommand("key-2", money(), "order-2"))

        val page = service.list(0, 20)

        page.totalElements shouldBe 2L
        page.items.size shouldBe 2
        page.items.first().reference shouldBe "order-2"
    }

    @Test
    fun `should cancel an initiated payment and emit a cancelled event`() {
        val payment = service.initiate(InitiatePaymentCommand("key-1", money(), "order-1"))

        service.cancel(payment.id)

        payments.findById(payment.id.value).orElseThrow().status shouldBe PaymentStatus.CANCELLED
        outbox.findAll().any { it.eventType == PaymentEvents.PaymentCancelled.fullType } shouldBe true
    }

    private fun money(): Money = Money(BigDecimal("100.00"), Currency.USD)

    @TestConfiguration
    class ObjectMapperConfig {
        @Bean
        fun objectMapper(): ObjectMapper = ObjectMapper().findAndRegisterModules()

        @Bean fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()
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
