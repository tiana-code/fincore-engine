// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DecisionLogRepository : JpaRepository<DecisionLogEntity, UUID> {
    fun findByRuleVersionId(ruleVersionId: UUID): List<DecisionLogEntity>

    fun findByInputHash(inputHash: String): List<DecisionLogEntity>
}
