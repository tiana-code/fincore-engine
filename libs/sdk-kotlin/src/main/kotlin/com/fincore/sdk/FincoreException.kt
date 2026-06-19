// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.sdk

class FincoreException(
    val status: Int,
    message: String,
) : RuntimeException(message)
