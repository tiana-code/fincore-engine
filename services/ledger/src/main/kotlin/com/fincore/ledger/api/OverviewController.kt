// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fincore.ledger.api.dto.response.OverviewResponse
import com.fincore.ledger.api.mapper.LedgerApiMapper
import com.fincore.ledger.application.OverviewService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Overview", description = "Recent ledger activity and 24h transaction sparkline")
@RestController
@RequestMapping("/v1/overview")
class OverviewController(
    private val overviewService: OverviewService,
    private val mapper: LedgerApiMapper,
) {
    @Operation(summary = "Ledger overview", description = "Returns recent ledger activity and a 24-slot hourly transaction sparkline.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Overview snapshot"),
        ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
        ApiResponse(responseCode = "403", description = "Insufficient scope"),
    )
    @GetMapping
    fun overview(): OverviewResponse = mapper.toResponse(overviewService.overview())
}
