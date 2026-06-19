// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.domain.enum

enum class KycStatus {
    INITIATED,
    SCREENING,
    APPROVED,
    REJECTED,
    ;

    fun isTerminal(): Boolean = this == APPROVED || this == REJECTED

    fun canTransitionTo(target: KycStatus): Boolean =
        when (this) {
            INITIATED -> target == SCREENING
            SCREENING -> target == APPROVED || target == REJECTED
            APPROVED, REJECTED -> false
        }
}
