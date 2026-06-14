// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.idempotency

object IdempotencyAttributes {
    const val HEADER = "Idempotency-Key"
    const val KEY = "fincore.idempotency.key"
    const val BODY = "fincore.idempotency.body"
}
