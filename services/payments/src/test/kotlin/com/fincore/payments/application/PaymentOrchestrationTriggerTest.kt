// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application

import com.fincore.core.PaymentId
import com.fincore.payments.application.event.PaymentInitiatedEvent
import com.fincore.payments.domain.Payment
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class PaymentOrchestrationTriggerTest {
    private val orchestrator = mockk<PaymentOrchestrator>()
    private val trigger = PaymentOrchestrationTrigger(orchestrator)
    private val id = PaymentId.generate()

    @Test
    fun `processes the payment when it is initiated`() {
        every { orchestrator.process(id) } returns mockk<Payment>()

        trigger.onPaymentInitiated(PaymentInitiatedEvent(id))

        verify { orchestrator.process(id) }
    }

    @Test
    fun `swallows an orchestration failure so the async worker survives`() {
        every { orchestrator.process(id) } throws RuntimeException("bank down")

        trigger.onPaymentInitiated(PaymentInitiatedEvent(id))

        verify { orchestrator.process(id) }
    }
}
