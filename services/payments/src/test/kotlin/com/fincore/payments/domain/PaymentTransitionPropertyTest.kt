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

class PaymentTransitionPropertyTest {
    @Test
    fun `should transition iff the edge is legal for every status pair`() {
        PaymentStatus.entries.forEach { from ->
            PaymentStatus.entries.forEach { to ->
                val payment = Payment(PaymentId.generate(), Money(BigDecimal("100.00"), Currency.USD), "order-1", from)

                if (from.canTransitionTo(to)) {
                    payment.transitionTo(to)
                    payment.status shouldBe to
                } else {
                    shouldThrow<PaymentDomainException> { payment.transitionTo(to) }
                    payment.status shouldBe from
                }
            }
        }
    }
}
