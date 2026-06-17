// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.config

import com.fincore.decision.eval.RuleEvaluator
import com.fincore.decision.parser.RuleParser
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

@Configuration
@EnableConfigurationProperties(DecisionApiProperties::class)
class DecisionEngineConfig {
    @Bean
    fun ruleParser(): RuleParser = RuleParser()

    @Bean
    fun ruleEvaluator(): RuleEvaluator = RuleEvaluator()

    @Bean(destroyMethod = "shutdownNow")
    fun evaluationWatchdog(): ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "decision-eval-watchdog").apply { isDaemon = true }
        }
}
