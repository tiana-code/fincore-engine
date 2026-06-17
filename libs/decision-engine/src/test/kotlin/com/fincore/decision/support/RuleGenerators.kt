// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.support

import com.fincore.decision.domain.AttrValue
import com.fincore.decision.domain.BoolValue
import com.fincore.decision.domain.Comparison
import com.fincore.decision.domain.ComparisonOperator
import com.fincore.decision.domain.Condition
import com.fincore.decision.domain.DecimalValue
import com.fincore.decision.domain.DecisionOutcome
import com.fincore.decision.domain.DecisionRule
import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.domain.ListOperand
import com.fincore.decision.domain.LogicalGroup
import com.fincore.decision.domain.LogicalOperator
import com.fincore.decision.domain.Operand
import com.fincore.decision.domain.SingleOperand
import com.fincore.decision.domain.StringValue
import com.fincore.decision.domain.ValueKind
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int

object RuleGenerators {
    private val attrNames = listOf("amount", "country", "vip", "score")
    private val stringPool = listOf("a", "b", "c")
    private val comparisonOps = ComparisonOperator.entries

    fun decimal(): Arb<AttrValue> = arbitrary { DecimalValue(Arb.int(0..100).bind().toBigDecimal()) }

    fun attrValues(): Arb<AttrValue> =
        arbitrary {
            when (Arb.int(0..2).bind()) {
                0 -> StringValue(Arb.element(stringPool).bind())
                1 -> DecimalValue(Arb.int(0..100).bind().toBigDecimal())
                else -> BoolValue(Arb.boolean().bind())
            }
        }

    fun conditions(depth: Int): Arb<Condition> =
        arbitrary {
            if (depth <= 0 || !Arb.boolean().bind()) comparison().bind() else logicalGroup(depth).bind()
        }

    fun rules(): Arb<DecisionRule> = arbitrary { DecisionRule(conditions(2).bind(), DecisionOutcome("matched")) }

    fun inputs(): Arb<EvaluationInput> =
        arbitrary {
            val attributes =
                attrNames
                    .mapNotNull { name ->
                        if (Arb.boolean().bind()) name to attrValues().bind() else null
                    }.toMap()
            EvaluationInput(attributes)
        }

    private fun comparison(): Arb<Condition> =
        arbitrary {
            val attr = Arb.element(attrNames).bind()
            val operator = Arb.element(comparisonOps).bind()
            Comparison(attr, operator, operand(operator).bind())
        }

    private fun logicalGroup(depth: Int): Arb<Condition> =
        arbitrary {
            val operator = Arb.element(LogicalOperator.entries).bind()
            val count = Arb.int(1..2).bind()
            LogicalGroup(operator, (0 until count).map { conditions(depth - 1).bind() })
        }

    private fun operand(operator: ComparisonOperator): Arb<Operand> =
        arbitrary {
            when (operator) {
                ComparisonOperator.IN, ComparisonOperator.NOT_IN ->
                    ListOperand((0 until Arb.int(1..2).bind()).map { decimal().bind() }, ValueKind.DECIMAL)
                ComparisonOperator.LT, ComparisonOperator.LTE, ComparisonOperator.GT, ComparisonOperator.GTE ->
                    SingleOperand(decimal().bind())
                else -> SingleOperand(attrValues().bind())
            }
        }
}
