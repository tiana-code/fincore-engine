// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.aml

import java.math.BigDecimal
import java.time.Instant

data class AmlObservation(
    val occurredAt: Instant,
    val amount: BigDecimal,
)
