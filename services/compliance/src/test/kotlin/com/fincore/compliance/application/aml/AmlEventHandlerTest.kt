// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.aml

import com.fincore.compliance.infrastructure.messaging.LedgerEntryLine
import com.fincore.compliance.infrastructure.messaging.LedgerTransactionPosted
import com.fincore.compliance.infrastructure.persistence.AmlAlertEntity
import com.fincore.compliance.infrastructure.persistence.AmlAlertRepository
import com.fincore.eventbus.consumer.IdempotentEventProcessor
import com.fincore.eventbus.consumer.InMemoryProcessedEventStore
import com.fincore.events.EventEnvelope
import com.fincore.events.LedgerEvents
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant

class AmlEventHandlerTest {
    private val evaluator = mockk<AmlEvaluator>()
    private val alerts = mockk<AmlAlertRepository>(relaxed = true)
    private val processor = IdempotentEventProcessor(InMemoryProcessedEventStore())
    private val handler = AmlEventHandler(processor, evaluator, alerts)

    @Test
    fun `should raise an alert carrying the subject when the evaluator flags`() {
        every { evaluator.evaluate(any()) } returns AmlDecision.Flagged(listOf("aml.velocity"))
        val saved = slot<AmlAlertEntity>()
        every { alerts.saveAndFlush(capture(saved)) } answers { firstArg() }

        handler.handle(envelope("tx_1"))

        saved.captured.subjectReference shouldBe "tx_1"
        saved.captured.ruleKey shouldBe "aml.velocity"
    }

    @Test
    fun `should not raise an alert when the evaluator clears`() {
        every { evaluator.evaluate(any()) } returns AmlDecision.Clear

        handler.handle(envelope("tx_2"))

        verify(exactly = 0) { alerts.saveAndFlush(any()) }
    }

    @Test
    fun `should sum only the debit entries into the evaluated amount`() {
        val view = slot<AmlTransactionView>()
        every { evaluator.evaluate(capture(view)) } returns AmlDecision.Clear

        handler.handle(envelope("tx_3"))

        view.captured.amount shouldBe java.math.BigDecimal("100.00")
    }

    @Test
    fun `should skip a duplicate envelope`() {
        every { evaluator.evaluate(any()) } returns AmlDecision.Flagged(listOf("aml.velocity"))
        every { alerts.saveAndFlush(any()) } answers { firstArg() }
        val duplicate = envelope("tx_4")

        handler.handle(duplicate)
        handler.handle(duplicate)

        verify(exactly = 1) { alerts.saveAndFlush(any()) }
    }

    private fun envelope(transactionId: String): EventEnvelope<LedgerTransactionPosted> =
        EventEnvelope.of(
            source = "ledger-service",
            type = LedgerEvents.TransactionPosted,
            data =
                LedgerTransactionPosted(
                    transactionId = transactionId,
                    reference = "order-1",
                    currency = "USD",
                    postedAt = Instant.now(),
                    entries =
                        listOf(
                            LedgerEntryLine("acc_1", "DEBIT", "100.00"),
                            LedgerEntryLine("acc_2", "CREDIT", "-100.00"),
                        ),
                ),
        )
}
