// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import java.math.BigDecimal
import java.time.Instant

/**
 * Spring Data projection for the recent-activity native query.
 * Field names match the lowercased SQL aliases emitted by PostgreSQL.
 */
interface TransactionActivityRow {
    val id: String
    val label: String
    val reversal: Boolean
    val postedat: Instant
    val amount: BigDecimal
    val currency: String?
}
