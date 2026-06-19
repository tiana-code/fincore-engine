// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.aml

import com.fincore.decision.domain.DecimalValue
import com.fincore.decision.domain.DecisionRule
import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.domain.StringValue
import com.fincore.decision.eval.RuleEvaluator
import com.fincore.decision.parser.RuleParser
import org.springframework.stereotype.Component

/**
 * Evaluates a transaction against the configured AML rule with the embedded decision engine. The rule is parsed
 * fail-closed at construction, so an invalid rule fails startup rather than silently clearing.
 */
@Component
class AmlEvaluator(
    properties: AmlRulesProperties,
) {
    private val rule: DecisionRule = RuleParser().parse(properties.rule)
    private val flagLabel: String = properties.flagLabel
    private val evaluator = RuleEvaluator()

    fun evaluate(view: AmlTransactionView): AmlDecision {
        val input =
            EvaluationInput(
                mapOf(
                    "amount" to DecimalValue(view.amount),
                    "currency" to StringValue(view.currency),
                    "subjectReference" to StringValue(view.subjectReference),
                ),
            )
        val result = evaluator.evaluate(rule, input)
        return if (result.matched && result.outcome?.label == flagLabel) {
            AmlDecision.Flagged(result.outcome?.reasonCodes.orEmpty())
        } else {
            AmlDecision.Clear
        }
    }
}
