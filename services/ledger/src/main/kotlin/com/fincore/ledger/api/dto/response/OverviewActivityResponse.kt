// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.dto.response

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fincore.ledger.api.serialization.MoneyAmountSerializer
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant

data class OverviewActivityResponse(
    val type: String,
    val resourceId: String,
    val label: String,
    @JsonSerialize(using = MoneyAmountSerializer::class)
    @Schema(type = "string", format = "decimal", example = "100.00", nullable = true)
    val amount: BigDecimal?,
    val currency: String?,
    val occurredAt: Instant,
)
