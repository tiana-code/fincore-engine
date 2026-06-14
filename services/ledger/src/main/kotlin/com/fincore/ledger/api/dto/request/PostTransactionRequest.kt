// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.dto.request

import com.fincore.ledger.domain.enum.EntryDirection
import jakarta.validation.Valid
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class PostTransactionRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val reference: String,
    @field:Size(max = 2048)
    val description: String?,
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Z]{3}$")
    val currency: String,
    @field:Size(min = 2, max = 1000)
    @field:Valid
    val entries: List<EntryLineRequest>,
)

data class EntryLineRequest(
    @field:NotBlank
    val accountId: String,
    @field:NotNull
    val direction: EntryDirection,
    @field:NotNull
    @field:Digits(integer = 20, fraction = 18)
    val amount: BigDecimal,
)
