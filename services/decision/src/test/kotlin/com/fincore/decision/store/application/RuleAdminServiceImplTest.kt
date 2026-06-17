// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import com.fincore.decision.domain.DslErrorCode
import com.fincore.decision.parser.RuleParser
import com.fincore.decision.store.config.DecisionApiProperties
import com.fincore.decision.store.exception.DslTooLargeException
import com.fincore.decision.store.exception.DuplicateRuleKeyException
import com.fincore.decision.store.exception.InvalidRuleDslException
import com.fincore.decision.store.exception.RuleNotFoundException
import com.fincore.decision.store.exception.VersionConflictException
import com.fincore.decision.store.persistence.DecisionRuleEntity
import com.fincore.decision.store.persistence.DecisionRuleRepository
import com.fincore.decision.store.persistence.RuleVersionEntity
import com.fincore.decision.store.persistence.RuleVersionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.util.Optional
import java.util.UUID

class RuleAdminServiceImplTest {
    private val ruleRepository = mockk<DecisionRuleRepository>()
    private val versionRepository = mockk<RuleVersionRepository>()
    private val service = RuleAdminServiceImpl(ruleRepository, versionRepository, RuleParser(), DecisionApiProperties())
    private val validDsl = """{"condition":{"attr":"amount","op":"gte","value":100},"outcome":{"label":"approve"}}"""

    @Test
    fun `should create a rule when the key is new`() {
        every { ruleRepository.findByRuleKey("k") } returns null
        val slot = slot<DecisionRuleEntity>()
        every { ruleRepository.saveAndFlush(capture(slot)) } answers { slot.captured }

        service.createRule("k").ruleKey shouldBe "k"
    }

    @Test
    fun `should reject creating a rule when the key already exists`() {
        every { ruleRepository.findByRuleKey("k") } returns mockk()

        shouldThrow<DuplicateRuleKeyException> { service.createRule("k") }
    }

    @Test
    fun `should map a unique violation to a duplicate key conflict when two creates race`() {
        every { ruleRepository.findByRuleKey("k") } returns null
        every { ruleRepository.saveAndFlush(any()) } throws DataIntegrityViolationException("dup")

        shouldThrow<DuplicateRuleKeyException> { service.createRule("k") }
    }

    @Test
    fun `should publish the first version and point the rule at it when published`() {
        val rule = DecisionRuleEntity(id = UUID.randomUUID(), ruleKey = "k")
        every { ruleRepository.findByRuleKey("k") } returns rule
        every { versionRepository.findMaxVersionNo(rule.id) } returns null
        val slot = slot<RuleVersionEntity>()
        every { versionRepository.saveAndFlush(capture(slot)) } answers { slot.captured }
        every { ruleRepository.saveAndFlush(rule) } returns rule

        val view = service.publishVersion("k", validDsl)

        view.versionNo shouldBe 1
        rule.activeVersionId shouldBe slot.captured.id
    }

    @Test
    fun `should assign the next monotonic version number when a later version is published`() {
        val rule = DecisionRuleEntity(id = UUID.randomUUID(), ruleKey = "k")
        every { ruleRepository.findByRuleKey("k") } returns rule
        every { versionRepository.findMaxVersionNo(rule.id) } returns 1
        val slot = slot<RuleVersionEntity>()
        every { versionRepository.saveAndFlush(capture(slot)) } answers { slot.captured }
        every { ruleRepository.saveAndFlush(rule) } returns rule

        service.publishVersion("k", validDsl).versionNo shouldBe 2
    }

    @Test
    fun `should reject an invalid dsl with its error code when published`() {
        shouldThrow<InvalidRuleDslException> {
            service.publishVersion("k", """{"outcome":{"label":"approve"}}""")
        }.dslCode shouldBe DslErrorCode.MISSING_FIELD
    }

    @Test
    fun `should reject a dsl document over the size cap before parsing`() {
        val tiny = RuleAdminServiceImpl(ruleRepository, versionRepository, RuleParser(), DecisionApiProperties(maxDslChars = 2))

        shouldThrow<DslTooLargeException> { tiny.publishVersion("k", validDsl) }
    }

    @Test
    fun `should reject publishing a version for an unknown rule`() {
        every { ruleRepository.findByRuleKey("missing") } returns null

        shouldThrow<RuleNotFoundException> { service.publishVersion("missing", validDsl) }
    }

    @Test
    fun `should map a unique violation to a version conflict when two publishes race`() {
        val rule = DecisionRuleEntity(id = UUID.randomUUID(), ruleKey = "k")
        every { ruleRepository.findByRuleKey("k") } returns rule
        every { versionRepository.findMaxVersionNo(rule.id) } returns null
        every { versionRepository.saveAndFlush(any()) } throws DataIntegrityViolationException("dup")

        shouldThrow<VersionConflictException> { service.publishVersion("k", validDsl) }
    }

    @Test
    fun `should return the active version dsl when the rule is read`() {
        val versionId = UUID.randomUUID()
        val rule = DecisionRuleEntity(id = UUID.randomUUID(), ruleKey = "k", activeVersionId = versionId)
        every { ruleRepository.findByRuleKey("k") } returns rule
        every { versionRepository.findById(versionId) } returns
            Optional.of(RuleVersionEntity(id = versionId, ruleId = rule.id, versionNo = 2, dsl = validDsl))

        val detail = service.getRule("k")

        detail.activeVersion?.versionNo shouldBe 2
        detail.activeVersion?.dsl shouldBe validDsl
    }

    @Test
    fun `should return a null active version when the rule has no version`() {
        val rule = DecisionRuleEntity(id = UUID.randomUUID(), ruleKey = "k")
        every { ruleRepository.findByRuleKey("k") } returns rule

        service.getRule("k").activeVersion shouldBe null
    }

    @Test
    fun `should reject reading an unknown rule`() {
        every { ruleRepository.findByRuleKey("x") } returns null

        shouldThrow<RuleNotFoundException> { service.getRule("x") }
    }
}
