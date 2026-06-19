// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.api

import com.fincore.compliance.application.kyc.KycCheckRequest
import com.fincore.compliance.application.kyc.KycCheckResult
import com.fincore.compliance.application.kyc.KycProvider
import com.fincore.test.containers.PostgresContainerExtension
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(PostgresContainerExtension::class)
@Import(KycApiContextIT.TestBeans::class)
class KycApiContextIT(
    @Autowired private val mockMvc: MockMvc,
) {
    @TestConfiguration
    class TestBeans {
        @Bean fun jwtDecoder(): JwtDecoder = mockk()

        // No in-tree KycProvider yet (sandbox is #239); a fake satisfies the orchestrator's dependency so the
        // full web context boots.
        @Bean fun kycProvider(): KycProvider =
            object : KycProvider {
                override fun check(request: KycCheckRequest): KycCheckResult = KycCheckResult.Pending("ref-test")
            }
    }

    @Test
    fun `should serve the openapi document listing the kyc operations`() {
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.paths['/v1/kyc/sessions']").exists())
    }

    @Test
    fun `should initiate then get a session end to end through the web stack`() {
        val location =
            mockMvc
                .perform(
                    post("/v1/kyc/sessions")
                        .with(jwt().authorities(SimpleGrantedAuthority(WRITE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"subjectReference":"subject-1"}"""),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.status").value("INITIATED"))
                .andReturn()
                .response
                .getHeader("Location")

        mockMvc
            .perform(get(location!!).with(jwt().authorities(SimpleGrantedAuthority(READ))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.subjectReference").value("subject-1"))
    }

    companion object {
        private const val READ = "SCOPE_compliance:read"
        private const val WRITE = "SCOPE_compliance:write"

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { PostgresContainerExtension.jdbcUrl }
            registry.add("spring.datasource.username") { PostgresContainerExtension.username }
            registry.add("spring.datasource.password") { PostgresContainerExtension.password }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") { "https://issuer.test" }
            // This context does not exercise the AML consumer; keep the Kafka listener from polling an absent broker.
            registry.add("spring.kafka.listener.auto-startup") { "false" }
        }
    }
}
