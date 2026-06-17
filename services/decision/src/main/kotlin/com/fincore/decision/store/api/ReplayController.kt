// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.decision.store.api.dto.request.ReplayRequest
import com.fincore.decision.store.api.dto.response.ReplayReportResponse
import com.fincore.decision.store.api.mapper.DecisionApiMapper
import com.fincore.decision.store.application.ReplayService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Decision replay", description = "Diff a candidate ruleset against recorded decisions for historical inputs")
@RestController
@RequestMapping("/v1/decision/replay")
class ReplayController(
    private val replayService: ReplayService,
    private val mapper: DecisionApiMapper,
    private val objectMapper: ObjectMapper,
) {
    @Operation(
        summary = "Replay a candidate ruleset",
        description = "Evaluates a candidate against each input and diffs the outcome against the recorded decision. Writes nothing.",
    )
    @PostMapping
    fun replay(
        @Valid @RequestBody request: ReplayRequest,
    ): ReplayReportResponse = mapper.toResponse(replayService.replay(objectMapper.writeValueAsString(request.candidate), request.inputs))
}
