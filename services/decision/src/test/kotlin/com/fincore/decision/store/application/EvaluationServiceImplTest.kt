// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fincore.decision.domain.ConditionTrace
import com.fincore.decision.domain.DecisionOutcome
import com.fincore.decision.domain.DecisionResult
import com.fincore.decision.domain.DecisionRule
import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.parser.RuleParser
import com.fincore.decision.store.config.DecisionApiProperties
import com.fincore.decision.store.exception.EvaluationTimeoutException
import com.fincore.decision.store.exception.RuleNotActiveException
import com.fincore.decision.store.exception.RuleNotFoundException
import com.fincore.decision.store.persistence.DecisionRuleEntity
import com.fincore.decision.store.persistence.DecisionRuleRepository
import com.fincore.decision.store.persistence.RuleVersionEntity
import com.fincore.decision.store.persistence.RuleVersionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class EvaluationServiceImplTest {
    private val ruleRepository = mockk<DecisionRuleRepository>()
    private val versionRepository = mockk<RuleVersionRepository>()
    private val boundedEvaluator = mockk<BoundedEvaluator>()
    private val logWriter = mockk<DecisionLogWriter>()
    private val mapper = ObjectMapper()
    private val validDsl = """{"condition":{"attr":"amount","op":"gte","value":100},"outcome":{"label":"approve"}}"""
    private val matched = DecisionResult(true, DecisionOutcome("approve", listOf("LOW_RISK")), listOf(ConditionTrace("amount gte", true)))

    private fun service(props: DecisionApiProperties = DecisionApiProperties()) =
        EvaluationServiceImpl(
            ruleRepository,
            versionRepository,
            RuleParser(),
            boundedEvaluator,
            InputHasher(mapper),
            InputMapper(props),
            logWriter,
        )

    private fun attrs(json: String): Map<String, JsonNode> =
        (mapper.readTree(json) as ObjectNode).fields().asSequence().associate { it.key to it.value }

    private fun activeRule(versionId: UUID): DecisionRuleEntity =
        DecisionRuleEntity(id = UUID.randomUUID(), ruleKey = "k", activeVersionId = versionId)

    @Test
    fun `should evaluate the active version and write an audit log when input is valid`() {
        val versionId = UUID.randomUUID()
        val logId = UUID.randomUUID()
        every { ruleRepository.findByRuleKey("k") } returns activeRule(versionId)
        every { versionRepository.findById(versionId) } returns
            Optional.of(RuleVersionEntity(id = versionId, ruleId = UUID.randomUUID(), versionNo = 1, dsl = validDsl))
        every { boundedEvaluator.evaluate(any<DecisionRule>(), any<EvaluationInput>()) } returns matched
        every { logWriter.write(versionId, any(), matched) } returns logId

        val outcome = service().evaluate("k", attrs("""{"amount":150}"""))

        outcome.decisionLogId shouldBe logId
        outcome.result.matched shouldBe true
        verify(exactly = 1) { logWriter.write(versionId, any(), matched) }
    }

    @Test
    fun `should reject evaluating an unknown rule`() {
        every { ruleRepository.findByRuleKey("missing") } returns null

        shouldThrow<RuleNotFoundException> { service().evaluate("missing", attrs("""{"amount":1}""")) }
    }

    @Test
    fun `should reject evaluating a rule with no active version`() {
        every { ruleRepository.findByRuleKey("k") } returns DecisionRuleEntity(id = UUID.randomUUID(), ruleKey = "k")

        shouldThrow<RuleNotActiveException> { service().evaluate("k", attrs("""{"amount":1}""")) }
    }

    @Test
    fun `should not write a log when evaluation times out`() {
        val versionId = UUID.randomUUID()
        every { ruleRepository.findByRuleKey("k") } returns activeRule(versionId)
        every { versionRepository.findById(versionId) } returns
            Optional.of(RuleVersionEntity(id = versionId, ruleId = UUID.randomUUID(), versionNo = 1, dsl = validDsl))
        every { boundedEvaluator.evaluate(any<DecisionRule>(), any<EvaluationInput>()) } throws EvaluationTimeoutException(1)

        shouldThrow<EvaluationTimeoutException> { service().evaluate("k", attrs("""{"amount":1}""")) }
        verify(exactly = 0) { logWriter.write(any(), any(), any()) }
    }
}
