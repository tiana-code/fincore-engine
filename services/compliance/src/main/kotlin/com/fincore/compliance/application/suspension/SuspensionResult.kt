// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.suspension

/**
 * Outcome of an [AutomatedSuspensionPort] action: a business outcome, distinct from a technical failure (a thrown
 * [AutomatedSuspensionException]).
 *
 * [AlreadySuspended] is a first-class outcome so a replayed signal is unambiguous (the port is idempotent).
 * [Rejected.reason] is a generic, provider-reported business refusal, never a technical error.
 */
sealed interface SuspensionResult {
    data class Suspended(
        val providerReference: String,
    ) : SuspensionResult

    data object AlreadySuspended : SuspensionResult

    data class Rejected(
        val reason: String,
    ) : SuspensionResult
}
