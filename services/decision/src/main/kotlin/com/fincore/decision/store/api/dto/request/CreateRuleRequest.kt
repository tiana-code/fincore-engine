// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateRuleRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 128)
    @field:Pattern(regexp = "^[A-Za-z0-9_.:-]+$")
    val ruleKey: String,
)
