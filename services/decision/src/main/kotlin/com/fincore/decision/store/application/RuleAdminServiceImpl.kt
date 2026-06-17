// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import com.fincore.decision.domain.DecisionDslException
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
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RuleAdminServiceImpl(
    private val ruleRepository: DecisionRuleRepository,
    private val versionRepository: RuleVersionRepository,
    private val ruleParser: RuleParser,
    private val properties: DecisionApiProperties,
) : RuleAdminService {
    @Transactional
    override fun createRule(ruleKey: String): RuleView {
        if (ruleRepository.findByRuleKey(ruleKey) != null) throw DuplicateRuleKeyException(ruleKey)
        val saved =
            try {
                ruleRepository.saveAndFlush(DecisionRuleEntity(id = UUID.randomUUID(), ruleKey = ruleKey))
            } catch (ex: DataIntegrityViolationException) {
                throw DuplicateRuleKeyException(ruleKey, ex)
            }
        return RuleView(saved.id, saved.ruleKey)
    }

    @Transactional
    override fun publishVersion(
        ruleKey: String,
        dslJson: String,
    ): VersionView {
        validate(dslJson)
        val rule = loadRule(ruleKey)
        val nextNo = (versionRepository.findMaxVersionNo(rule.id) ?: 0) + 1
        val version =
            try {
                versionRepository.saveAndFlush(
                    RuleVersionEntity(id = UUID.randomUUID(), ruleId = rule.id, versionNo = nextNo, dsl = dslJson),
                )
            } catch (ex: DataIntegrityViolationException) {
                throw VersionConflictException(ruleKey, ex)
            }
        rule.activeVersionId = version.id
        ruleRepository.saveAndFlush(rule)
        return VersionView(version.id, version.versionNo)
    }

    @Transactional(readOnly = true)
    override fun getRule(ruleKey: String): RuleDetailView {
        val rule = loadRule(ruleKey)
        val active =
            rule.activeVersionId
                ?.let { versionRepository.findById(it).orElse(null) }
                ?.let { ActiveVersionView(it.versionNo, it.dsl) }
        return RuleDetailView(rule.id, rule.ruleKey, active)
    }

    private fun validate(dslJson: String) {
        if (dslJson.length > properties.maxDslChars) throw DslTooLargeException(properties.maxDslChars)
        try {
            ruleParser.parse(dslJson)
        } catch (ex: DecisionDslException) {
            throw InvalidRuleDslException(ex.code, ex.message, ex)
        }
    }

    private fun loadRule(ruleKey: String): DecisionRuleEntity =
        ruleRepository.findByRuleKey(ruleKey) ?: throw RuleNotFoundException(ruleKey)
}
