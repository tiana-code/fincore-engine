// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.api.dto.response

data class CaseResponse(
    val id: String,
    val reference: String,
    val status: String,
)
