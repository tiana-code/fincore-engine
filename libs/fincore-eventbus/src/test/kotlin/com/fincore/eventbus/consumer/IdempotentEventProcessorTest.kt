// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.consumer

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.UUID

class IdempotentEventProcessorTest {
    private val processor = IdempotentEventProcessor(InMemoryProcessedEventStore())

    @Test
    fun `should run the handler and report processed when the envelope is first seen`() {
        var calls = 0

        val outcome = processor.process(UUID.randomUUID(), "group", { calls++ })

        outcome shouldBe EventProcessingOutcome.PROCESSED
        calls shouldBe 1
    }

    @Test
    fun `should skip the handler and report duplicate when the envelope was already processed`() {
        val id = UUID.randomUUID()
        var calls = 0
        processor.process(id, "group", { calls++ })

        val outcome = processor.process(id, "group", { calls++ })

        outcome shouldBe EventProcessingOutcome.DUPLICATE_SKIPPED
        calls shouldBe 1
    }

    @Test
    fun `should process the same envelope once per consumer group`() {
        val id = UUID.randomUUID()
        var calls = 0
        processor.process(id, "group-a", { calls++ })

        val outcome = processor.process(id, "group-b", { calls++ })

        outcome shouldBe EventProcessingOutcome.PROCESSED
        calls shouldBe 2
    }

    @Test
    fun `should propagate the exception when the handler throws`() {
        val id = UUID.randomUUID()

        shouldThrow<IllegalStateException> {
            processor.process(id, "group", { error("handler failed") })
        }

        // in-memory has no tx so the claim survives a thrown handler; rollback is a JDBC property (see the IT)
        processor.process(id, "group", { }) shouldBe EventProcessingOutcome.DUPLICATE_SKIPPED
    }
}
