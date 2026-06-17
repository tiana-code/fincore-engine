// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api

import com.fincore.decision.store.api.mapper.DecisionApiMapper
import com.fincore.decision.store.application.DecisionLogService
import com.fincore.decision.store.application.DecisionLogView
import com.fincore.decision.store.config.SecurityConfig
import com.fincore.decision.store.exception.GlobalExceptionHandler
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

@WebMvcTest(DecisionLogController::class)
@Import(SecurityConfig::class, DecisionApiMapper::class, GlobalExceptionHandler::class, DecisionLogControllerTest.Mocks::class)
class DecisionLogControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val decisionLogService: DecisionLogService,
) {
    @TestConfiguration
    class Mocks {
        @Bean fun decisionLogService(): DecisionLogService = mockk()

        @Bean fun jwtDecoder(): JwtDecoder = mockk()
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(decisionLogService)
    }

    @Test
    fun `should return logs filtered by input hash with the read scope`() {
        val hash = "a".repeat(HASH_LENGTH)
        every { decisionLogService.byInputHash(hash) } returns listOf(log(hash))

        mockMvc
            .perform(get("/v1/decision/logs").param("inputHash", hash).with(jwt().authorities(SimpleGrantedAuthority(SCOPE_READ))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].inputHash").value(hash))
    }

    @Test
    fun `should return logs filtered by rule version id with the read scope`() {
        val versionId = UUID.randomUUID()
        every { decisionLogService.byRuleVersionId(versionId) } returns listOf(log("b".repeat(HASH_LENGTH)))

        mockMvc
            .perform(
                get("/v1/decision/logs")
                    .param("ruleVersionId", versionId.toString())
                    .with(jwt().authorities(SimpleGrantedAuthority(SCOPE_READ))),
            ).andExpect(status().isOk)
    }

    @Test
    fun `should reject a logs request with no filter as a 400 problem document`() {
        mockMvc
            .perform(get("/v1/decision/logs").with(jwt().authorities(SimpleGrantedAuthority(SCOPE_READ))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }

    @Test
    fun `should reject an unauthenticated logs request with 401`() {
        mockMvc.perform(get("/v1/decision/logs").param("inputHash", "x")).andExpect(status().isUnauthorized)
    }

    private fun log(hash: String): DecisionLogView =
        DecisionLogView(
            id = UUID.randomUUID(),
            evaluatedAt = Instant.now(),
            ruleVersionId = UUID.randomUUID(),
            inputHash = hash,
            matched = false,
            outcomeLabel = null,
        )

    private companion object {
        const val SCOPE_READ = "SCOPE_decision:read"
        const val HASH_LENGTH = 64
    }
}
