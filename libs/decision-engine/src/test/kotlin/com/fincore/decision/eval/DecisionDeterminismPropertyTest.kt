// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.eval

import com.fincore.decision.support.RuleGenerators
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll

class DecisionDeterminismPropertyTest :
    FreeSpec({

        val evaluator = RuleEvaluator()

        "should yield an identical result when evaluating the same rule and input twice" {
            checkAll(RuleGenerators.rules(), RuleGenerators.inputs()) { rule, input ->
                evaluator.evaluate(rule, input) shouldBe evaluator.evaluate(rule, input)
            }
        }
    })
