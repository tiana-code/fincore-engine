// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.aml

import java.math.BigDecimal
import java.time.Instant

/** Generic view of a transaction for AML evaluation. [subjectReference] is an opaque token, never raw PII. */
data class AmlTransactionView(
    val subjectReference: String,
    val amount: BigDecimal,
    val currency: String,
    val occurredAt: Instant,
)
