// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.config

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
    // Order matters (first match wins): public paths, then the GET read rule before the catch-all write rule.
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it
                    .requestMatchers(*PUBLIC_PATHS)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, KYC_PATHS)
                    .hasAuthority(SCOPE_READ)
                    .requestMatchers(KYC_PATHS)
                    .hasAuthority(SCOPE_WRITE)
                    .requestMatchers(HttpMethod.GET, COMPLIANCE_PATHS)
                    .hasAuthority(SCOPE_READ)
                    .requestMatchers(COMPLIANCE_PATHS)
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
        const val KYC_PATHS = "/v1/kyc/**"
        const val COMPLIANCE_PATHS = "/v1/compliance/**"
        const val SCOPE_READ = "SCOPE_compliance:read"
        const val SCOPE_WRITE = "SCOPE_compliance:write"
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
