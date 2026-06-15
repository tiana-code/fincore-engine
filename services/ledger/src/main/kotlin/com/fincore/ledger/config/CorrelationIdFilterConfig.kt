// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.config

import com.fincore.ledger.api.observability.CorrelationIdFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
class CorrelationIdFilterConfig {
    @Bean
    fun correlationIdFilterRegistration(): FilterRegistrationBean<CorrelationIdFilter> =
        FilterRegistrationBean(CorrelationIdFilter()).apply {
            order = Ordered.HIGHEST_PRECEDENCE
            addUrlPatterns("/*")
        }
}
