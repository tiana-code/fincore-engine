// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.retry

import com.fincore.core.PaymentId
import com.fincore.payments.application.PaymentOrchestrator
import com.fincore.payments.application.PaymentService
import com.fincore.payments.domain.enum.PaymentStatus
import com.fincore.payments.infrastructure.persistence.PaymentEntity
import com.fincore.payments.infrastructure.persistence.PaymentPersistenceAdapter
import com.fincore.payments.infrastructure.persistence.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * Advances payments that stalled in INITIATED or SCREENING (a crash dropped the after-commit orchestration, or a
 * transient bank failure left them screening) within a bounded age window, failing them once past the deadline. NOT
 * transactional: the bank call happens inside [PaymentOrchestrator] outside any transaction, and each transition is its
 * own short transaction. Assumes a single scheduler instance; overlapping ticks rely on an idempotent bank submit (the
 * sandbox is deterministic by payment id).
 */
@Service
class PaymentRetryServiceImpl(
    private val paymentRepository: PaymentRepository,
    private val adapter: PaymentPersistenceAdapter,
    private val orchestrator: PaymentOrchestrator,
    private val paymentService: PaymentService,
    private val properties: PaymentRetryProperties,
) : PaymentRetryService {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun retryStuck() {
        val now = Instant.now()
        val cutoff = now.minus(properties.stuckAfter)
        val deadline = now.minus(properties.maxAge)
        sweep(PaymentStatus.INITIATED, cutoff, deadline) { entity -> orchestrator.process(PaymentId(entity.id)) }
        sweep(PaymentStatus.SCREENING, cutoff, deadline) { entity -> orchestrator.resume(adapter.toDomain(entity)) }
    }

    private fun sweep(
        status: PaymentStatus,
        cutoff: Instant,
        deadline: Instant,
        recover: (PaymentEntity) -> Unit,
    ) {
        for (entity in paymentRepository.findByStatusAndCreatedAtBefore(status, cutoff)) {
            val expired = entity.createdAt.isBefore(deadline)
            attempt(entity.id, expired) {
                if (expired) {
                    paymentService.markFailed(PaymentId(entity.id), DEADLINE_REASON)
                } else {
                    recover(entity)
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught") // a single stuck payment must not abort the batch
    private fun attempt(
        paymentId: UUID,
        expired: Boolean,
        action: () -> Unit,
    ) {
        try {
            action()
        } catch (ex: Exception) {
            log
                .atWarn()
                .addKeyValue("paymentId", paymentId)
                .addKeyValue("expired", expired)
                .setCause(ex)
                .log("payment retry attempt failed")
        }
    }

    private companion object {
        const val DEADLINE_REASON = "retry deadline exceeded"
    }
}
