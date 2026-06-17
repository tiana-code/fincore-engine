// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.domain

enum class ComparisonOperator(
    val token: String,
) {
    EQ("eq"),
    NEQ("neq"),
    LT("lt"),
    LTE("lte"),
    GT("gt"),
    GTE("gte"),
    IN("in"),
    NOT_IN("not_in"),
    ;

    companion object {
        fun fromToken(token: String): ComparisonOperator? = entries.find { it.token == token }
    }
}

enum class LogicalOperator(
    val key: String,
) {
    ALL("all"),
    ANY("any"),
    NONE("none"),
}
