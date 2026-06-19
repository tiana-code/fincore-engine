// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.aml

import com.fincore.decision.domain.DecisionDslException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class AmlEvaluatorTest {
    private fun view(amount: BigDecimal) = AmlTransactionView("subject-1", amount, "USD", Instant.now())

    @Test
    fun `should clear with the neutral default rule for a positive amount`() {
        val evaluator = AmlEvaluator(AmlRulesProperties())

        evaluator.evaluate(view(BigDecimal("100.00"))) shouldBe AmlDecision.Clear
    }

    @Test
    fun `should clear with the neutral default rule for a negative amount`() {
        val evaluator = AmlEvaluator(AmlRulesProperties())

        evaluator.evaluate(view(BigDecimal("-1.00"))) shouldBe AmlDecision.Clear
    }

    @Test
    fun `should flag when the configured rule yields the flag label`() {
        val rule = """{"condition":{"attr":"amount","op":"gte","value":0},"outcome":{"label":"FLAG","reasonCodes":["R1"]}}"""
        val evaluator = AmlEvaluator(AmlRulesProperties(rule = rule, flagLabel = "FLAG"))

        val decision = evaluator.evaluate(view(BigDecimal("100.00")))

        decision.shouldBeInstanceOf<AmlDecision.Flagged>().reasonCodes shouldBe listOf("R1")
    }

    @Test
    fun `should clear when the rule does not match`() {
        val rule = """{"condition":{"attr":"amount","op":"gte","value":1000000},"outcome":{"label":"FLAG"}}"""
        val evaluator = AmlEvaluator(AmlRulesProperties(rule = rule, flagLabel = "FLAG"))

        evaluator.evaluate(view(BigDecimal("1.00"))) shouldBe AmlDecision.Clear
    }

    @Test
    fun `should clear when the rule matches but its label is not the flag label`() {
        val rule = """{"condition":{"attr":"amount","op":"gte","value":0},"outcome":{"label":"WARN"}}"""
        val evaluator = AmlEvaluator(AmlRulesProperties(rule = rule, flagLabel = "FLAG"))

        evaluator.evaluate(view(BigDecimal("100.00"))) shouldBe AmlDecision.Clear
    }

    @Test
    fun `should fail closed when the configured rule is invalid`() {
        shouldThrow<DecisionDslException> { AmlEvaluator(AmlRulesProperties(rule = "not-json")) }
    }
}
