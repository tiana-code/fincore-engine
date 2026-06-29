// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import java.time.Instant

/**
 * Spring Data projection for the hourly sparkline native query.
 * Field names match the lowercased SQL aliases emitted by PostgreSQL.
 */
interface HourlyTransactionCountRow {
    val bucket: Instant
    val cnt: Long
}
