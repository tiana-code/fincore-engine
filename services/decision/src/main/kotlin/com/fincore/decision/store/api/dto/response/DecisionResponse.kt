// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api.dto.response

import com.fasterxml.jackson.databind.JsonNode

data class DecisionResponse(
    val matched: Boolean,
    val outcome: OutcomeResponse?,
    val trace: JsonNode,
    val decisionLogId: String,
)

data class OutcomeResponse(
    val label: String,
    val reasonCodes: List<String>,
)
