// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AmlRuleRepository : JpaRepository<AmlRuleEntity, UUID> {
    fun findByRuleKey(ruleKey: String): AmlRuleEntity?
}
