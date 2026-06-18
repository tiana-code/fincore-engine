// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.screening

import com.fincore.core.Currency
import com.fincore.core.Money
import com.fincore.core.PaymentId
import com.fincore.decision.domain.DecisionDslException
import com.fincore.payments.domain.Payment
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ScreeningEvaluatorTest {
    @Test
    fun `should approve when the default rule matches the payment`() {
        val evaluator = ScreeningEvaluator(PaymentScreeningProperties())

        evaluator.evaluate(payment()) shouldBe ScreeningDecision.Approve
    }

    @Test
    fun `should decline when the configured rule does not match`() {
        val rule = """{"condition":{"attr":"amount","op":"gte","value":1000000},"outcome":{"label":"APPROVE"}}"""
        val evaluator = ScreeningEvaluator(PaymentScreeningProperties(rule = rule))

        evaluator.evaluate(payment()).shouldBeInstanceOf<ScreeningDecision.Decline>()
    }

    @Test
    fun `should fail construction when the configured rule is invalid`() {
        shouldThrow<DecisionDslException> { ScreeningEvaluator(PaymentScreeningProperties(rule = "{not-valid")) }
    }

    private fun payment(): Payment = Payment(PaymentId.generate(), Money(BigDecimal("100.00"), Currency.USD), "order-1")
}
