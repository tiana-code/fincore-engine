// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision

import com.fincore.decision.domain.DecimalValue
import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.eval.RuleEvaluator
import com.fincore.decision.parser.RuleParser
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

private const val RULE =
    """{"condition":{"attr":"age","op":"gte","value":18},"outcome":{"label":"APPROVE"}}"""

class LibraryEmbeddingTest {
    private val parser = RuleParser()
    private val evaluator = RuleEvaluator()

    @Test
    fun `should approve through the public api when an adult is supplied`() {
        val result = evaluator.evaluate(parser.parse(RULE), input("age", "20"))

        result.matched shouldBe true
        result.outcome?.label shouldBe "APPROVE"
    }

    @Test
    fun `should not match through the public api when a minor is supplied`() {
        val result = evaluator.evaluate(parser.parse(RULE), input("age", "16"))

        result.matched shouldBe false
        result.outcome shouldBe null
    }

    private fun input(
        attr: String,
        value: String,
    ) = EvaluationInput(mapOf(attr to DecimalValue(BigDecimal(value))))
}
