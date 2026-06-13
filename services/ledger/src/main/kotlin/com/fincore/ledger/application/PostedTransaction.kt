// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.TransactionId
import java.time.Instant

data class PostedTransaction(
    val id: TransactionId,
    val reference: String,
    val postedAt: Instant,
)
