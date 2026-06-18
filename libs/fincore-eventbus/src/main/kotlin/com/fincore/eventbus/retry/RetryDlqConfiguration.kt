// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.retry

import org.apache.kafka.common.TopicPartition
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.ExponentialBackOff

@Configuration
class RetryDlqConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun retryTopicNaming(properties: RetryDlqProperties): RetryTopicNaming =
        RetryTopicNaming(properties.retrySuffix, properties.deadLetterSuffix)

    @Bean
    @ConditionalOnMissingBean
    fun deadLetterPublishingRecoverer(
        kafkaTemplate: KafkaTemplate<String, String>,
        naming: RetryTopicNaming,
    ): DeadLetterPublishingRecoverer =
        DeadLetterPublishingRecoverer(kafkaTemplate) { record, _ ->
            // -1: producer places by key, preserving affinity without matching source partition count
            TopicPartition(naming.deadLetterTopic(record.topic()), PARTITION_BY_KEY)
        }

    @Bean
    @ConditionalOnMissingBean
    fun kafkaErrorHandler(
        recoverer: DeadLetterPublishingRecoverer,
        properties: RetryDlqProperties,
    ): DefaultErrorHandler =
        DefaultErrorHandler(
            recoverer,
            ExponentialBackOff().apply {
                initialInterval = properties.initialBackoff.toMillis()
                multiplier = properties.backoffMultiplier
                maxInterval = properties.maxBackoff.toMillis()
                // the initial delivery counts as attempt 1, so this caps the number of RETRIES
                maxAttempts = properties.maxAttempts - 1
            },
        )

    @Bean
    @ConditionalOnMissingBean
    fun deadLetterReplayer(kafkaTemplate: KafkaTemplate<String, String>): DeadLetterReplayer = DeadLetterReplayer(kafkaTemplate)

    private companion object {
        const val PARTITION_BY_KEY = -1
    }
}
