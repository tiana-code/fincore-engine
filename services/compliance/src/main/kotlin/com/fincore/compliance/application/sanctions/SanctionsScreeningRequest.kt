// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.sanctions

import com.fincore.compliance.domain.KycSession

/**
 * A request to screen a subject with a configurable m-of-n partial match.
 *
 * [subjectReference] is an opaque token identifying the subject, never raw PII; the provider resolves the actual
 * subject data out of tree by this reference (same model as the KYC port). [attributes] is the set of generic
 * attribute-key dimensions to screen ("n"), never raw PII values. [requiredMatches] is the minimum dimensions that
 * must match for a potential hit ("m"), where 1 <= m <= n.
 */
data class SanctionsScreeningRequest(
    val subjectReference: String,
    val attributes: Set<String>,
    val requiredMatches: Int,
) {
    init {
        require(subjectReference.isNotBlank() && subjectReference.length <= KycSession.MAX_SUBJECT_REFERENCE_LENGTH) {
            "subjectReference must be non-blank and at most ${KycSession.MAX_SUBJECT_REFERENCE_LENGTH} characters"
        }
        require(attributes.isNotEmpty()) { "attributes must not be empty" }
        require(requiredMatches in 1..attributes.size) {
            "requiredMatches must be between 1 and the number of attributes"
        }
    }
}
