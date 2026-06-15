// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.dto.request

import jakarta.validation.constraints.Size

data class ReverseTransactionRequest(
    @field:Size(max = 512)
    val reason: String? = null,
)
