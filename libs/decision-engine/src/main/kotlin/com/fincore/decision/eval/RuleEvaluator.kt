// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.eval

import com.fincore.decision.domain.AttrValue
import com.fincore.decision.domain.Comparison
import com.fincore.decision.domain.ComparisonOperator
import com.fincore.decision.domain.Condition
import com.fincore.decision.domain.ConditionTrace
import com.fincore.decision.domain.DecimalValue
import com.fincore.decision.domain.DecisionResult
import com.fincore.decision.domain.DecisionRule
import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.domain.ListOperand
import com.fincore.decision.domain.LogicalGroup
import com.fincore.decision.domain.LogicalOperator
import com.fincore.decision.domain.MatchesComparison
import com.fincore.decision.domain.Operand
import com.fincore.decision.domain.SingleOperand
import com.fincore.decision.domain.StringValue
import com.fincore.decision.domain.TraceDetail

class RuleEvaluator {
    fun evaluate(
        rule: DecisionRule,
        input: EvaluationInput,
    ): DecisionResult {
        val evaluated = evalCondition(rule.condition, input)
        return DecisionResult(
            matched = evaluated.result,
            outcome = if (evaluated.result) rule.outcome else null,
            trace = listOf(evaluated.trace),
        )
    }

    private fun evalCondition(
        condition: Condition,
        input: EvaluationInput,
    ): EvalResult =
        when (condition) {
            is LogicalGroup -> evalLogical(condition, input)
            is Comparison -> evalComparison(condition, input)
            is MatchesComparison -> evalMatches(condition, input)
        }

    private fun evalLogical(
        group: LogicalGroup,
        input: EvaluationInput,
    ): EvalResult {
        val childResults = group.children.map { evalCondition(it, input) }
        val anyTrue = childResults.any { it.result }
        val result =
            when (group.operator) {
                LogicalOperator.ALL -> childResults.all { it.result }
                LogicalOperator.ANY -> anyTrue
                LogicalOperator.NONE -> !anyTrue
            }
        return EvalResult(result, ConditionTrace(group.operator.key, result, children = childResults.map { it.trace }))
    }

    private fun evalComparison(
        comparison: Comparison,
        input: EvaluationInput,
    ): EvalResult {
        val attrValue =
            input.get(comparison.attr)
                ?: return absent(comparison.attr, comparison.operator.token)
        val result = applyOperator(comparison.operator, attrValue, comparison.operand)
        return evaluated("${comparison.attr} ${comparison.operator.token}", result)
    }

    private fun evalMatches(
        comparison: MatchesComparison,
        input: EvaluationInput,
    ): EvalResult {
        val attrValue =
            input.get(comparison.attr)
                ?: return absent(comparison.attr, MatchesComparison.TOKEN)
        val result = attrValue is StringValue && comparison.regex.matches(attrValue.value)
        return evaluated("${comparison.attr} ${MatchesComparison.TOKEN}", result)
    }

    private fun applyOperator(
        operator: ComparisonOperator,
        attrValue: AttrValue,
        operand: Operand,
    ): Boolean =
        when (operator) {
            ComparisonOperator.EQ -> equalsOperand(attrValue, operand)
            ComparisonOperator.NEQ -> !equalsOperand(attrValue, operand)
            ComparisonOperator.LT -> compareDecimal(attrValue, operand) { it < 0 }
            ComparisonOperator.LTE -> compareDecimal(attrValue, operand) { it <= 0 }
            ComparisonOperator.GT -> compareDecimal(attrValue, operand) { it > 0 }
            ComparisonOperator.GTE -> compareDecimal(attrValue, operand) { it >= 0 }
            ComparisonOperator.IN -> memberOf(attrValue, operand)
            ComparisonOperator.NOT_IN -> !memberOf(attrValue, operand)
        }

    private fun equalsOperand(
        attrValue: AttrValue,
        operand: Operand,
    ): Boolean = operand is SingleOperand && attrValue.semanticallyEquals(operand.value)

    private inline fun compareDecimal(
        attrValue: AttrValue,
        operand: Operand,
        predicate: (Int) -> Boolean,
    ): Boolean {
        if (attrValue !is DecimalValue || operand !is SingleOperand || operand.value !is DecimalValue) return false
        return predicate(attrValue.value.compareTo(operand.value.value))
    }

    private fun memberOf(
        attrValue: AttrValue,
        operand: Operand,
    ): Boolean = operand is ListOperand && operand.values.any { it.semanticallyEquals(attrValue) }

    private fun absent(
        attr: String,
        operatorToken: String,
    ): EvalResult = EvalResult(false, ConditionTrace("$attr $operatorToken", false, TraceDetail.ATTRIBUTE_ABSENT))

    private fun evaluated(
        description: String,
        result: Boolean,
    ): EvalResult = EvalResult(result, ConditionTrace(description, result))

    private data class EvalResult(
        val result: Boolean,
        val trace: ConditionTrace,
    )
}
