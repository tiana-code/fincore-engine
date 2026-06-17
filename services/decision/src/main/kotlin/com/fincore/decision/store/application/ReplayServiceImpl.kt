// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import com.fasterxml.jackson.databind.JsonNode
import com.fincore.decision.domain.DecisionDslException
import com.fincore.decision.domain.DecisionRule
import com.fincore.decision.parser.RuleParser
import com.fincore.decision.store.config.DecisionApiProperties
import com.fincore.decision.store.exception.DslTooLargeException
import com.fincore.decision.store.exception.InputTooLargeException
import com.fincore.decision.store.exception.InvalidRuleDslException
import com.fincore.decision.store.persistence.DecisionLogEntity
import com.fincore.decision.store.persistence.DecisionLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReplayServiceImpl(
    private val ruleParser: RuleParser,
    private val boundedEvaluator: BoundedEvaluator,
    private val inputMapper: InputMapper,
    private val inputHasher: InputHasher,
    private val decisionLogRepository: DecisionLogRepository,
    private val properties: DecisionApiProperties,
) : ReplayService {
    @Transactional(readOnly = true)
    override fun replay(
        candidateDsl: String,
        inputs: List<Map<String, JsonNode>>,
    ): ReplayReport {
        if (inputs.size > properties.maxReplayInputs) throw InputTooLargeException(properties.maxReplayInputs)
        val candidate = validate(candidateDsl)
        return report(inputs.map { diffOne(candidate, it) })
    }

    private fun validate(candidateDsl: String): DecisionRule {
        if (candidateDsl.length > properties.maxDslChars) throw DslTooLargeException(properties.maxDslChars)
        return try {
            ruleParser.parse(candidateDsl)
        } catch (ex: DecisionDslException) {
            throw InvalidRuleDslException(ex.code, ex.message, ex)
        }
    }

    private fun diffOne(
        candidate: DecisionRule,
        attributes: Map<String, JsonNode>,
    ): ReplayDiff {
        val input = inputMapper.toEvaluationInput(attributes)
        val hash = inputHasher.hash(input)
        val recorded = decisionLogRepository.findFirstByInputHashOrderByEvaluatedAtDesc(hash)
        val result = boundedEvaluator.evaluate(candidate, input)
        val candidateLabel = result.outcome?.label
        return ReplayDiff(
            hash,
            recorded?.matched,
            recorded?.outcomeLabel,
            result.matched,
            candidateLabel,
            statusOf(recorded, result.matched, candidateLabel),
        )
    }

    private fun statusOf(
        recorded: DecisionLogEntity?,
        candidateMatched: Boolean,
        candidateLabel: String?,
    ): DiffStatus =
        when {
            recorded == null -> DiffStatus.NO_BASELINE
            recorded.matched == candidateMatched && recorded.outcomeLabel == candidateLabel -> DiffStatus.UNCHANGED
            else -> DiffStatus.CHANGED
        }

    private fun report(diffs: List<ReplayDiff>): ReplayReport =
        ReplayReport(
            total = diffs.size,
            unchanged = diffs.count { it.status == DiffStatus.UNCHANGED },
            changed = diffs.count { it.status == DiffStatus.CHANGED },
            noBaseline = diffs.count { it.status == DiffStatus.NO_BASELINE },
            diffs = diffs,
        )
}
