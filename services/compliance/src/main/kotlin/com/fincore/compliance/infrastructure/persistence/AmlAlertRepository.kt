// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.persistence

import com.fincore.compliance.domain.enum.AmlAlertStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AmlAlertRepository : JpaRepository<AmlAlertEntity, UUID> {
    fun findByStatus(status: AmlAlertStatus): List<AmlAlertEntity>
}
