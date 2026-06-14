// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.TransactionId
import com.fincore.ledger.domain.enum.TransactionStatus
import java.time.Instant

data class TransactionSummary(
    val id: TransactionId,
    val reference: String,
    val status: TransactionStatus,
    val postedAt: Instant,
)
