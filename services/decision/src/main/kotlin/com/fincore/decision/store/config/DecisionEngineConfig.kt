// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.config

import com.fincore.decision.parser.RuleParser
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(DecisionApiProperties::class)
class DecisionEngineConfig {
    @Bean
    fun ruleParser(): RuleParser = RuleParser()
}
