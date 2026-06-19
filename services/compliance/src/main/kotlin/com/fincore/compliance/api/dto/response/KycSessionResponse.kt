// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.api.dto.response

data class KycSessionResponse(
    val id: String,
    val subjectReference: String,
    val status: String,
)
