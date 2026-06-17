// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import java.time.Instant
import java.util.UUID

interface DecisionLogService {
    fun byInputHash(inputHash: String): List<DecisionLogView>

    fun byRuleVersionId(ruleVersionId: UUID): List<DecisionLogView>
}

data class DecisionLogView(
    val id: UUID,
    val evaluatedAt: Instant,
    val ruleVersionId: UUID,
    val inputHash: String,
    val matched: Boolean,
    val outcomeLabel: String?,
)
