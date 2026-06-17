// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.domain

enum class DslErrorCode {
    UNKNOWN_OPERATOR,
    UNKNOWN_LOGICAL_KEY,
    EMPTY_LOGICAL_GROUP,
    TYPE_MISMATCH,
    INVALID_PATTERN,
    MISSING_FIELD,
}

class DecisionDslException(
    val code: DslErrorCode,
    override val message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
