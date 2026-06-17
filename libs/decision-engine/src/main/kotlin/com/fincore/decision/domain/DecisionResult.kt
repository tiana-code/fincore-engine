// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.domain

enum class TraceDetail { EVALUATED, ATTRIBUTE_ABSENT }

data class ConditionTrace(
    val description: String,
    val result: Boolean,
    val detail: TraceDetail = TraceDetail.EVALUATED,
    val children: List<ConditionTrace> = emptyList(),
)

data class DecisionResult(
    val matched: Boolean,
    val outcome: DecisionOutcome?,
    val trace: List<ConditionTrace>,
)
