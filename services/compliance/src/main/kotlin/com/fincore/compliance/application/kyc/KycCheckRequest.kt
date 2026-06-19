// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

import com.fincore.compliance.domain.KycSession

/**
 * A request to check a subject. [subjectReference] is an opaque token identifying the subject under check, never raw
 * PII; the provider resolves the actual subject data out of tree. Its bound is the KycSession domain invariant.
 */
data class KycCheckRequest(
    val subjectReference: String,
) {
    init {
        require(subjectReference.isNotBlank() && subjectReference.length <= KycSession.MAX_SUBJECT_REFERENCE_LENGTH) {
            "subjectReference must be non-blank and at most ${KycSession.MAX_SUBJECT_REFERENCE_LENGTH} characters"
        }
    }
}
