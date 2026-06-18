// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.retry

import org.apache.kafka.clients.consumer.Consumer
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration

/**
 * Re-drives dead-lettered records back to a target topic. Polls a caller-supplied consumer (already
 * subscribed to the dead-letter topic, so offset and commit policy stay with the caller) once and
 * re-publishes each record preserving its key. Non-destructive: the dead-letter topic is not mutated.
 */
class DeadLetterReplayer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    /** Returns the number of records confirmed re-published. Throws if any send fails, so an
     *  incident-recovery caller never sees a success count for records that did not land. */
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
