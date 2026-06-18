// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.outbox

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import java.util.concurrent.TimeUnit

/**
 * Relays claimed outbox events to the broker. Orchestration only - not transactional. Each event is
 * published outside any transaction; claiming and settling its outcome are delegated to [OutboxStore].
 */
class OutboxDispatcher(
    private val store: OutboxStore,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val settings: OutboxDispatchSettings,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun dispatch(): DispatchSummary {
        val claimed = store.claim(settings.maxAttempts, settings.leaseTimeout, settings.batchSize)
        var published = 0
        for (event in claimed) {
            if (publish(event)) published++
        }
        if (claimed.isNotEmpty()) {
            log
                .atInfo()
                .addKeyValue("claimed", claimed.size)
                .addKeyValue("published", published)
                .addKeyValue("failed", claimed.size - published)
                .log("outbox dispatch cycle completed")
        }
        return DispatchSummary(published, claimed.size - published)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun publish(event: ClaimedEvent): Boolean =
        try {
            val topic = "${settings.topicPrefix}.${event.aggregateType.lowercase()}"
            kafkaTemplate
                .send(topic, event.aggregateId, event.payload)
                .get(settings.sendTimeout.toMillis(), TimeUnit.MILLISECONDS)
            store.markPublished(event.id)
            true
        } catch (ex: Exception) {
            store.markFailed(event.id, event.attempts + 1, settings.maxAttempts, ex.message)
            false
        }
}
