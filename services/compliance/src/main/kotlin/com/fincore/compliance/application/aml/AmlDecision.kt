// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.aml

sealed interface AmlDecision {
    data object Clear : AmlDecision

    data class Flagged(
        val reasonCodes: List<String>,
    ) : AmlDecision
}
