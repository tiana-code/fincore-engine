// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.sdk.model

data class Account(
    val id: String,
    val name: String,
    val type: String,
    val currency: String,
    val status: String,
)
