// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.domain.enum

enum class AuditAction {
    ACCOUNT_CREATE,
    ACCOUNT_RENAME,
    ACCOUNT_STATUS_CHANGE,
    TRANSACTION_POST,
    TRANSACTION_REVERSE,
}
