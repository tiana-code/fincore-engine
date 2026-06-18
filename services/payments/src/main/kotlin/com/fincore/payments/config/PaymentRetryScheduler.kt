// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.config

import com.fincore.payments.application.retry.PaymentRetryService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "fincore.payments.retry", name = ["enabled"], havingValue = "true", matchIfMissing = false)
class PaymentRetryScheduler(
    private val retryService: PaymentRetryService,
) {
    @Scheduled(cron = "\${fincore.payments.retry.cron}")
    fun retryStuck() {
        retryService.retryStuck()
    }
}
