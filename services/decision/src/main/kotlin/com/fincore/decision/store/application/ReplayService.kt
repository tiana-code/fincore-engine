// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import com.fasterxml.jackson.databind.JsonNode

interface ReplayService {
    fun replay(
        candidateDsl: String,
        inputs: List<Map<String, JsonNode>>,
    ): ReplayReport
}

enum class DiffStatus { UNCHANGED, CHANGED, NO_BASELINE }

data class ReplayReport(
    val total: Int,
    val unchanged: Int,
    val changed: Int,
    val noBaseline: Int,
    val diffs: List<ReplayDiff>,
)

data class ReplayDiff(
    val inputHash: String,
    val recordedMatched: Boolean?,
    val recordedLabel: String?,
    val candidateMatched: Boolean,
    val candidateLabel: String?,
    val status: DiffStatus,
)
