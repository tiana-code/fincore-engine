// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.domain.StringValue
import com.fincore.decision.eval.RuleEvaluator
import com.fincore.decision.parser.RuleParser
import com.fincore.decision.store.config.DecisionApiProperties
import com.fincore.decision.store.exception.EvaluationTimeoutException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors

class BoundedEvaluatorTest {
    private val parser = RuleParser()
    private val watchdog = Executors.newSingleThreadScheduledExecutor()
    private val evaluator = BoundedEvaluator(RuleEvaluator(), watchdog, DecisionApiProperties(evaluationTimeoutMillis = BUDGET))

    @AfterEach
    fun tearDown() {
        watchdog.shutdownNow()
    }

    @Test
    fun `should abort and clear the interrupt flag when a match exceeds the budget`() {
        val rule = parser.parse("""{"condition":{"attr":"s","op":"matches","value":"(.*,){1,100}Z"},"outcome":{"label":"x"}}""")
        val input = EvaluationInput(mapOf("s" to StringValue("a,".repeat(REDOS_REPEATS))))

        shouldThrow<EvaluationTimeoutException> { evaluator.evaluate(rule, input) }
        Thread.currentThread().isInterrupted shouldBe false
    }

    @Test
    fun `should return the result when evaluation completes within the budget`() {
        val rule = parser.parse("""{"condition":{"attr":"s","op":"matches","value":"[a-z]+"},"outcome":{"label":"ok"}}""")
        val input = EvaluationInput(mapOf("s" to StringValue("hello")))

        evaluator.evaluate(rule, input).matched shouldBe true
    }

    private companion object {
        const val BUDGET = 100L
        const val REDOS_REPEATS = 25
    }
}
