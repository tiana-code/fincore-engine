// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.api

import com.fincore.compliance.api.dto.request.CreateKycSessionRequest
import com.fincore.compliance.api.dto.response.KycSessionResponse
import com.fincore.compliance.api.mapper.KycApiMapper
import com.fincore.compliance.application.kyc.KycService
import com.fincore.core.KycSessionId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@Tag(name = "Compliance", description = "Start and read KYC sessions")
@RestController
@RequestMapping("/v1/kyc/sessions")
class KycController(
    private val kycService: KycService,
    private val mapper: KycApiMapper,
) {
    @Operation(summary = "Start a KYC session", description = "Creates a session in the INITIATED state.")
    @PostMapping
    fun initiate(
        @Valid @RequestBody request: CreateKycSessionRequest,
    ): ResponseEntity<KycSessionResponse> {
        val response = mapper.toResponse(kycService.initiate(mapper.toCommand(request)))
        return ResponseEntity.created(URI.create("/v1/kyc/sessions/${response.id}")).body(response)
    }

    @Operation(summary = "Get a KYC session", description = "Returns the session or 404.")
    @GetMapping("/{id}")
    fun get(
        @PathVariable id: String,
    ): KycSessionResponse = mapper.toResponse(kycService.get(KycSessionId.fromString(id)))
}
