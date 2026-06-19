// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.sdk.model

data class Transaction(
    val id: String,
    val reference: String,
    val status: String,
    val postedAt: String,
)
