// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.infrastructure.bank

import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigDecimal
import java.time.Duration

@ConfigurationProperties(prefix = "fincore.payments.bank.sandbox")
data class SandboxBankProperties(
    val rejectAmount: BigDecimal = DEFAULT_REJECT_AMOUNT,
    val delayAmount: BigDecimal = DEFAULT_DELAY_AMOUNT,
    val delay: Duration = DEFAULT_DELAY,
    val maxDelay: Duration = DEFAULT_MAX_DELAY,
) {
    private companion object {
        val DEFAULT_REJECT_AMOUNT: BigDecimal = BigDecimal("999.99")
        val DEFAULT_DELAY_AMOUNT: BigDecimal = BigDecimal("888.88")
        val DEFAULT_DELAY: Duration = Duration.ofSeconds(2)
        val DEFAULT_MAX_DELAY: Duration = Duration.ofSeconds(10)
    }
}
