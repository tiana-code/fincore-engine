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
