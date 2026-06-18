// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application.outbox

import com.fincore.ledger.config.OutboxDispatcherProperties
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture

class OutboxDispatcherTest {
    private val claimStore = mockk<OutboxClaimStore>(relaxed = true)
    private val kafkaTemplate = mockk<KafkaTemplate<String, String>>()
    private val properties =
        OutboxDispatcherProperties(
            enabled = true,
            batchSize = 10,
            maxAttempts = 5,
            sendTimeout = Duration.ofSeconds(2),
            topicPrefix = "fincore",
        )
    private val dispatcher = OutboxDispatcher(claimStore, kafkaTemplate, properties)

    @Test
    fun `should publish each event to the prefixed aggregate topic keyed by aggregate id`() {
        val id = UUID.randomUUID()
        val event = ClaimedEvent(id, "Transaction", "tx_7", "type", "{\"id\":\"e1\"}", attempts = 0)
        every { claimStore.claim(any(), any(), any()) } returns listOf(event)
        every { kafkaTemplate.send("fincore.transaction", "tx_7", "{\"id\":\"e1\"}") } returns completed()

        val summary = dispatcher.dispatch()

        summary shouldBe DispatchSummary(published = 1, failed = 0)
        verify { kafkaTemplate.send("fincore.transaction", "tx_7", "{\"id\":\"e1\"}") }
        verify { claimStore.markPublished(id) }
    }

    @Test
    fun `should continue the batch and settle a failed publish when one event throws`() {
        val badId = UUID.randomUUID()
        val okId = UUID.randomUUID()
        val bad = ClaimedEvent(badId, "Transaction", "tx_bad", "type", "{}", attempts = 0)
        val ok = ClaimedEvent(okId, "Transaction", "tx_ok", "type", "{}", attempts = 0)
        every { claimStore.claim(any(), any(), any()) } returns listOf(bad, ok)
        every { kafkaTemplate.send("fincore.transaction", "tx_bad", any()) } returns
            CompletableFuture.failedFuture(RuntimeException("broker down"))
        every { kafkaTemplate.send("fincore.transaction", "tx_ok", any()) } returns completed()

        val summary = dispatcher.dispatch()

        summary shouldBe DispatchSummary(published = 1, failed = 1)
        verify { claimStore.markPublished(okId) }
        verify { claimStore.markFailed(badId, 1, 5, any()) }
    }

    @Test
    fun `should not register a dispatcher bean when the dispatcher is disabled`() {
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of())
            .withUserConfiguration(com.fincore.ledger.config.OutboxDispatchConfig::class.java)
            .run { context ->
                context.getBeanNamesForType(OutboxDispatcher::class.java).size shouldBe 0
            }
    }

    private fun completed(): CompletableFuture<SendResult<String, String>> = CompletableFuture.completedFuture(mockk(relaxed = true))
}
