// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.exception

import com.fincore.decision.domain.DslErrorCode

class RuleNotFoundException(
    ruleKey: String,
) : RuntimeException("rule not found: $ruleKey")

class DuplicateRuleKeyException(
    ruleKey: String,
    cause: Throwable? = null,
) : RuntimeException("rule already exists: $ruleKey", cause)

class InvalidRuleDslException(
    val dslCode: DslErrorCode,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class DslTooLargeException(
    val maxChars: Int,
) : RuntimeException("rule document exceeds the maximum of $maxChars characters")

class VersionConflictException(
    ruleKey: String,
    cause: Throwable? = null,
) : RuntimeException("concurrent version publish conflict for rule: $ruleKey", cause)

class RuleNotActiveException(
    ruleKey: String,
) : RuntimeException("rule has no active version: $ruleKey")

class EvaluationTimeoutException(
    budgetMillis: Long,
    cause: Throwable? = null,
) : RuntimeException("evaluation exceeded the time budget of ${budgetMillis}ms", cause)

class InputNotMappableException : RuntimeException("input attribute values must be string, number, or boolean")

class InputTooLargeException(
    max: Int,
) : RuntimeException("input exceeds the maximum of $max")
