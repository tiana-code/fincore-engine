// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import com.fasterxml.jackson.databind.JsonNode
import com.fincore.decision.parser.RuleParser
import com.fincore.decision.store.exception.RuleNotActiveException
import com.fincore.decision.store.exception.RuleNotFoundException
import com.fincore.decision.store.persistence.DecisionRuleRepository
import com.fincore.decision.store.persistence.RuleVersionEntity
import com.fincore.decision.store.persistence.RuleVersionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EvaluationServiceImpl(
    private val ruleRepository: DecisionRuleRepository,
    private val versionRepository: RuleVersionRepository,
    private val ruleParser: RuleParser,
    private val boundedEvaluator: BoundedEvaluator,
    private val inputHasher: InputHasher,
    private val inputMapper: InputMapper,
    private val logWriter: DecisionLogWriter,
) : EvaluationService {
    @Transactional
    override fun evaluate(
        ruleKey: String,
        attributes: Map<String, JsonNode>,
    ): EvaluationOutcome {
        val version = loadActiveVersion(ruleKey)
        val rule = ruleParser.parse(version.dsl)
        val input = inputMapper.toEvaluationInput(attributes)
        val hash = inputHasher.hash(input)
        val result = boundedEvaluator.evaluate(rule, input)
        val logId = logWriter.write(version.id, hash, result)
        return EvaluationOutcome(result, logId)
    }

    private fun loadActiveVersion(ruleKey: String): RuleVersionEntity {
        val rule = ruleRepository.findByRuleKey(ruleKey) ?: throw RuleNotFoundException(ruleKey)
        val versionId = rule.activeVersionId ?: throw RuleNotActiveException(ruleKey)
        return versionRepository.findById(versionId).orElseThrow { RuleNotActiveException(ruleKey) }
    }
}
