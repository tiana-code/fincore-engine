// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.decision.eval.RuleEvaluator
import com.fincore.decision.parser.RuleParser
import com.fincore.decision.store.config.DecisionApiProperties
import com.fincore.decision.store.exception.DslTooLargeException
import com.fincore.decision.store.exception.InputTooLargeException
import com.fincore.decision.store.exception.InvalidRuleDslException
import com.fincore.decision.store.persistence.DecisionLogEntity
import com.fincore.decision.store.persistence.DecisionLogRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors

class ReplayServiceImplTest {
    private val json = ObjectMapper()
    private val props = DecisionApiProperties()
    private val watchdog = Executors.newSingleThreadScheduledExecutor()
    private val repository = mockk<DecisionLogRepository>()
    private val service =
        ReplayServiceImpl(
            RuleParser(),
            BoundedEvaluator(RuleEvaluator(), watchdog, props),
            InputMapper(props),
            InputHasher(json),
            repository,
            props,
        )
    private val approveAtHundred = """{"condition":{"attr":"amount","op":"gte","value":100},"outcome":{"label":"approve"}}"""
    private val approveAtThousand = """{"condition":{"attr":"amount","op":"gte","value":1000},"outcome":{"label":"approve"}}"""

    @AfterEach
    fun tearDown() {
        watchdog.shutdownNow()
    }

    private fun inputs(vararg raw: String): List<Map<String, JsonNode>> =
        raw.map {
            (json.readTree(it) as com.fasterxml.jackson.databind.node.ObjectNode).fields().asSequence().associate { f ->
                f.key to
                    f.value
            }
        }

    private fun recorded(
        matched: Boolean,
        label: String?,
    ): DecisionLogEntity =
        DecisionLogEntity(
            id = UUID.randomUUID(),
            evaluatedAt = Instant.now(),
            ruleVersionId = UUID.randomUUID(),
            inputHash = "h",
            matched = matched,
            outcomeLabel = label,
            trace = "[]",
        )

    @Test
    fun `should report changed when the candidate outcome differs from the recorded decision`() {
        every { repository.findFirstByInputHashOrderByEvaluatedAtDesc(any()) } returns recorded(true, "approve")

        val report = service.replay(approveAtThousand, inputs("""{"amount":150}"""))

        report.changed shouldBe 1
        report.diffs.single().status shouldBe DiffStatus.CHANGED
    }

    @Test
    fun `should report unchanged when the candidate reproduces the recorded decision`() {
        every { repository.findFirstByInputHashOrderByEvaluatedAtDesc(any()) } returns recorded(true, "approve")

        val report = service.replay(approveAtHundred, inputs("""{"amount":150}"""))

        report.unchanged shouldBe 1
        report.diffs.single().status shouldBe DiffStatus.UNCHANGED
    }

    @Test
    fun `should report unchanged when both recorded and candidate do not match`() {
        every { repository.findFirstByInputHashOrderByEvaluatedAtDesc(any()) } returns recorded(false, null)

        val report = service.replay(approveAtThousand, inputs("""{"amount":50}"""))

        report.diffs.single().status shouldBe DiffStatus.UNCHANGED
    }

    @Test
    fun `should report no baseline when the input has no recorded decision`() {
        every { repository.findFirstByInputHashOrderByEvaluatedAtDesc(any()) } returns null

        val report = service.replay(approveAtHundred, inputs("""{"amount":150}"""))

        report.noBaseline shouldBe 1
        report.diffs.single().status shouldBe DiffStatus.NO_BASELINE
    }

    @Test
    fun `should aggregate counts that sum to the total and never write a row`() {
        every { repository.findFirstByInputHashOrderByEvaluatedAtDesc(any()) } returns recorded(true, "approve")

        val report = service.replay(approveAtHundred, inputs("""{"amount":150}""", """{"amount":50}"""))

        report.total shouldBe 2
        (report.unchanged + report.changed + report.noBaseline) shouldBe report.total
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `should reject an invalid candidate dsl`() {
        shouldThrow<InvalidRuleDslException> { service.replay("""{"outcome":{"label":"x"}}""", inputs("""{"amount":1}""")) }
    }

    @Test
    fun `should reject a candidate over the size cap`() {
        val tiny =
            ReplayServiceImpl(
                RuleParser(),
                BoundedEvaluator(RuleEvaluator(), watchdog, props),
                InputMapper(props),
                InputHasher(json),
                repository,
                DecisionApiProperties(maxDslChars = 2),
            )

        shouldThrow<DslTooLargeException> { tiny.replay(approveAtHundred, inputs("""{"amount":1}""")) }
    }

    @Test
    fun `should reject more inputs than the replay cap`() {
        val capped =
            ReplayServiceImpl(
                RuleParser(),
                BoundedEvaluator(RuleEvaluator(), watchdog, props),
                InputMapper(props),
                InputHasher(json),
                repository,
                DecisionApiProperties(maxReplayInputs = 1),
            )

        shouldThrow<InputTooLargeException> { capped.replay(approveAtHundred, inputs("""{"amount":1}""", """{"amount":2}""")) }
    }
}
