// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.api

import com.fincore.compliance.application.kyc.KycCheckRequest
import com.fincore.compliance.application.kyc.KycCheckResult
import com.fincore.compliance.application.kyc.KycProvider
import com.fincore.compliance.infrastructure.persistence.ComplianceCaseRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
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
@Import(ComplianceLifecycleIT.TestBeans::class)
class ComplianceLifecycleIT(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val cases: ComplianceCaseRepository,
) {
    @TestConfiguration
    class TestBeans {
        @Bean fun jwtDecoder(): JwtDecoder = mockk()

        @Bean fun kycProvider(): KycProvider =
            object : KycProvider {
                override fun check(request: KycCheckRequest): KycCheckResult = KycCheckResult.Pending("ref-test")
            }
    }

    @AfterEach
    fun cleanUp() {
        cases.deleteAll()
    }

    @Test
    fun `should drive a case from open through claim to resolved over http`() {
        val location = openCase()

        mockMvc
            .perform(post("$location/claim").with(jwt().authorities(SimpleGrantedAuthority(WRITE))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CLAIMED"))

        mockMvc
            .perform(post("$location/resolve").with(jwt().authorities(SimpleGrantedAuthority(WRITE))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("RESOLVED"))

        mockMvc
            .perform(get(location).with(jwt().authorities(SimpleGrantedAuthority(READ))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("RESOLVED"))
    }

    @Test
    fun `should return 409 when resolving a case twice`() {
        val location = openCase()
        mockMvc
            .perform(post("$location/claim").with(jwt().authorities(SimpleGrantedAuthority(WRITE))))
            .andExpect(status().isOk)
        mockMvc
            .perform(post("$location/resolve").with(jwt().authorities(SimpleGrantedAuthority(WRITE))))
            .andExpect(status().isOk)

        mockMvc
            .perform(post("$location/resolve").with(jwt().authorities(SimpleGrantedAuthority(WRITE))))
            .andExpect(status().isConflict)
    }

    @Test
    fun `should return 403 when opening a case with only the read scope`() {
        mockMvc
            .perform(
                post("/v1/compliance/cases")
                    .with(jwt().authorities(SimpleGrantedAuthority(READ)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should publish both kyc and compliance case paths in the openapi document`() {
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.paths['/v1/kyc/sessions']").exists())
            .andExpect(jsonPath("$.paths['/v1/compliance/cases']").exists())
    }

    @Test
    fun `should expose the health endpoint without a token`() {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk)
    }

    private fun openCase(): String =
        mockMvc
            .perform(
                post("/v1/compliance/cases")
                    .with(jwt().authorities(SimpleGrantedAuthority(WRITE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY),
            ).andExpect(status().isCreated)
            .andReturn()
            .response
            .getHeader("Location")!!

    companion object {
        private const val READ = "SCOPE_compliance:read"
        private const val WRITE = "SCOPE_compliance:write"
        private const val VALID_BODY = """{"reference":"case-ref-1"}"""

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
