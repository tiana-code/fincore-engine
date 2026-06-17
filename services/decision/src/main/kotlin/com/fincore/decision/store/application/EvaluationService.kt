// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import com.fasterxml.jackson.databind.JsonNode
import com.fincore.decision.domain.DecisionResult
import java.util.UUID

interface EvaluationService {
    fun evaluate(
        ruleKey: String,
        attributes: Map<String, JsonNode>,
    ): EvaluationOutcome
}

data class EvaluationOutcome(
    val result: DecisionResult,
    val decisionLogId: UUID,
)
