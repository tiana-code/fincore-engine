// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import com.fincore.decision.store.config.DecisionApiProperties
import com.fincore.decision.store.persistence.DecisionLogEntity
import com.fincore.decision.store.persistence.DecisionLogRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DecisionLogServiceImpl(
    private val decisionLogRepository: DecisionLogRepository,
    private val properties: DecisionApiProperties,
) : DecisionLogService {
    @Transactional(readOnly = true)
    override fun byInputHash(inputHash: String): List<DecisionLogView> =
        decisionLogRepository.findByInputHash(inputHash, newestFirst()).map(::toView)

    @Transactional(readOnly = true)
    override fun byRuleVersionId(ruleVersionId: UUID): List<DecisionLogView> =
        decisionLogRepository.findByRuleVersionId(ruleVersionId, newestFirst()).map(::toView)

    private fun newestFirst(): PageRequest = PageRequest.of(0, properties.maxLogPageSize, Sort.by(Sort.Direction.DESC, "evaluatedAt"))

    private fun toView(entity: DecisionLogEntity): DecisionLogView =
        DecisionLogView(
            id = entity.id,
            evaluatedAt = entity.evaluatedAt,
            ruleVersionId = entity.ruleVersionId,
            inputHash = entity.inputHash,
            matched = entity.matched,
            outcomeLabel = entity.outcomeLabel,
        )
}
