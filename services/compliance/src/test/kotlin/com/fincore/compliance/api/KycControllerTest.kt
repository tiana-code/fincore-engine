// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.api

import com.fincore.compliance.api.mapper.KycApiMapper
import com.fincore.compliance.application.kyc.KycService
import com.fincore.compliance.application.kyc.KycSessionNotFoundException
import com.fincore.compliance.config.SecurityConfig
import com.fincore.compliance.domain.KycSession
import com.fincore.compliance.exception.GlobalExceptionHandler
import com.fincore.core.KycSessionId
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(KycController::class)
@Import(SecurityConfig::class, KycApiMapper::class, GlobalExceptionHandler::class, KycControllerTest.Mocks::class)
class KycControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val kycService: KycService,
) {
    @TestConfiguration
    class Mocks {
        @Bean fun kycService(): KycService = mockk()

        @Bean fun jwtDecoder(): JwtDecoder = mockk()
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(kycService)
    }

    @Test
    fun `should initiate and return 201 when authorized with write scope`() {
        every { kycService.initiate(any()) } returns session()

        mockMvc
            .perform(
                post("/v1/kyc/sessions")
                    .with(jwt().authorities(SimpleGrantedAuthority(WRITE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("INITIATED"))
    }

    @Test
    fun `should return 401 when initiating without a token`() {
        mockMvc
            .perform(post("/v1/kyc/sessions").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 403 when initiating with only the read scope`() {
        mockMvc
            .perform(
                post("/v1/kyc/sessions")
                    .with(jwt().authorities(SimpleGrantedAuthority(READ)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should return 400 when the body is invalid`() {
        mockMvc
            .perform(
                post("/v1/kyc/sessions")
                    .with(jwt().authorities(SimpleGrantedAuthority(WRITE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"subjectReference":""}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return the session when getting with read scope`() {
        val session = session()
        every { kycService.get(session.id) } returns session

        mockMvc
            .perform(get("/v1/kyc/sessions/${session.id}").with(jwt().authorities(SimpleGrantedAuthority(READ))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(session.id.toString()))
    }

    @Test
    fun `should return 404 when getting an unknown session`() {
        every { kycService.get(any()) } throws KycSessionNotFoundException(KycSessionId.generate())

        mockMvc
            .perform(get("/v1/kyc/sessions/${KycSessionId.generate()}").with(jwt().authorities(SimpleGrantedAuthority(READ))))
            .andExpect(status().isNotFound)
    }

    private fun session(): KycSession = KycSession(KycSessionId.generate(), "subject-1")

    private companion object {
        const val READ = "SCOPE_compliance:read"
        const val WRITE = "SCOPE_compliance:write"
        const val VALID_BODY = """{"subjectReference":"subject-1"}"""
    }
}
