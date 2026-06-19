// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/** Enables @Async and provides a bounded executor for post-commit payment orchestration. */
@Configuration
@EnableAsync
class PaymentAsyncConfig {
    @Bean
    fun paymentOrchestrationExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = CORE_POOL_SIZE
            maxPoolSize = MAX_POOL_SIZE
            setQueueCapacity(QUEUE_CAPACITY)
            setThreadNamePrefix("payment-orch-")
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS)
            initialize()
        }

    private companion object {
        const val CORE_POOL_SIZE = 2
        const val MAX_POOL_SIZE = 8
        const val QUEUE_CAPACITY = 100
        const val AWAIT_TERMINATION_SECONDS = 20
    }
}
