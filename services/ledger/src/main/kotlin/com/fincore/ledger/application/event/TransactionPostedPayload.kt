// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application.event

import java.time.Instant

data class EntryLinePayload(
    val accountId: String,
    val direction: String,
    val amount: String,
)

data class TransactionPostedPayload(
    val transactionId: String,
    val reference: String,
    val currency: String,
    val postedAt: Instant,
    val entries: List<EntryLinePayload>,
)
