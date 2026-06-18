// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application

import com.fincore.payments.domain.Payment
import com.fincore.payments.infrastructure.persistence.PaymentIdempotencyKeyEntity
import com.fincore.payments.infrastructure.persistence.PaymentIdempotencyKeyRepository
import com.fincore.payments.infrastructure.persistence.PaymentPersistenceAdapter
import com.fincore.payments.infrastructure.persistence.PaymentRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

// Raised when a concurrent caller already committed this idempotency key. The orchestrator retries in a fresh
// transaction, where the winner's committed key is found and its payment replayed.
internal class PaymentIdempotencyRaceException(
    cause: Throwable,
) : RuntimeException(cause)

@Component
class PaymentIdempotencyStore(
    private val keyRepository: PaymentIdempotencyKeyRepository,
    private val paymentRepository: PaymentRepository,
    private val adapter: PaymentPersistenceAdapter,
) {
    @Transactional
    fun reserveOrRun(
        keyHash: String,
        action: () -> Payment,
    ): Payment {
        val existing = keyRepository.findById(keyHash).orElse(null)
        if (existing != null) {
            val entity =
                paymentRepository.findById(existing.paymentId).orElseThrow {
                    IllegalStateException("Idempotency key references a missing payment ${existing.paymentId}")
                }
            return adapter.toDomain(entity)
        }
        val payment = action()
        try {
            keyRepository.saveAndFlush(PaymentIdempotencyKeyEntity(keyHash, payment.id.value, Instant.now()))
        } catch (duplicate: DataIntegrityViolationException) {
            throw PaymentIdempotencyRaceException(duplicate)
        }
        return payment
    }
}
