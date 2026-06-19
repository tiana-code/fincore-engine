// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.case

import com.fincore.compliance.domain.ComplianceCase
import com.fincore.compliance.domain.enum.CaseStatus
import com.fincore.core.ComplianceCaseId

interface ComplianceCaseService {
    fun open(command: OpenCaseCommand): ComplianceCase

    fun get(id: ComplianceCaseId): ComplianceCase

    fun claim(id: ComplianceCaseId): ComplianceCase

    fun resolve(id: ComplianceCaseId): ComplianceCase

    fun escalate(id: ComplianceCaseId): ComplianceCase

    fun list(status: CaseStatus): List<ComplianceCase>
}
