// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.decision.domain.DecisionResult
import com.fincore.decision.store.persistence.DecisionLogEntity
import com.fincore.decision.store.persistence.DecisionLogRepository
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/**
 * Builds and persists the immutable decision_logs audit row for one completed evaluation. A non-matching
 * result has no outcome, so it stores null label and reason codes (the DB CHECK enforces that totality).
 */
@Component
class DecisionLogWriter(
    private val decisionLogRepository: DecisionLogRepository,
    private val objectMapper: ObjectMapper,
) {
    fun write(
        ruleVersionId: UUID,
        inputHash: String,
        result: DecisionResult,
    ): UUID {
        val entity =
            DecisionLogEntity(
                id = UUID.randomUUID(),
                evaluatedAt = Instant.now(),
                ruleVersionId = ruleVersionId,
                inputHash = inputHash,
                matched = result.matched,
                outcomeLabel = result.outcome?.label,
                reasonCodes = result.outcome?.let { objectMapper.writeValueAsString(it.reasonCodes) },
                trace = objectMapper.writeValueAsString(result.trace),
            )
        return decisionLogRepository.save(entity).id
    }
}
