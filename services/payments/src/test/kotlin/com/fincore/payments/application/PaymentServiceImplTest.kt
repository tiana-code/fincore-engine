// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.core.Currency
import com.fincore.core.Money
import com.fincore.core.PaymentId
import com.fincore.events.PaymentEvents
import com.fincore.payments.application.event.PaymentInitiatedEvent
import com.fincore.payments.domain.Payment
import com.fincore.payments.domain.enum.PaymentStatus
import com.fincore.payments.domain.exception.PaymentDomainException
import com.fincore.payments.domain.exception.PaymentNotFoundException
import com.fincore.payments.infrastructure.outbox.PaymentOutboxEventPublisher
import com.fincore.payments.infrastructure.persistence.PaymentEntity
import com.fincore.payments.infrastructure.persistence.PaymentEventEntity
import com.fincore.payments.infrastructure.persistence.PaymentEventRepository
import com.fincore.payments.infrastructure.persistence.PaymentPersistenceAdapter
import com.fincore.payments.infrastructure.persistence.PaymentRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import java.util.UUID

class PaymentServiceImplTest {
    private val idempotencyStore = mockk<PaymentIdempotencyStore>()
    private val paymentRepository = mockk<PaymentRepository>(relaxed = true)
    private val paymentEventRepository = mockk<PaymentEventRepository>(relaxed = true)
    private val outboxPublisher = mockk<PaymentOutboxEventPublisher>(relaxed = true)
    private val objectMapper = mockk<ObjectMapper> { every { writeValueAsString(any()) } returns "{}" }
    private val metrics = mockk<PaymentMetrics>(relaxed = true)
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val service =
        PaymentServiceImpl(
            idempotencyStore,
            paymentRepository,
            paymentEventRepository,
            PaymentPersistenceAdapter(),
            outboxPublisher,
            objectMapper,
            metrics,
            eventPublisher,
        )

    init {
        every { paymentRepository.saveAndFlush(any<PaymentEntity>()) } answers { firstArg() }
        every { paymentEventRepository.saveAndFlush(any<PaymentEventEntity>()) } answers { firstArg() }
    }

    @Test
    fun `should persist and emit an initiated event when initiating a payment`() {
        every { idempotencyStore.reserveOrRun(any(), any()) } answers { secondArg<() -> Payment>().invoke() }

        val payment = service.initiate(InitiatePaymentCommand("key-1", money(), "order-1"))

        payment.status shouldBe PaymentStatus.INITIATED
        verify { outboxPublisher.publish(any(), "Payment", any(), PaymentEvents.PaymentInitiated.fullType, any()) }
        verify { eventPublisher.publishEvent(PaymentInitiatedEvent(payment.id)) }
    }

    @Test
    fun `should fail after exhausting retries when the idempotency key keeps racing`() {
        every { idempotencyStore.reserveOrRun(any(), any()) } throws PaymentIdempotencyRaceException(RuntimeException())

        shouldThrow<PaymentConcurrencyException> { service.initiate(InitiatePaymentCommand("key-1", money(), "order-1")) }

        verify(exactly = MAX_RETRIES) { idempotencyStore.reserveOrRun(any(), any()) }
    }

    @Test
    fun `should transition to cancelled and emit a cancelled event when cancelling`() {
        val id = UUID.randomUUID()
        every { paymentRepository.findById(id) } returns Optional.of(entity(id, PaymentStatus.INITIATED))

        val payment = service.cancel(PaymentId(id))

        payment.status shouldBe PaymentStatus.CANCELLED
        verify { outboxPublisher.publish(any(), "Payment", any(), PaymentEvents.PaymentCancelled.fullType, any()) }
    }

    @Test
    fun `should reject cancelling a terminal payment and emit nothing`() {
        val id = UUID.randomUUID()
        every { paymentRepository.findById(id) } returns Optional.of(entity(id, PaymentStatus.SETTLED))

        shouldThrow<PaymentDomainException> { service.cancel(PaymentId(id)) }

        verify(exactly = 0) { outboxPublisher.publish(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `should raise not found when cancelling an unknown payment`() {
        val id = UUID.randomUUID()
        every { paymentRepository.findById(id) } returns Optional.empty()

        shouldThrow<PaymentNotFoundException> { service.cancel(PaymentId(id)) }
    }

    private fun money(): Money = Money(BigDecimal("100.00"), Currency.USD)

    private fun entity(
        id: UUID,
        status: PaymentStatus,
    ): PaymentEntity = PaymentEntity(id, "order-1", BigDecimal("100.00"), "USD", status, Instant.now(), 0L)

    private companion object {
        const val MAX_RETRIES = 3
    }
}
