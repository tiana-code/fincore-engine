// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.eval

import com.fincore.decision.domain.DecimalValue
import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.parser.RuleParser
import io.kotest.assertions.withClue
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

private const val RULE_CONDITIONS = 100
private const val WARMUP_ITERATIONS = 5_000
private const val MEASURED_ITERATIONS = 10_000
private const val P99_CEILING_MILLIS = 10L
private const val NANOS_PER_MILLI = 1_000_000L

class EvaluatorLatencyGateTest {
    private val evaluator = RuleEvaluator()
    private val rule = RuleParser().parse(buildDsl())
    private val input = buildInput()

    @Test
    fun `should produce a matching decision for the latency gate rule`() {
        evaluator.evaluate(rule, input).matched shouldBe true
    }

    @Test
    fun `should evaluate a 100 condition rule within the p99 latency ceiling`() {
        repeat(WARMUP_ITERATIONS) { evaluator.evaluate(rule, input) }

        val durations = LongArray(MEASURED_ITERATIONS)
        var matchedAll = true
        for (i in 0 until MEASURED_ITERATIONS) {
            val start = System.nanoTime()
            val result = evaluator.evaluate(rule, input)
            durations[i] = System.nanoTime() - start
            matchedAll = matchedAll && result.matched
        }

        matchedAll shouldBe true
        durations.sort()
        val p99Nanos = durations[durations.size - durations.size / 100 - 1]
        withClue("p99=${p99Nanos / NANOS_PER_MILLI}ms ceiling=${P99_CEILING_MILLIS}ms") {
            p99Nanos shouldBeLessThan P99_CEILING_MILLIS * NANOS_PER_MILLI
        }
    }

    private fun buildDsl(): String {
        val conditions = (1..RULE_CONDITIONS).joinToString(",") { """{"attr":"a$it","op":"gte","value":0}""" }
        return """{"condition":{"all":[$conditions]},"outcome":{"label":"OK"}}"""
    }

    private fun buildInput(): EvaluationInput = EvaluationInput((1..RULE_CONDITIONS).associate { "a$it" to DecimalValue(BigDecimal.ONE) })
}
