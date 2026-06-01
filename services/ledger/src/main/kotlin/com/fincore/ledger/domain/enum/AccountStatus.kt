// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.domain.enum

enum class AccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED,
    ;

    fun canTransitionTo(target: AccountStatus): Boolean =
        when (this) {
            ACTIVE -> target == FROZEN || target == CLOSED
            FROZEN -> target == ACTIVE || target == CLOSED
            CLOSED -> false
        }
}
