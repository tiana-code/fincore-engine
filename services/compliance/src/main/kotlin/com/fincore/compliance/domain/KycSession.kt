// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.domain

import com.fincore.compliance.domain.enum.KycStatus
import com.fincore.core.KycSessionId

class KycSession(
    val id: KycSessionId,
    subjectReference: String,
    status: KycStatus = KycStatus.INITIATED,
) {
    val subjectReference: String = validatedSubjectReference(subjectReference)

    var status: KycStatus = status
        private set

    fun transitionTo(target: KycStatus) {
        if (!status.canTransitionTo(target)) {
            throw ComplianceDomainException("Illegal KYC status transition: $status -> $target for session $id")
        }
        status = target
    }

    fun isTerminal(): Boolean = status.isTerminal()

    private fun validatedSubjectReference(value: String): String {
        if (value.isBlank() || value.length > MAX_SUBJECT_REFERENCE_LENGTH) {
            throw ComplianceDomainException(
                "KYC subjectReference must be non-blank and at most $MAX_SUBJECT_REFERENCE_LENGTH characters",
            )
        }
        return value
    }

    companion object {
        const val MAX_SUBJECT_REFERENCE_LENGTH = 140
    }
}
