// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.decision.store.api.dto.response.ActiveVersionResponse
import com.fincore.decision.store.api.dto.response.RuleDetailResponse
import com.fincore.decision.store.api.dto.response.RuleResponse
import com.fincore.decision.store.api.dto.response.VersionResponse
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
}
