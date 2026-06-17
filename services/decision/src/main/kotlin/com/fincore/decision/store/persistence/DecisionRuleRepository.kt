// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DecisionRuleRepository : JpaRepository<DecisionRuleEntity, UUID> {
    fun findByRuleKey(ruleKey: String): DecisionRuleEntity?
}
