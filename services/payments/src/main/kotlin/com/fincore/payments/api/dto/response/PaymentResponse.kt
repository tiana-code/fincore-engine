// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.api.dto.response

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fincore.payments.api.serialization.MoneyAmountSerializer
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

data class PaymentResponse(
    val id: String,
    val reference: String,
    @JsonSerialize(using = MoneyAmountSerializer::class)
    @Schema(type = "string", format = "decimal", example = "125.50")
    val amount: BigDecimal,
    val currency: String,
    val status: String,
)
