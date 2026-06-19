// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.messaging

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.Instant

// Compliance's own view of the ledger transaction-posted wire payload; the JSON contract is the boundary, not a
// shared class (services do not depend on each other). Unknown fields are ignored for forward compatibility.
@JsonIgnoreProperties(ignoreUnknown = true)
data class LedgerEntryLine(
    val accountId: String,
    val direction: String,
    val amount: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LedgerTransactionPosted(
    val transactionId: String,
    val reference: String,
    val currency: String,
    val postedAt: Instant,
    val entries: List<LedgerEntryLine>,
)
