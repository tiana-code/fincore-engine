// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.ledger.domain.enum.ActivityType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ActivityItem(
    val type: ActivityType,
    val resourceId: UUID,
    val label: String,
    val amount: BigDecimal?,
    val currency: String?,
    val occurredAt: Instant,
)
