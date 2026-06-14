// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.dto.response

import com.fincore.ledger.domain.enum.TransactionStatus
import java.time.Instant

data class TransactionResponse(
    val id: String,
    val reference: String,
    val status: TransactionStatus,
    val postedAt: Instant,
)
