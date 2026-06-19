// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.aml

import java.math.BigDecimal

data class WindowAggregate(
    val count: Long,
    val total: BigDecimal,
)
