// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.config

import com.fincore.eventbus.outbox.OutboxDispatchSettings
import com.fincore.eventbus.outbox.OutboxDispatcher
import com.fincore.payments.application.outbox.PaymentOutboxStore
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@Configuration
@EnableScheduling
@ConditionalOnProperty(
    prefix = "fincore.payments.dispatcher",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class PaymentOutboxDispatchConfig(
    store: PaymentOutboxStore,
    kafkaTemplate: KafkaTemplate<String, String>,
    properties: PaymentDispatcherProperties,
) {
    private val dispatcher =
        OutboxDispatcher(
            store,
            kafkaTemplate,
            OutboxDispatchSettings(
                batchSize = properties.batchSize,
                maxAttempts = properties.maxAttempts,
                leaseTimeout = properties.leaseTimeout,
                sendTimeout = properties.sendTimeout,
                topicPrefix = properties.topicPrefix,
            ),
        )

    @Bean
    fun paymentOutboxDispatcher(): OutboxDispatcher = dispatcher

    @Scheduled(fixedDelayString = "\${fincore.payments.dispatcher.poll-delay}")
    fun poll() {
        dispatcher.dispatch()
    }
}
