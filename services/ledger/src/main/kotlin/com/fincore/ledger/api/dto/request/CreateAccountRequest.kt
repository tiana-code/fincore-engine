// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.dto.request

import com.fincore.ledger.domain.enum.AccountType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateAccountRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 255)
    val name: String,
    @field:NotNull
    val type: AccountType,
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Z]{3}$")
    val currency: String,
)
