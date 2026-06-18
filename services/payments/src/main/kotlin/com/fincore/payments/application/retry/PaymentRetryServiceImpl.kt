// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.retry

import com.fincore.core.PaymentId
import com.fincore.payments.application.PaymentOrchestrator
import com.fincore.payments.application.PaymentService
import com.fincore.payments.domain.enum.PaymentStatus
import com.fincore.payments.infrastructure.persistence.PaymentPersistenceAdapter
import com.fincore.payments.infrastructure.persistence.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * Re-routes payments stuck in SCREENING (a transient bank failure left them there) within a bounded age window,
 * failing them once past the deadline. NOT transactional: the bank re-submit happens inside [PaymentOrchestrator]
 * outside any transaction, and each transition is its own short transaction. Assumes a single scheduler instance;
 * overlapping ticks rely on an idempotent bank submit (the sandbox is deterministic by payment id).
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
        val stuck = paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.SCREENING, now.minus(properties.stuckAfter))
        val deadline = now.minus(properties.maxAge)
        for (entity in stuck) {
            val expired = entity.createdAt.isBefore(deadline)
            attempt(entity.id, expired) {
                if (expired) {
                    paymentService.markFailed(PaymentId(entity.id), DEADLINE_REASON)
                } else {
                    orchestrator.resume(adapter.toDomain(entity))
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
