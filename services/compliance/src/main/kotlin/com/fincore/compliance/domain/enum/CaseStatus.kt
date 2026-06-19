// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.domain.enum

enum class CaseStatus {
    OPEN,
    CLAIMED,
    ESCALATED,
    RESOLVED,
    ;

    fun isTerminal(): Boolean = this == RESOLVED

    fun canTransitionTo(target: CaseStatus): Boolean =
        when (this) {
            OPEN -> target == CLAIMED
            CLAIMED -> target == RESOLVED || target == ESCALATED
            ESCALATED -> target == RESOLVED
            RESOLVED -> false
        }
}
