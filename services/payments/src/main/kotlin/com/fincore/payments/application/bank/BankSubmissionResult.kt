// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.bank

/**
 * Outcome of submitting a payment to a [BankProvider]. A business decision (accept/reject), distinct from a
 * technical failure (which is a thrown [BankProviderException]).
 *
 * [Accepted] means accepted-for-processing at submission, NOT final settlement; the final state arrives
 * asynchronously (provider webhook).
 */
sealed interface BankSubmissionResult {
    data class Accepted(
        val providerReference: String,
    ) : BankSubmissionResult

    data class Rejected(
        val reason: String,
    ) : BankSubmissionResult
}
