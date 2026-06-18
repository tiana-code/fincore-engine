// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.domain

import com.fincore.core.Currency
import com.fincore.core.Money
import com.fincore.core.PaymentId
import com.fincore.payments.domain.enum.PaymentStatus
import com.fincore.payments.domain.exception.PaymentDomainException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class PaymentTest {
    @Test
    fun `should apply a legal transition`() {
        val payment = payment()

        payment.transitionTo(PaymentStatus.SCREENING)

        payment.status shouldBe PaymentStatus.SCREENING
    }

    @Test
    fun `should reject an illegal transition and leave the status unchanged`() {
        val payment = payment()

        shouldThrow<PaymentDomainException> { payment.transitionTo(PaymentStatus.SETTLED) }

        payment.status shouldBe PaymentStatus.INITIATED
    }

    @Test
    fun `should reject a blank reference`() {
        shouldThrow<PaymentDomainException> { payment(reference = " ") }
    }

    @Test
    fun `should reject an over-long reference`() {
        shouldThrow<PaymentDomainException> { payment(reference = "a".repeat(OVER_MAX_REFERENCE)) }
    }

    @Test
    fun `should report terminal only in a terminal status`() {
        payment(status = PaymentStatus.SETTLED).isTerminal() shouldBe true
        payment().isTerminal() shouldBe false
    }

    private fun payment(
        reference: String = "order-1",
        status: PaymentStatus = PaymentStatus.INITIATED,
    ): Payment = Payment(PaymentId.generate(), Money(BigDecimal("100.00"), Currency.USD), reference, status)

    private companion object {
        const val OVER_MAX_REFERENCE = 141
    }
}
