// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.domain.enum

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PaymentStatusTest {
    @Test
    fun `should report terminal for settled failed and cancelled only`() {
        PaymentStatus.SETTLED.isTerminal() shouldBe true
        PaymentStatus.FAILED.isTerminal() shouldBe true
        PaymentStatus.CANCELLED.isTerminal() shouldBe true
        PaymentStatus.INITIATED.isTerminal() shouldBe false
        PaymentStatus.SCREENING.isTerminal() shouldBe false
        PaymentStatus.SUBMITTED.isTerminal() shouldBe false
    }

    @Test
    fun `should allow only screening or cancelled from initiated`() {
        PaymentStatus.INITIATED.canTransitionTo(PaymentStatus.SCREENING) shouldBe true
        PaymentStatus.INITIATED.canTransitionTo(PaymentStatus.CANCELLED) shouldBe true
        PaymentStatus.INITIATED.canTransitionTo(PaymentStatus.SUBMITTED) shouldBe false
        PaymentStatus.INITIATED.canTransitionTo(PaymentStatus.SETTLED) shouldBe false
    }

    @Test
    fun `should allow only settled or failed from submitted`() {
        PaymentStatus.SUBMITTED.canTransitionTo(PaymentStatus.SETTLED) shouldBe true
        PaymentStatus.SUBMITTED.canTransitionTo(PaymentStatus.FAILED) shouldBe true
        PaymentStatus.SUBMITTED.canTransitionTo(PaymentStatus.CANCELLED) shouldBe false
    }

    @Test
    fun `should allow no transition out of a terminal status`() {
        PaymentStatus.SETTLED.canTransitionTo(PaymentStatus.FAILED) shouldBe false
        PaymentStatus.FAILED.canTransitionTo(PaymentStatus.SUBMITTED) shouldBe false
        PaymentStatus.CANCELLED.canTransitionTo(PaymentStatus.INITIATED) shouldBe false
    }
}
