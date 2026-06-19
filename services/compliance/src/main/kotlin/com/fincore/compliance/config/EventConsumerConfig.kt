// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.config

import com.fincore.eventbus.consumer.IdempotentEventProcessor
import com.fincore.eventbus.consumer.JdbcProcessedEventStore
import com.fincore.eventbus.consumer.ProcessedEventStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate

@Configuration
class EventConsumerConfig {
    @Bean
    fun processedEventStore(jdbcTemplate: JdbcTemplate): ProcessedEventStore = JdbcProcessedEventStore(jdbcTemplate)

    @Bean
    fun idempotentEventProcessor(store: ProcessedEventStore): IdempotentEventProcessor = IdempotentEventProcessor(store)
}
