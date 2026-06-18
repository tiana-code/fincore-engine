// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.screening

sealed interface ScreeningDecision {
    data object Approve : ScreeningDecision

    data class Decline(
        val reason: String,
    ) : ScreeningDecision
}
