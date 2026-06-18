// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.api.dto.request

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class InitiatePaymentRequest(
    @field:DecimalMin(value = "0.0", inclusive = false, message = "amount must be positive")
    val amount: BigDecimal,
    @field:Pattern(regexp = "^[A-Z]{3}$", message = "currency must be an ISO 4217 code")
    val currency: String,
    @field:NotBlank
    @field:Size(max = 140)
    val reference: String,
)
