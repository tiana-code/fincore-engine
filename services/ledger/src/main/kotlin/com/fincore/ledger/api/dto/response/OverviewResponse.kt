// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.dto.response

data class OverviewResponse(
    val activity: List<OverviewActivityResponse>,
    val transactionsLast24h: List<Int>,
)
