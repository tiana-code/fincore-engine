// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.sdk.model

data class KycSession(
    val id: String,
    val subjectReference: String,
    val status: String,
)
