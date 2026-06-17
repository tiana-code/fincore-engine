// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api

import com.fasterxml.jackson.databind.JsonNode
import com.fincore.decision.store.api.dto.response.DecisionResponse
import com.fincore.decision.store.api.mapper.DecisionApiMapper
import com.fincore.decision.store.application.EvaluationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Decision evaluation", description = "Evaluate an input against a rule's active version")
@RestController
@RequestMapping("/v1/decision/rules")
class DecisionEvaluationController(
    private val evaluationService: EvaluationService,
    private val mapper: DecisionApiMapper,
) {
    @Operation(
        summary = "Evaluate a rule",
        description = "Evaluates the input attributes against the rule's active version, writes an audit log, and returns the decision.",
    )
    @PostMapping("/{ruleKey}/evaluate", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun evaluate(
        @PathVariable ruleKey: String,
        @RequestBody attributes: Map<String, JsonNode>,
    ): DecisionResponse = mapper.toResponse(evaluationService.evaluate(ruleKey, attributes))
}
