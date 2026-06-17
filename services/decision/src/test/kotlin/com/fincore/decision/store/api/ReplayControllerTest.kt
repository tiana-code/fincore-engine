// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api

import com.fincore.decision.domain.DslErrorCode
import com.fincore.decision.store.api.mapper.DecisionApiMapper
import com.fincore.decision.store.application.DiffStatus
import com.fincore.decision.store.application.ReplayDiff
import com.fincore.decision.store.application.ReplayReport
import com.fincore.decision.store.application.ReplayService
import com.fincore.decision.store.config.SecurityConfig
import com.fincore.decision.store.exception.GlobalExceptionHandler
import com.fincore.decision.store.exception.InvalidRuleDslException
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
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(ReplayController::class)
@Import(SecurityConfig::class, DecisionApiMapper::class, GlobalExceptionHandler::class, ReplayControllerTest.Mocks::class)
class ReplayControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val replayService: ReplayService,
) {
    private val candidate = """{"condition":{"attr":"amount","op":"gte","value":100},"outcome":{"label":"approve"}}"""
    private val body = """{"candidate":$candidate,"inputs":[{"amount":150}]}"""
    private val report = ReplayReport(1, 0, 1, 0, listOf(ReplayDiff("h", true, "approve", false, null, DiffStatus.CHANGED)))

    @TestConfiguration
    class Mocks {
        @Bean fun replayService(): ReplayService = mockk()

        @Bean fun jwtDecoder(): JwtDecoder = mockk()
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(replayService)
    }

    @Test
    fun `should reject an unauthenticated replay with 401`() {
        mockMvc
            .perform(post("/v1/decision/replay").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should reject a replay carrying only the read scope with 403`() {
        mockMvc
            .perform(
                post("/v1/decision/replay")
                    .with(jwt().authorities(SimpleGrantedAuthority(SCOPE_READ)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should return the diff report when the write scope is present`() {
        every { replayService.replay(any(), any()) } returns report

        mockMvc
            .perform(
                post("/v1/decision/replay")
                    .with(jwt().authorities(SimpleGrantedAuthority(SCOPE_WRITE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.changed").value(1))
            .andExpect(jsonPath("$.diffs[0].status").value("CHANGED"))
            .andExpect(jsonPath("$.diffs[0].recorded.matched").value(true))
            .andExpect(jsonPath("$.diffs[0].candidate.matched").value(false))
    }

    @Test
    fun `should reject a null input element with 400 rather than 500`() {
        mockMvc
            .perform(
                post("/v1/decision/replay")
                    .with(jwt().authorities(SimpleGrantedAuthority(SCOPE_WRITE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"candidate":$candidate,"inputs":[null]}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should reject a null candidate with 400 rather than 500`() {
        mockMvc
            .perform(
                post("/v1/decision/replay")
                    .with(jwt().authorities(SimpleGrantedAuthority(SCOPE_WRITE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"candidate":null,"inputs":[{"amount":1}]}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should map an invalid candidate to a 422 problem document`() {
        every { replayService.replay(any(), any()) } throws InvalidRuleDslException(DslErrorCode.UNKNOWN_OPERATOR, "unknown operator")

        mockMvc
            .perform(
                post("/v1/decision/replay")
                    .with(jwt().authorities(SimpleGrantedAuthority(SCOPE_WRITE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isUnprocessableEntity)
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("INVALID_DSL"))
    }

    private companion object {
        const val SCOPE_READ = "SCOPE_decision:read"
        const val SCOPE_WRITE = "SCOPE_decision:write"
        const val PROBLEM_JSON = "application/problem+json"
    }
}
