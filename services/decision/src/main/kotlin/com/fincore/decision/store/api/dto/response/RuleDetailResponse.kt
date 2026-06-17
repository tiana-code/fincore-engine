// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api.dto.response

import com.fasterxml.jackson.databind.JsonNode

data class RuleDetailResponse(
    val id: String,
    val ruleKey: String,
    val activeVersion: ActiveVersionResponse?,
)

data class ActiveVersionResponse(
    val versionNo: Int,
    val dsl: JsonNode,
)
