// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.retry

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

class DeadLetterReplayerTest {
    private val consumer = mockk<Consumer<String, String>>()
    private val kafkaTemplate = mockk<KafkaTemplate<String, String>>()
    private val replayer = DeadLetterReplayer(kafkaTemplate)

    @Test
    fun `should republish each polled record to the target topic and return the confirmed count`() {
        every { consumer.poll(any<Duration>()) } returns records("k1" to "v1", "k2" to "v2")
        every { kafkaTemplate.send("payments", any(), any()) } returns completed()

        val replayed = replayer.replay(consumer, "payments", Duration.ofSeconds(1))

        replayed shouldBe 2
        verify { kafkaTemplate.send("payments", "k1", "v1") }
        verify { kafkaTemplate.send("payments", "k2", "v2") }
    }

    @Test
    fun `should propagate the failure when a republish does not confirm`() {
        every { consumer.poll(any<Duration>()) } returns records("k1" to "v1")
        every { kafkaTemplate.send("payments", "k1", "v1") } returns
            CompletableFuture.failedFuture(RuntimeException("broker down"))

        shouldThrow<ExecutionException> {
            replayer.replay(consumer, "payments", Duration.ofSeconds(1))
        }
    }

    private fun records(vararg entries: Pair<String, String>): ConsumerRecords<String, String> {
        val partition = TopicPartition("payments-dlt", 0)
        val list = entries.mapIndexed { i, (k, v) -> ConsumerRecord("payments-dlt", 0, i.toLong(), k, v) }
        return ConsumerRecords(mapOf(partition to list))
    }

    private fun completed(): CompletableFuture<SendResult<String, String>> = CompletableFuture.completedFuture(mockk(relaxed = true))
}
