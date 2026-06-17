// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import com.fasterxml.jackson.databind.JsonNode
import com.fincore.decision.domain.AttrValue
import com.fincore.decision.domain.BoolValue
import com.fincore.decision.domain.DecimalValue
import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.domain.StringValue
import com.fincore.decision.parser.RuleParser
import com.fincore.decision.store.config.DecisionApiProperties
import com.fincore.decision.store.exception.InputNotMappableException
import com.fincore.decision.store.exception.InputTooLargeException
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
    private val logWriter: DecisionLogWriter,
    private val properties: DecisionApiProperties,
) : EvaluationService {
    @Transactional
    override fun evaluate(
        ruleKey: String,
        attributes: Map<String, JsonNode>,
    ): EvaluationOutcome {
        val version = loadActiveVersion(ruleKey)
        val rule = ruleParser.parse(version.dsl)
        val input = toEvaluationInput(attributes)
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

    private fun toEvaluationInput(attributes: Map<String, JsonNode>): EvaluationInput {
        if (attributes.size > properties.maxInputAttributes) throw InputTooLargeException(properties.maxInputAttributes)
        return EvaluationInput(attributes.mapValues { (_, node) -> toAttrValue(node) })
    }

    private fun toAttrValue(node: JsonNode): AttrValue =
        when {
            node.isTextual -> StringValue(boundedText(node.textValue()))
            node.isBoolean -> BoolValue(node.booleanValue())
            node.isNumber -> DecimalValue(node.decimalValue())
            else -> throw InputNotMappableException()
        }

    private fun boundedText(value: String): String {
        if (value.length > properties.maxInputValueChars) throw InputTooLargeException(properties.maxInputValueChars)
        return value
    }
}
