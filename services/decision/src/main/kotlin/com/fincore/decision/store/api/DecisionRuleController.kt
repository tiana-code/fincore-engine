// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api

import com.fincore.decision.store.api.dto.request.CreateRuleRequest
import com.fincore.decision.store.api.dto.response.RuleDetailResponse
import com.fincore.decision.store.api.dto.response.RuleResponse
import com.fincore.decision.store.api.dto.response.VersionResponse
import com.fincore.decision.store.api.mapper.DecisionApiMapper
import com.fincore.decision.store.application.RuleAdminService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@Tag(name = "Decision rules", description = "Create rules, publish immutable rule versions, and read them")
@RestController
@RequestMapping("/v1/decision/rules")
class DecisionRuleController(
    private val ruleAdminService: RuleAdminService,
    private val mapper: DecisionApiMapper,
) {
    @Operation(summary = "Create a decision rule", description = "Creates a named rule with no active version yet.")
    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateRuleRequest,
    ): ResponseEntity<RuleResponse> {
        val response = mapper.toResponse(ruleAdminService.createRule(request.ruleKey))
        return ResponseEntity.created(URI.create("/v1/decision/rules/${response.ruleKey}")).body(response)
    }

    // No Idempotency-Key here by design: versions are append-only and monotonic, so a retried publish creates
    // a new, immutable version rather than corrupting state.
    @Operation(summary = "Publish a rule version", description = "Validates the DSL document and stores it as the new active version.")
    @PostMapping("/{ruleKey}/versions", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun publishVersion(
        @PathVariable ruleKey: String,
        @RequestBody dslJson: String,
    ): ResponseEntity<VersionResponse> {
        val response = mapper.toResponse(ruleAdminService.publishVersion(ruleKey, dslJson))
        return ResponseEntity.created(URI.create("/v1/decision/rules/$ruleKey")).body(response)
    }

    @Operation(summary = "Get a decision rule", description = "Returns the rule and its active version DSL, or 404.")
    @GetMapping("/{ruleKey}")
    fun get(
        @PathVariable ruleKey: String,
    ): RuleDetailResponse = mapper.toDetail(ruleAdminService.getRule(ruleKey))
}
