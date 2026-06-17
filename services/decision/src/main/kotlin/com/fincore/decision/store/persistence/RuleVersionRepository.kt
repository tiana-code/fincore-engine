// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface RuleVersionRepository : JpaRepository<RuleVersionEntity, UUID> {
    fun findByRuleId(ruleId: UUID): List<RuleVersionEntity>

    @Query("select max(v.versionNo) from RuleVersionEntity v where v.ruleId = :ruleId")
    fun findMaxVersionNo(
        @Param("ruleId") ruleId: UUID,
    ): Int?
}
