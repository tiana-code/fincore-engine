// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application

import com.fincore.payments.domain.Payment

data class PaymentPage(
    val items: List<Payment>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
