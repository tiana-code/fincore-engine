// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application

import com.fincore.payments.domain.enum.PaymentStatus
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class PaymentMetrics(
    private val registry: MeterRegistry,
) {
    fun record(status: PaymentStatus) = registry.counter(METRIC, TAG_STATUS, status.name.lowercase()).increment()

    private companion object {
        const val METRIC = "payments.lifecycle"
        const val TAG_STATUS = "status"
    }
}
