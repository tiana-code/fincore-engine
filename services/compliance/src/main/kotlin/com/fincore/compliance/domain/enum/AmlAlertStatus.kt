// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.domain.enum

enum class AmlAlertStatus {
    OPEN,
    RESOLVED,
    DISMISSED,
    ;

    fun isTerminal(): Boolean = this == RESOLVED || this == DISMISSED

    fun canTransitionTo(target: AmlAlertStatus): Boolean =
        when (this) {
            OPEN -> target == RESOLVED || target == DISMISSED
            RESOLVED, DISMISSED -> false
        }
}
