// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.retry

import com.fincore.core.Currency
import com.fincore.core.Money
import com.fincore.core.PaymentId
import com.fincore.payments.application.PaymentOrchestrator
import com.fincore.payments.application.PaymentService
import com.fincore.payments.domain.Payment
import com.fincore.payments.domain.enum.PaymentStatus
import com.fincore.payments.infrastructure.persistence.PaymentEntity
import com.fincore.payments.infrastructure.persistence.PaymentPersistenceAdapter
import com.fincore.payments.infrastructure.persistence.PaymentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID

class PaymentRetryServiceTest {
    private val paymentRepository = mockk<PaymentRepository>()
    private val orchestrator = mockk<PaymentOrchestrator>(relaxed = true)
    private val paymentService = mockk<PaymentService>(relaxed = true)
    private val service =
        PaymentRetryServiceImpl(
            paymentRepository,
            PaymentPersistenceAdapter(),
            orchestrator,
            paymentService,
            PaymentRetryProperties(enabled = true, stuckAfter = Duration.ofMinutes(5), maxAge = Duration.ofHours(1)),
        )

    init {
        every { paymentRepository.findByStatusAndCreatedAtBefore(any(), any()) } returns emptyList()
    }

    @Test
    fun `should resume a payment stuck in screening within the retry window`() {
        screening(listOf(stuck(PaymentStatus.SCREENING, Duration.ofMinutes(10))))

        service.retryStuck()

        verify { orchestrator.resume(any()) }
        verify(exactly = 0) { paymentService.markFailed(any(), any()) }
    }

    @Test
    fun `should fail a screening payment past the retry deadline without resuming`() {
        screening(listOf(stuck(PaymentStatus.SCREENING, Duration.ofHours(2))))

        service.retryStuck()

        verify { paymentService.markFailed(any(), "retry deadline exceeded") }
        verify(exactly = 0) { orchestrator.resume(any()) }
    }

    @Test
    fun `should process a payment stuck in initiated within the retry window`() {
        initiated(listOf(stuck(PaymentStatus.INITIATED, Duration.ofMinutes(10))))

        service.retryStuck()

        verify { orchestrator.process(any()) }
        verify(exactly = 0) { paymentService.markFailed(any(), any()) }
        verify(exactly = 0) { orchestrator.resume(any()) }
    }

    @Test
    fun `should fail an initiated payment past the retry deadline without processing`() {
        initiated(listOf(stuck(PaymentStatus.INITIATED, Duration.ofHours(2))))

        service.retryStuck()

        verify { paymentService.markFailed(any(), "retry deadline exceeded") }
        verify(exactly = 0) { orchestrator.process(any()) }
        verify(exactly = 0) { orchestrator.resume(any()) }
    }

    @Test
    fun `should continue the batch when resuming one payment throws`() {
        screening(
            listOf(
                stuck(PaymentStatus.SCREENING, Duration.ofMinutes(10)),
                stuck(PaymentStatus.SCREENING, Duration.ofMinutes(20)),
            ),
        )
        every { orchestrator.resume(any()) } throws RuntimeException("bank down") andThen
            Payment(PaymentId.generate(), Money(BigDecimal("100.00"), Currency.USD), "order-1", PaymentStatus.SUBMITTED)

        service.retryStuck()

        verify(exactly = 2) { orchestrator.resume(any()) }
    }

    private fun screening(entities: List<PaymentEntity>) {
        every { paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.SCREENING, any()) } returns entities
    }

    private fun initiated(entities: List<PaymentEntity>) {
        every { paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.INITIATED, any()) } returns entities
    }

    private fun stuck(
        status: PaymentStatus,
        age: Duration,
    ): PaymentEntity =
        PaymentEntity(
            UUID.randomUUID(),
            "order-1",
            BigDecimal("100.00"),
            "USD",
            status,
            Instant.now().minus(age),
            0L,
        )
}
