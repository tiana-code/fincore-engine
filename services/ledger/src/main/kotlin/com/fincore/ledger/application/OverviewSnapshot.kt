// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

data class OverviewSnapshot(
    val activity: List<ActivityItem>,
    val transactionsLast24h: List<Int>,
)
