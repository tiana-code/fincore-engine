// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.decision.store.api.dto.response.ActiveVersionResponse
import com.fincore.decision.store.api.dto.response.DecisionLogResponse
import com.fincore.decision.store.api.dto.response.DecisionResponse
import com.fincore.decision.store.api.dto.response.OutcomeResponse
import com.fincore.decision.store.api.dto.response.OutcomeSummaryResponse
import com.fincore.decision.store.api.dto.response.ReplayDiffResponse
import com.fincore.decision.store.api.dto.response.ReplayReportResponse
import com.fincore.decision.store.api.dto.response.RuleDetailResponse
import com.fincore.decision.store.api.dto.response.RuleResponse
import com.fincore.decision.store.api.dto.response.VersionResponse
import com.fincore.decision.store.application.DecisionLogView
import com.fincore.decision.store.application.EvaluationOutcome
import com.fincore.decision.store.application.ReplayDiff
import com.fincore.decision.store.application.ReplayReport
import com.fincore.decision.store.application.RuleDetailView
import com.fincore.decision.store.application.RuleView
import com.fincore.decision.store.application.VersionView
import org.springframework.stereotype.Component

// Hand-written, not MapStruct: the conversions are UUID/document-string to String/JsonNode plus a nullable
// active-version assembly, the same MapStruct-unfriendly shape the ledger LedgerApiMapper documented; a hand
// mapper avoids pulling kapt into this module for a trivial mapping.
@Component
class DecisionApiMapper(
    private val objectMapper: ObjectMapper,
) {
    fun toResponse(view: RuleView): RuleResponse = RuleResponse(view.id.toString(), view.ruleKey)

    fun toResponse(view: VersionView): VersionResponse = VersionResponse(view.id.toString(), view.versionNo)

    fun toDetail(view: RuleDetailView): RuleDetailResponse =
        RuleDetailResponse(
            id = view.id.toString(),
            ruleKey = view.ruleKey,
            activeVersion =
                view.activeVersion?.let {
                    ActiveVersionResponse(it.versionNo, objectMapper.readTree(it.dsl))
                },
        )

    fun toResponse(outcome: EvaluationOutcome): DecisionResponse =
        DecisionResponse(
            matched = outcome.result.matched,
            outcome = outcome.result.outcome?.let { OutcomeResponse(it.label, it.reasonCodes) },
            trace = objectMapper.valueToTree(outcome.result.trace),
            decisionLogId = outcome.decisionLogId.toString(),
        )

    fun toLogResponse(view: DecisionLogView): DecisionLogResponse =
        DecisionLogResponse(
            id = view.id.toString(),
            evaluatedAt = view.evaluatedAt,
            ruleVersionId = view.ruleVersionId.toString(),
            inputHash = view.inputHash,
            matched = view.matched,
            outcomeLabel = view.outcomeLabel,
        )

    fun toResponse(report: ReplayReport): ReplayReportResponse =
        ReplayReportResponse(
            total = report.total,
            unchanged = report.unchanged,
            changed = report.changed,
            noBaseline = report.noBaseline,
            diffs = report.diffs.map(::toDiffResponse),
        )

    private fun toDiffResponse(diff: ReplayDiff): ReplayDiffResponse =
        ReplayDiffResponse(
            inputHash = diff.inputHash,
            recorded = diff.recordedMatched?.let { OutcomeSummaryResponse(it, diff.recordedLabel) },
            candidate = OutcomeSummaryResponse(diff.candidateMatched, diff.candidateLabel),
            status = diff.status.name,
        )
}
