// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api.dto.response

data class ReplayReportResponse(
    val total: Int,
    val unchanged: Int,
    val changed: Int,
    val noBaseline: Int,
    val diffs: List<ReplayDiffResponse>,
)

data class ReplayDiffResponse(
    val inputHash: String,
    val recorded: OutcomeSummaryResponse?,
    val candidate: OutcomeSummaryResponse,
    val status: String,
)

data class OutcomeSummaryResponse(
    val matched: Boolean,
    val outcomeLabel: String?,
)
