// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application.outbox

import com.fincore.ledger.config.OutboxDispatcherProperties
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import java.util.concurrent.TimeUnit

/**
 * Relays claimed outbox events to the broker. Orchestration only - not transactional. Each event is
 * published outside any transaction; settling its outcome is delegated to [OutboxClaimStore].
 */
class OutboxDispatcher(
    private val claimStore: OutboxClaimStore,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val properties: OutboxDispatcherProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun dispatch(): DispatchSummary {
        val claimed = claimStore.claim(properties.maxAttempts, properties.leaseTimeout, properties.batchSize)
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
            val topic = "${properties.topicPrefix}.${event.aggregateType.lowercase()}"
            kafkaTemplate
                .send(topic, event.aggregateId, event.payload)
                .get(properties.sendTimeout.toMillis(), TimeUnit.MILLISECONDS)
            claimStore.markPublished(event.id)
            true
        } catch (ex: Exception) {
            claimStore.markFailed(event.id, event.attempts + 1, properties.maxAttempts, ex.message)
            false
        }
}
