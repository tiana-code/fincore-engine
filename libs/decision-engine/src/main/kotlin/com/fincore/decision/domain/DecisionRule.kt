// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.domain

data class DecisionOutcome(
    val label: String,
    val reasonCodes: List<String> = emptyList(),
)

data class DecisionRule(
    val condition: Condition,
    val outcome: DecisionOutcome,
)
