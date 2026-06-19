// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.api.mapper

import com.fincore.compliance.api.dto.request.OpenCaseRequest
import com.fincore.compliance.api.dto.response.CaseResponse
import com.fincore.compliance.application.case.OpenCaseCommand
import com.fincore.compliance.domain.ComplianceCase
import org.springframework.stereotype.Component

// Hand-written: the value-class ComplianceCaseId crosses the wire as a prefixed-ULID string.
@Component
class CaseApiMapper {
    fun toCommand(request: OpenCaseRequest): OpenCaseCommand = OpenCaseCommand(reference = request.reference)

    fun toResponse(case: ComplianceCase): CaseResponse =
        CaseResponse(
            id = case.id.toString(),
            reference = case.reference,
            status = case.status.name,
        )
}
