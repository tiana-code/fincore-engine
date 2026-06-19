// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

/**
 * Outcome of a [KycProvider] check: a business decision, distinct from a technical failure (a thrown
 * [KycProviderException]).
 *
 * [reason] and [missing] carry generic codes or field identifiers, never PII or raw subject values.
 * [Approved] and [Pending] are accepted at submission; a final state may arrive asynchronously.
 * [InsufficientData] is a first-class outcome: the check could not be decided for want of the listed attributes.
 */
sealed interface KycCheckResult {
    data class Approved(
        val providerReference: String,
    ) : KycCheckResult

    data class Rejected(
        val reason: String,
    ) : KycCheckResult

    data class Pending(
        val providerReference: String,
    ) : KycCheckResult

    data class InsufficientData(
        val missing: List<String>,
    ) : KycCheckResult {
        init {
            require(missing.isNotEmpty()) { "missing must not be empty" }
        }
    }
}
