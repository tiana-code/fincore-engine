// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.core.PaymentId
import com.fincore.events.EventEnvelope
import com.fincore.events.EventType
import com.fincore.events.PaymentEvents
import com.fincore.payments.domain.Payment
import com.fincore.payments.domain.enum.PaymentStatus
import com.fincore.payments.domain.exception.PaymentNotFoundException
import com.fincore.payments.infrastructure.outbox.PaymentOutboxEventPublisher
import com.fincore.payments.infrastructure.persistence.PaymentEventEntity
import com.fincore.payments.infrastructure.persistence.PaymentEventRepository
import com.fincore.payments.infrastructure.persistence.PaymentPersistenceAdapter
import com.fincore.payments.infrastructure.persistence.PaymentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class PaymentServiceImpl(
    private val idempotencyStore: PaymentIdempotencyStore,
    private val paymentRepository: PaymentRepository,
    private val paymentEventRepository: PaymentEventRepository,
    private val adapter: PaymentPersistenceAdapter,
    private val outboxPublisher: PaymentOutboxEventPublisher,
    private val objectMapper: ObjectMapper,
) : PaymentService {
    override fun initiate(command: InitiatePaymentCommand): Payment {
        val keyHash = Sha256.hex(command.idempotencyKey)
        var attempt = 0
        while (true) {
            try {
                return idempotencyStore.reserveOrRun(keyHash) { createPayment(command) }
            } catch (race: PaymentIdempotencyRaceException) {
                attempt++
                if (attempt >= MAX_ATTEMPTS) throw PaymentConcurrencyException(race)
            }
        }
    }

    @Transactional
    override fun cancel(id: PaymentId): Payment = transition(id, PaymentStatus.CANCELLED, PaymentEvents.PaymentCancelled)

    @Transactional
    override fun screen(id: PaymentId): Payment {
        val entity = paymentRepository.findById(id.value).orElseThrow { PaymentNotFoundException(id) }
        val payment = adapter.toDomain(entity)
        payment.transitionTo(PaymentStatus.SCREENING)
        entity.status = payment.status
        paymentRepository.saveAndFlush(entity)
        return payment
    }

    @Transactional
    override fun markSubmitted(
        id: PaymentId,
        providerReference: String,
    ): Payment {
        val entity = paymentRepository.findById(id.value).orElseThrow { PaymentNotFoundException(id) }
        val payment = adapter.toDomain(entity)
        payment.transitionTo(PaymentStatus.SUBMITTED)
        entity.status = payment.status
        entity.providerReference = providerReference
        paymentRepository.saveAndFlush(entity)
        recordEvent(payment, PaymentEvents.PaymentScreened)
        return payment
    }

    @Transactional
    override fun markFailed(
        id: PaymentId,
        reason: String,
    ): Payment = transition(id, PaymentStatus.FAILED, PaymentEvents.PaymentFailed, reason)

    private fun transition(
        id: PaymentId,
        target: PaymentStatus,
        type: EventType,
        detail: String? = null,
    ): Payment {
        val entity = paymentRepository.findById(id.value).orElseThrow { PaymentNotFoundException(id) }
        val payment = adapter.toDomain(entity)
        payment.transitionTo(target)
        entity.status = payment.status
        paymentRepository.saveAndFlush(entity)
        recordEvent(payment, type, detail)
        return payment
    }

    private fun createPayment(command: InitiatePaymentCommand): Payment {
        val payment = Payment(PaymentId.generate(), command.amount, command.reference)
        paymentRepository.saveAndFlush(adapter.toNewEntity(payment, Instant.now()))
        recordEvent(payment, PaymentEvents.PaymentInitiated)
        return payment
    }

    private fun recordEvent(
        payment: Payment,
        type: EventType,
        detail: String? = null,
    ) {
        val now = Instant.now()
        val envelope =
            EventEnvelope.of(
                source = SOURCE,
                type = type,
                data = PaymentEventData.from(payment, detail),
                subject = payment.id.toString(),
            )
        paymentEventRepository.saveAndFlush(
            PaymentEventEntity(UUID.randomUUID(), payment.id.value, type.fullType, objectMapper.writeValueAsString(envelope), now),
        )
        outboxPublisher.publish(envelope, AGGREGATE_TYPE, payment.id.toString(), type.fullType, now)
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val SOURCE = "payments"
        const val AGGREGATE_TYPE = "Payment"
    }
}
