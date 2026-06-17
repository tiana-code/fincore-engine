// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import com.fincore.decision.domain.DecisionResult
import com.fincore.decision.domain.DecisionRule
import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.eval.RegexMatchInterruptedException
import com.fincore.decision.eval.RuleEvaluator
import com.fincore.decision.store.config.DecisionApiProperties
import com.fincore.decision.store.exception.EvaluationTimeoutException
import org.springframework.stereotype.Component
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Runs the pure evaluator under a hard time budget. A watchdog interrupts the calling thread after the
 * budget; the engine matches author patterns against an interruptible view of the input, so a catastrophic
 * backtrack unwinds promptly as a [RegexMatchInterruptedException] instead of hanging. This wraps ONLY the
 * evaluate call: the watchdog is cancelled and the interrupt flag cleared before returning, so a caller can
 * safely proceed (e.g. to persist an audit row) on a clean thread.
 */
@Component
class BoundedEvaluator(
    private val ruleEvaluator: RuleEvaluator,
    private val watchdog: ScheduledExecutorService,
    private val properties: DecisionApiProperties,
) {
    fun evaluate(
        rule: DecisionRule,
        input: EvaluationInput,
    ): DecisionResult {
        val current = Thread.currentThread()
        val timer = watchdog.schedule({ current.interrupt() }, properties.evaluationTimeoutMillis, TimeUnit.MILLISECONDS)
        try {
            return ruleEvaluator.evaluate(rule, input)
        } catch (ex: RegexMatchInterruptedException) {
            throw EvaluationTimeoutException(properties.evaluationTimeoutMillis, ex)
        } finally {
            timer.cancel(false)
            Thread.interrupted()
        }
    }
}
