// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.eval

import com.fincore.decision.domain.AttrValue
import com.fincore.decision.domain.BoolValue
import com.fincore.decision.domain.DecimalValue
import com.fincore.decision.domain.DecisionResult
import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.domain.StringValue
import com.fincore.decision.domain.TraceDetail
import com.fincore.decision.parser.RuleParser
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RuleEvaluatorTest {
    private val parser = RuleParser()
    private val evaluator = RuleEvaluator()

    private val nestedRule =
        """
        {"condition":{"all":[
            {"attr":"amount","op":"gte","value":100},
            {"any":[{"attr":"country","op":"in","value":["A","B"]},{"attr":"vip","op":"eq","value":true}]}
        ]},"outcome":{"label":"approve"}}
        """.trimIndent()

    private fun eval(
        json: String,
        input: Map<String, AttrValue>,
    ): DecisionResult = evaluator.evaluate(parser.parse(json), EvaluationInput(input))

    @Test
    fun `should match when nested all and any conditions are satisfied`() {
        val result =
            eval(
                nestedRule,
                mapOf("amount" to DecimalValue(BigDecimal("100")), "country" to StringValue("A"), "vip" to BoolValue(false)),
            )
        result.matched shouldBe true
        result.outcome?.label shouldBe "approve"
    }

    @Test
    fun `should not match when the ordered condition fails`() {
        val result =
            eval(
                nestedRule,
                mapOf("amount" to DecimalValue(BigDecimal.ONE), "country" to StringValue("A"), "vip" to BoolValue(true)),
            )
        result.matched shouldBe false
        result.outcome shouldBe null
        result.trace
            .first()
            .children
            .first()
            .result shouldBe false
    }

    @Test
    fun `should treat decimal scale as equal when comparing 2_0 against 2_00`() {
        val json = """{"condition":{"attr":"x","op":"eq","value":2.0},"outcome":{"label":"y"}}"""
        eval(json, mapOf("x" to DecimalValue(BigDecimal("2.00")))).matched shouldBe true
    }

    @Test
    fun `should degrade to false with attribute-absent trace when the attribute is missing`() {
        val json = """{"condition":{"attr":"score","op":"gt","value":1},"outcome":{"label":"y"}}"""
        val result = eval(json, emptyMap())
        result.matched shouldBe false
        result.trace.first().detail shouldBe TraceDetail.ATTRIBUTE_ABSENT
    }

    @Test
    fun `should evaluate false when a present attribute has the wrong kind`() {
        val json = """{"condition":{"attr":"amount","op":"gt","value":1},"outcome":{"label":"y"}}"""
        val result = eval(json, mapOf("amount" to StringValue("big")))
        result.matched shouldBe false
        result.trace.first().detail shouldBe TraceDetail.EVALUATED
    }

    @Test
    fun `should match full pattern when matches uses anchored semantics`() {
        val json = """{"condition":{"attr":"name","op":"matches","value":"a.+"},"outcome":{"label":"y"}}"""
        eval(json, mapOf("name" to StringValue("abc"))).matched shouldBe true
        eval(json, mapOf("name" to StringValue("xabc"))).matched shouldBe false
    }

    @Test
    fun `should produce equal results when evaluating twice`() {
        val result = eval(nestedRule, mapOf("amount" to DecimalValue(BigDecimal("100")), "vip" to BoolValue(true)))
        eval(nestedRule, mapOf("amount" to DecimalValue(BigDecimal("100")), "vip" to BoolValue(true))) shouldBe result
    }
}
