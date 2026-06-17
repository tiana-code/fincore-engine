// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.eval

import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.domain.StringValue
import com.fincore.decision.parser.RuleParser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RegexMatchInterruptTest {
    private val parser = RuleParser()
    private val evaluator = RuleEvaluator()
    private val matchesRule = """{"condition":{"attr":"name","op":"matches","value":"[a-z]+"},"outcome":{"label":"ok"}}"""

    @Test
    fun `should abort a match when the evaluating thread is interrupted`() {
        val rule = parser.parse(matchesRule)
        val input = EvaluationInput(mapOf("name" to StringValue("abcdef")))
        Thread.currentThread().interrupt()
        try {
            shouldThrow<RegexMatchInterruptedException> { evaluator.evaluate(rule, input) }
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun `should match normally when the thread is not interrupted`() {
        val rule = parser.parse(matchesRule)
        val input = EvaluationInput(mapOf("name" to StringValue("abcdef")))

        evaluator.evaluate(rule, input).matched shouldBe true
    }
}
