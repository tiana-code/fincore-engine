// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it
                    .requestMatchers(*PUBLIC_PATHS)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, DECISION_PATHS)
                    .hasAuthority(SCOPE_READ)
                    .requestMatchers(DECISION_PATHS)
                    .hasAuthority(SCOPE_WRITE)
                    .anyRequest()
                    .authenticated()
            }.oauth2ResourceServer { resource ->
                resource.jwt { it.jwtAuthenticationConverter(jwtAuthenticationConverter()) }
            }.build()

    private fun jwtAuthenticationConverter(): JwtAuthenticationConverter =
        JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(JwtGrantedAuthoritiesConverter())
        }

    private companion object {
        const val DECISION_PATHS = "/v1/**"
        const val SCOPE_READ = "SCOPE_decision:read"
        const val SCOPE_WRITE = "SCOPE_decision:write"
        val PUBLIC_PATHS =
            arrayOf(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/actuator/health",
                "/actuator/health/**",
                "/actuator/prometheus",
            )
    }
}
