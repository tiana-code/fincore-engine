// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.suspension

import com.fincore.compliance.domain.KycSession

/**
 * A request to suspend a subject. [subjectReference] is an opaque token (bounded by the shared
 * KycSession.MAX_SUBJECT_REFERENCE_LENGTH); [reason] is a generic, non-PII reason code or note.
 */
data class SuspensionRequest(
    val subjectReference: String,
    val reason: String,
) {
    init {
        require(subjectReference.isNotBlank() && subjectReference.length <= KycSession.MAX_SUBJECT_REFERENCE_LENGTH) {
            "subjectReference must be non-blank and at most ${KycSession.MAX_SUBJECT_REFERENCE_LENGTH} characters"
        }
        require(reason.isNotBlank() && reason.length <= MAX_REASON_LENGTH) {
            "reason must be non-blank and at most $MAX_REASON_LENGTH characters"
        }
    }

    companion object {
        const val MAX_REASON_LENGTH = 280
    }
}
