// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fincore.compliance.application.aml.AmlEventHandler
import com.fincore.events.EventEnvelope
import com.fincore.events.EventType
import com.fincore.events.LedgerEvents
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant

class AmlTransactionConsumerTest {
    private val mapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
    private val handler = mockk<AmlEventHandler>(relaxed = true)
    private val consumer = AmlTransactionConsumer(mapper, handler)

    @Test
    fun `should deserialize a typed payload and forward a transaction-posted event`() {
        val captured = slot<EventEnvelope<LedgerTransactionPosted>>()
        every { handler.handle(capture(captured)) } just Runs

        consumer.onMessage(mapper.writeValueAsString(envelope(LedgerEvents.TransactionPosted)))

        captured.captured.data.transactionId shouldBe "tx_1"
        captured.captured.data.entries.size shouldBe 2
    }

    @Test
    fun `should ignore a non transaction-posted event on the same topic`() {
        consumer.onMessage(mapper.writeValueAsString(envelope(LedgerEvents.TransactionReversed)))

        verify(exactly = 0) { handler.handle(any()) }
    }

    private fun envelope(type: EventType): EventEnvelope<LedgerTransactionPosted> =
        EventEnvelope.of(
            source = "ledger-service",
            type = type,
            data =
                LedgerTransactionPosted(
                    transactionId = "tx_1",
                    reference = "order-1",
                    currency = "USD",
                    postedAt = Instant.parse("2026-06-19T00:00:00Z"),
                    entries =
                        listOf(
                            LedgerEntryLine("acc_1", "DEBIT", "100.00"),
                            LedgerEntryLine("acc_2", "CREDIT", "-100.00"),
                        ),
                ),
        )
}
