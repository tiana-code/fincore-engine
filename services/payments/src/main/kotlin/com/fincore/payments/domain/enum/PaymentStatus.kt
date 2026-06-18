// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.domain.enum

enum class PaymentStatus {
    INITIATED,
    SCREENING,
    SUBMITTED,
    SETTLED,
    FAILED,
    CANCELLED,
    ;

    fun isTerminal(): Boolean = this == SETTLED || this == FAILED || this == CANCELLED

    fun canTransitionTo(target: PaymentStatus): Boolean =
        when (this) {
            INITIATED -> target == SCREENING || target == CANCELLED
            SCREENING -> target == SUBMITTED || target == FAILED || target == CANCELLED
            SUBMITTED -> target == SETTLED || target == FAILED
            SETTLED, FAILED, CANCELLED -> false
        }
}
