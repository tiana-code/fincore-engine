// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api

import com.fincore.decision.store.api.dto.response.DecisionLogResponse
import com.fincore.decision.store.api.mapper.DecisionApiMapper
import com.fincore.decision.store.application.DecisionLogService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Decision logs", description = "Read the append-only decision audit log")
@RestController
@RequestMapping("/v1/decision/logs")
class DecisionLogController(
    private val decisionLogService: DecisionLogService,
    private val mapper: DecisionApiMapper,
) {
    @Operation(summary = "List decision logs", description = "Returns audit logs filtered by exactly one of inputHash or ruleVersionId.")
    @GetMapping
    fun list(
        @RequestParam(required = false) inputHash: String?,
        @RequestParam(required = false) ruleVersionId: String?,
    ): List<DecisionLogResponse> {
        val logs =
            when {
                inputHash != null && ruleVersionId == null -> decisionLogService.byInputHash(inputHash)
                ruleVersionId != null && inputHash == null -> decisionLogService.byRuleVersionId(parseVersionId(ruleVersionId))
                else -> throw IllegalArgumentException("provide exactly one of inputHash or ruleVersionId")
            }
        return logs.map(mapper::toLogResponse)
    }

    private fun parseVersionId(value: String): UUID =
        runCatching { UUID.fromString(value) }.getOrElse { throw IllegalArgumentException("ruleVersionId must be a valid UUID") }
}
