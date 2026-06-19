// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.persistence

import com.fincore.compliance.domain.ComplianceCase
import com.fincore.core.ComplianceCaseId
import org.springframework.stereotype.Component
import java.time.Instant

// Hand-written because MapStruct cannot construct the value-class domain aggregate; the domain stays pure.
@Component
class ComplianceCasePersistenceAdapter {
    fun toDomain(entity: ComplianceCaseEntity): ComplianceCase =
        ComplianceCase(
            id = ComplianceCaseId(entity.id),
            reference = entity.reference,
            status = entity.status,
        )

    fun toNewEntity(
        case: ComplianceCase,
        now: Instant,
    ): ComplianceCaseEntity =
        ComplianceCaseEntity(
            id = case.id.value,
            reference = case.reference,
            status = case.status,
            createdAt = now,
            version = 0,
        )
}
