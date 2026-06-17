// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.domain

import java.util.Objects

sealed interface Operand

data class SingleOperand(
    val value: AttrValue,
) : Operand

data class ListOperand(
    val values: List<AttrValue>,
    val kind: ValueKind,
) : Operand

sealed interface Condition

data class Comparison(
    val attr: String,
    val operator: ComparisonOperator,
    val operand: Operand,
) : Condition

class MatchesComparison(
    val attr: String,
    val pattern: String,
) : Condition {
    val regex: Regex by lazy { Regex(pattern) }

    override fun equals(other: Any?): Boolean =
        this === other || (other is MatchesComparison && attr == other.attr && pattern == other.pattern)

    override fun hashCode(): Int = Objects.hash(attr, pattern)

    override fun toString(): String = "MatchesComparison(attr=$attr, pattern=$pattern)"

    companion object {
        const val TOKEN = "matches"
    }
}

data class LogicalGroup(
    val operator: LogicalOperator,
    val children: List<Condition>,
) : Condition
