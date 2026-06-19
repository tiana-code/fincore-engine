// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.api

import com.fincore.compliance.api.dto.request.OpenCaseRequest
import com.fincore.compliance.api.dto.response.CaseResponse
import com.fincore.compliance.api.mapper.CaseApiMapper
import com.fincore.compliance.application.case.ComplianceCaseService
import com.fincore.compliance.domain.enum.CaseStatus
import com.fincore.core.ComplianceCaseId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@Tag(name = "Compliance", description = "Open, read and transition compliance cases")
@RestController
@RequestMapping("/v1/compliance/cases")
class CaseController(
    private val caseService: ComplianceCaseService,
    private val mapper: CaseApiMapper,
) {
    @Operation(summary = "Open a compliance case", description = "Creates a case in the OPEN state.")
    @PostMapping
    fun open(
        @Valid @RequestBody request: OpenCaseRequest,
    ): ResponseEntity<CaseResponse> {
        val response = mapper.toResponse(caseService.open(mapper.toCommand(request)))
        return ResponseEntity.created(URI.create("/v1/compliance/cases/${response.id}")).body(response)
    }

    @Operation(summary = "List cases by status")
    @GetMapping
    fun list(
        @RequestParam status: CaseStatus,
    ): List<CaseResponse> = caseService.list(status).map(mapper::toResponse)

    @Operation(summary = "Get a case", description = "Returns the case or 404.")
    @GetMapping("/{id}")
    fun get(
        @PathVariable id: String,
    ): CaseResponse = mapper.toResponse(caseService.get(ComplianceCaseId.fromString(id)))

    @Operation(summary = "Claim a case", description = "OPEN to CLAIMED, or 409 if illegal.")
    @PostMapping("/{id}/claim")
    fun claim(
        @PathVariable id: String,
    ): CaseResponse = mapper.toResponse(caseService.claim(ComplianceCaseId.fromString(id)))

    @Operation(summary = "Resolve a case", description = "Marks the case RESOLVED, or 409 if illegal.")
    @PostMapping("/{id}/resolve")
    fun resolve(
        @PathVariable id: String,
    ): CaseResponse = mapper.toResponse(caseService.resolve(ComplianceCaseId.fromString(id)))

    @Operation(summary = "Escalate a case", description = "Marks the case ESCALATED, or 409 if illegal.")
    @PostMapping("/{id}/escalate")
    fun escalate(
        @PathVariable id: String,
    ): CaseResponse = mapper.toResponse(caseService.escalate(ComplianceCaseId.fromString(id)))
}
