// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.eval

import com.fincore.decision.domain.Comparison
import com.fincore.decision.domain.ComparisonOperator
import com.fincore.decision.domain.Condition
import com.fincore.decision.domain.DecisionOutcome
import com.fincore.decision.domain.DecisionRule
import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.domain.ListOperand
import com.fincore.decision.domain.LogicalGroup
import com.fincore.decision.domain.LogicalOperator
import com.fincore.decision.domain.ValueKind
import com.fincore.decision.support.RuleGenerators
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll

class OperatorAlgebraPropertyTest :
    FreeSpec({

        val evaluator = RuleEvaluator()
        val outcome = DecisionOutcome("matched")

        fun matched(
            condition: Condition,
            input: EvaluationInput,
        ): Boolean = evaluator.evaluate(DecisionRule(condition, outcome), input).matched

        "should equal not-any when evaluating none over the same children" {
            checkAll(Arb.list(RuleGenerators.conditions(1), 1..2), RuleGenerators.inputs()) { children, input ->
                matched(LogicalGroup(LogicalOperator.NONE, children), input) shouldBe
                    !matched(LogicalGroup(LogicalOperator.ANY, children), input)
            }
        }

        "should equal all when evaluating not-any over negated children" {
            checkAll(Arb.list(RuleGenerators.conditions(1), 1..2), RuleGenerators.inputs()) { children, input ->
                val negated = children.map { LogicalGroup(LogicalOperator.NONE, listOf(it)) }
                matched(LogicalGroup(LogicalOperator.ALL, children), input) shouldBe
                    !matched(LogicalGroup(LogicalOperator.ANY, negated), input)
            }
        }

        "should be true iff a member when evaluating in, and not_in is its exact negation for a present attribute" {
            checkAll(RuleGenerators.decimal(), Arb.list(RuleGenerators.decimal(), 1..2)) { attr, members ->
                val input = EvaluationInput(mapOf("a" to attr))
                val expected = members.any { it.semanticallyEquals(attr) }
                matched(Comparison("a", ComparisonOperator.IN, ListOperand(members, ValueKind.DECIMAL)), input) shouldBe
                    expected
                matched(Comparison("a", ComparisonOperator.NOT_IN, ListOperand(members, ValueKind.DECIMAL)), input) shouldBe
                    !expected
            }
        }
    })
