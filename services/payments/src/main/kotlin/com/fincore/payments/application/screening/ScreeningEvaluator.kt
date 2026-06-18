// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.screening

import com.fincore.decision.domain.DecimalValue
import com.fincore.decision.domain.DecisionResult
import com.fincore.decision.domain.DecisionRule
import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.domain.StringValue
import com.fincore.decision.eval.RuleEvaluator
import com.fincore.decision.parser.RuleParser
import com.fincore.payments.domain.Payment
import org.springframework.stereotype.Component

/**
 * Screens a payment in-process with the embedded decision engine. The configured rule is parsed fail-closed at
 * construction, so an invalid rule fails startup rather than silently approving.
 */
@Component
class ScreeningEvaluator(
    properties: PaymentScreeningProperties,
) {
    private val rule: DecisionRule = RuleParser().parse(properties.rule)
    private val approveLabel: String = properties.approveLabel
    private val evaluator = RuleEvaluator()

    fun evaluate(payment: Payment): ScreeningDecision {
        val input =
            EvaluationInput(
                mapOf(
                    "amount" to DecimalValue(payment.amount.amount),
                    "currency" to StringValue(payment.amount.currency.code),
                    "reference" to StringValue(payment.reference),
                ),
            )
        val result = evaluator.evaluate(rule, input)
        return if (result.matched && result.outcome?.label == approveLabel) {
            ScreeningDecision.Approve
        } else {
            ScreeningDecision.Decline(declineReason(result))
        }
    }

    private fun declineReason(result: DecisionResult): String =
        result.outcome
            ?.reasonCodes
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(",") ?: DECLINED

    private companion object {
        const val DECLINED = "screening declined"
    }
}
