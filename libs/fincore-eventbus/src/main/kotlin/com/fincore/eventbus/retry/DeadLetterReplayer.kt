// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.retry

import org.apache.kafka.clients.consumer.Consumer
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration

class DeadLetterReplayer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    // awaits each send so the returned count is confirmed (throws if any re-publish fails)
    fun replay(
        consumer: Consumer<String, String>,
        targetTopic: String,
        pollTimeout: Duration,
    ): Int {
        val records = consumer.poll(pollTimeout)
        records
            .map { record -> kafkaTemplate.send(targetTopic, record.key(), record.value()) }
            .forEach { it.get() }
        return records.count()
    }
}
