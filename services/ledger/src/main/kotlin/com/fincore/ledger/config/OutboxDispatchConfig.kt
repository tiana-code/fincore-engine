// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.config

import com.fincore.ledger.application.outbox.OutboxClaimStore
import com.fincore.ledger.application.outbox.OutboxDispatcher
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@Configuration
@EnableScheduling
@ConditionalOnProperty(
    prefix = "fincore.ledger.dispatcher",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class OutboxDispatchConfig(
    claimStore: OutboxClaimStore,
    kafkaTemplate: KafkaTemplate<String, String>,
    properties: OutboxDispatcherProperties,
) {
    private val dispatcher = OutboxDispatcher(claimStore, kafkaTemplate, properties)

    @Bean
    fun outboxDispatcher(): OutboxDispatcher = dispatcher

    @Scheduled(fixedDelayString = "\${fincore.ledger.dispatcher.poll-delay}")
    fun poll() {
        dispatcher.dispatch()
    }
}
