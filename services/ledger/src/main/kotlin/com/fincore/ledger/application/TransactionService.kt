// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

interface TransactionService {
    fun post(command: PostTransactionCommand): PostedTransaction

    fun list(
        page: Int,
        size: Int,
    ): TransactionPage
}
