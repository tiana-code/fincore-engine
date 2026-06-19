// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.copilot

import com.fincore.compliance.domain.KycSession

/**
 * Context for an [AmlCopilot] assist call. [caseReference] is an opaque token (bounded by the shared
 * KycSession.MAX_SUBJECT_REFERENCE_LENGTH). [context] is a list of generic notes or codes, never raw PII or business
 * rules; the adapter resolves any further detail out of tree.
 */
data class CopilotRequest(
    val caseReference: String,
    val context: List<String> = emptyList(),
) {
    init {
        require(caseReference.isNotBlank() && caseReference.length <= KycSession.MAX_SUBJECT_REFERENCE_LENGTH) {
            "caseReference must be non-blank and at most ${KycSession.MAX_SUBJECT_REFERENCE_LENGTH} characters"
        }
    }
}
