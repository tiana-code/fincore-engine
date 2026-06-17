// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api.dto.request

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class ReplayRequest(
    @field:NotNull
    @JsonSetter(nulls = Nulls.FAIL)
    val candidate: JsonNode,
    @field:NotEmpty
    @JsonSetter(contentNulls = Nulls.FAIL)
    val inputs: List<Map<String, JsonNode>>,
)
