// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.parser

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.decision.domain.AttrValue
import com.fincore.decision.domain.BoolValue
import com.fincore.decision.domain.Comparison
import com.fincore.decision.domain.ComparisonOperator
import com.fincore.decision.domain.Condition
import com.fincore.decision.domain.DecimalValue
import com.fincore.decision.domain.DecisionDslException
import com.fincore.decision.domain.DecisionOutcome
import com.fincore.decision.domain.DecisionRule
import com.fincore.decision.domain.DslErrorCode
import com.fincore.decision.domain.ListOperand
import com.fincore.decision.domain.LogicalGroup
import com.fincore.decision.domain.LogicalOperator
import com.fincore.decision.domain.MatchesComparison
import com.fincore.decision.domain.Operand
import com.fincore.decision.domain.SingleOperand
import com.fincore.decision.domain.StringValue
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

private const val CONDITION = "condition"
private const val OUTCOME = "outcome"
private const val ATTR = "attr"
private const val OP = "op"
private const val VALUE = "value"
private const val LABEL = "label"
private const val REASON_CODES = "reasonCodes"

class RuleParser(
    private val mapper: ObjectMapper = ObjectMapper(),
) {
    fun parse(json: String): DecisionRule {
        val tree =
            try {
                mapper.readTree(json)
            } catch (ex: JsonProcessingException) {
                throw DecisionDslException(DslErrorCode.MISSING_FIELD, "malformed JSON rule", ex)
            }
        if (tree == null || tree.isNull) {
            fail(DslErrorCode.MISSING_FIELD, "rule JSON is empty")
        }
        return parseRule(tree)
    }

    private fun parseRule(root: JsonNode): DecisionRule {
        if (!root.isObject) fail(DslErrorCode.MISSING_FIELD, "rule must be an object")
        val conditionNode = root.get(CONDITION) ?: fail(DslErrorCode.MISSING_FIELD, "rule requires 'condition'")
        val outcomeNode = root.get(OUTCOME) ?: fail(DslErrorCode.MISSING_FIELD, "rule requires 'outcome'")
        return DecisionRule(parseCondition(conditionNode), parseOutcome(outcomeNode))
    }

    private fun parseOutcome(node: JsonNode): DecisionOutcome {
        val label =
            node.get(LABEL)?.takeIf { it.isTextual }?.textValue()
                ?: fail(DslErrorCode.MISSING_FIELD, "outcome requires a string 'label'")
        val reasons = node.get(REASON_CODES)?.takeIf { it.isArray }?.mapNotNull { it.textValue() } ?: emptyList()
        return DecisionOutcome(label, reasons)
    }

    private fun parseCondition(node: JsonNode): Condition {
        if (!node.isObject) fail(DslErrorCode.MISSING_FIELD, "condition must be an object")
        val logicalKey = LogicalOperator.entries.firstOrNull { node.has(it.key) }
        if (logicalKey != null) return parseLogical(node, logicalKey)
        if (node.has(ATTR) || node.has(OP)) return parseComparison(node)
        fail(DslErrorCode.UNKNOWN_LOGICAL_KEY, "condition has no known logical key or comparison fields")
    }

    private fun parseLogical(
        node: JsonNode,
        operator: LogicalOperator,
    ): Condition {
        val array = node.get(operator.key)
        if (array == null || !array.isArray) fail(DslErrorCode.MISSING_FIELD, "logical '${operator.key}' must be an array")
        if (array.isEmpty) fail(DslErrorCode.EMPTY_LOGICAL_GROUP, "logical '${operator.key}' must not be empty")
        return LogicalGroup(operator, array.map { parseCondition(it) })
    }

    private fun parseComparison(node: JsonNode): Condition {
        val attr =
            node.get(ATTR)?.takeIf { it.isTextual }?.textValue()
                ?: fail(DslErrorCode.MISSING_FIELD, "comparison requires a string 'attr'")
        val opToken =
            node.get(OP)?.takeIf { it.isTextual }?.textValue()
                ?: fail(DslErrorCode.MISSING_FIELD, "comparison requires a string 'op'")
        val valueNode = node.get(VALUE) ?: fail(DslErrorCode.MISSING_FIELD, "comparison requires 'value'")
        if (opToken == MatchesComparison.TOKEN) return buildMatches(attr, valueNode)
        val operator =
            ComparisonOperator.fromToken(opToken)
                ?: fail(DslErrorCode.UNKNOWN_OPERATOR, "unknown operator '$opToken'")
        return buildComparison(attr, operator, valueNode)
    }

    private fun buildComparison(
        attr: String,
        operator: ComparisonOperator,
        valueNode: JsonNode,
    ): Condition =
        when (operator) {
            ComparisonOperator.IN, ComparisonOperator.NOT_IN -> Comparison(attr, operator, parseListOperand(valueNode))
            ComparisonOperator.LT, ComparisonOperator.LTE, ComparisonOperator.GT, ComparisonOperator.GTE ->
                Comparison(attr, operator, parseOrderedOperand(operator, valueNode))
            else -> Comparison(attr, operator, SingleOperand(parseScalar(valueNode)))
        }

    private fun buildMatches(
        attr: String,
        valueNode: JsonNode,
    ): Condition {
        if (!valueNode.isTextual) fail(DslErrorCode.TYPE_MISMATCH, "'matches' requires a string pattern")
        val pattern = valueNode.textValue()
        validatePattern(pattern)
        return MatchesComparison(attr, pattern)
    }

    private fun validatePattern(pattern: String) {
        try {
            Pattern.compile(pattern)
        } catch (ex: PatternSyntaxException) {
            throw DecisionDslException(DslErrorCode.INVALID_PATTERN, "invalid regex pattern", ex)
        }
    }

    private fun parseOrderedOperand(
        operator: ComparisonOperator,
        valueNode: JsonNode,
    ): Operand {
        if (!valueNode.isNumber) fail(DslErrorCode.TYPE_MISMATCH, "'${operator.token}' requires a numeric value")
        return SingleOperand(DecimalValue(valueNode.decimalValue()))
    }

    private fun parseListOperand(valueNode: JsonNode): Operand {
        if (!valueNode.isArray || valueNode.isEmpty) {
            fail(DslErrorCode.TYPE_MISMATCH, "'in'/'not_in' requires a non-empty array")
        }
        val values = valueNode.map { parseScalar(it) }
        val kind = values.first().kind
        if (values.any { it.kind != kind }) fail(DslErrorCode.TYPE_MISMATCH, "'in'/'not_in' members must share one kind")
        return ListOperand(values, kind)
    }

    private fun parseScalar(node: JsonNode): AttrValue =
        when {
            node.isTextual -> StringValue(node.textValue())
            node.isBoolean -> BoolValue(node.booleanValue())
            node.isNumber -> DecimalValue(node.decimalValue())
            else -> fail(DslErrorCode.TYPE_MISMATCH, "unsupported value type")
        }

    private fun fail(
        code: DslErrorCode,
        message: String,
    ): Nothing = throw DecisionDslException(code, message)
}
