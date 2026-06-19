// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.domain

import com.fincore.compliance.domain.enum.CaseStatus
import com.fincore.core.ComplianceCaseId

class ComplianceCase(
    val id: ComplianceCaseId,
    reference: String,
    status: CaseStatus = CaseStatus.OPEN,
) {
    val reference: String = validatedReference(reference)

    var status: CaseStatus = status
        private set

    fun transitionTo(target: CaseStatus) {
        if (!status.canTransitionTo(target)) {
            throw ComplianceDomainException("Illegal case status transition: $status -> $target for case $id")
        }
        status = target
    }

    fun isTerminal(): Boolean = status.isTerminal()

    private fun validatedReference(value: String): String {
        if (value.isBlank() || value.length > MAX_REFERENCE_LENGTH) {
            throw ComplianceDomainException(
                "Case reference must be non-blank and at most $MAX_REFERENCE_LENGTH characters",
            )
        }
        return value
    }

    private companion object {
        const val MAX_REFERENCE_LENGTH = 140
    }
}
