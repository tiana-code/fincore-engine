// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.TransactionId

interface TransactionService {
    fun post(command: PostTransactionCommand): PostedTransaction

    fun get(id: TransactionId): TransactionDetail

    fun reverse(
        id: TransactionId,
        actor: String,
        correlationId: String?,
    ): PostedTransaction

    fun list(
        page: Int,
        size: Int,
    ): TransactionPage
}
