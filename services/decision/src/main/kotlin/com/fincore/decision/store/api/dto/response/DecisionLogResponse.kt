// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api.dto.response

import java.time.Instant

data class DecisionLogResponse(
    val id: String,
    val evaluatedAt: Instant,
    val ruleVersionId: String,
    val inputHash: String,
    val matched: Boolean,
    val outcomeLabel: String?,
)
