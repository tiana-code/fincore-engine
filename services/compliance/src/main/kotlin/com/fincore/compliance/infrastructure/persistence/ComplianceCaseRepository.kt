// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.persistence

import com.fincore.compliance.domain.enum.CaseStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ComplianceCaseRepository : JpaRepository<ComplianceCaseEntity, UUID> {
    fun findByStatus(status: CaseStatus): List<ComplianceCaseEntity>
}
