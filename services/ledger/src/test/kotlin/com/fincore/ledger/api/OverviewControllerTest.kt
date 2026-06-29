// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fincore.ledger.api.error.AuditEndpointResolver
import com.fincore.ledger.api.idempotency.IdempotencyFilter
import com.fincore.ledger.api.mapper.LedgerApiMapper
import com.fincore.ledger.application.ActivityItem
import com.fincore.ledger.application.AuditTrailWriter
import com.fincore.ledger.application.OverviewService
import com.fincore.ledger.application.OverviewSnapshot
import com.fincore.ledger.config.AuditingAccessDeniedHandler
import com.fincore.ledger.config.SecurityConfig
import com.fincore.ledger.domain.enum.ActivityType
import com.fincore.ledger.exception.FailureAuditRecorder
import io.mockk.every
import io.mockk.mockk
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
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@WebMvcTest(OverviewController::class)
@Import(
    SecurityConfig::class,
    LedgerApiMapper::class,
    IdempotencyFilter::class,
    AuditEndpointResolver::class,
    FailureAuditRecorder::class,
    AuditingAccessDeniedHandler::class,
    OverviewControllerTest.Mocks::class,
)
class OverviewControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val overviewService: OverviewService,
) {
    @TestConfiguration
    class Mocks {
        @Bean fun overviewService(): OverviewService = mockk()

        @Bean fun auditTrailWriter(): AuditTrailWriter = mockk(relaxed = true)

        @Bean fun jwtDecoder(): JwtDecoder = mockk()
    }

    private val postedAt = Instant.parse("2026-06-28T10:01:02Z")

    private fun emptySnapshot() =
        OverviewSnapshot(
            activity = emptyList(),
            transactionsLast24h = List(24) { 0 },
        )

    private fun snapshotWithActivity(): OverviewSnapshot {
        val txItem =
            ActivityItem(
                type = ActivityType.TRANSACTION_POSTED,
                resourceId = UUID.fromString("018f7e2a-0000-7000-8000-000000000001"),
                label = "wallet top-up",
                amount = BigDecimal("500.000000000000000000"),
                currency = "USD",
                occurredAt = postedAt,
            )
        val sparkline = List(24) { if (it == 23) 1 else 0 }
        return OverviewSnapshot(activity = listOf(txItem), transactionsLast24h = sparkline)
    }

    @Test
    fun `should return 401 when unauthenticated`() {
        mockMvc.perform(get("/v1/overview")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 403 when authenticated with write scope only`() {
        every { overviewService.overview() } returns emptySnapshot()

        mockMvc
            .perform(get("/v1/overview").with(jwt().authorities(SimpleGrantedAuthority(SCOPE_WRITE))))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should return 200 with read scope`() {
        every { overviewService.overview() } returns emptySnapshot()

        mockMvc
            .perform(get("/v1/overview").with(jwt().authorities(SimpleGrantedAuthority(SCOPE_READ))))
            .andExpect(status().isOk)
    }

    @Test
    fun `should return transactionsLast24h as an array of exactly 24 integers`() {
        every { overviewService.overview() } returns emptySnapshot()

        mockMvc
            .perform(get("/v1/overview").with(readJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.transactionsLast24h").isArray)
            .andExpect(jsonPath("$.transactionsLast24h.length()").value(24))
    }

    @Test
    fun `should return empty activity array on empty ledger`() {
        every { overviewService.overview() } returns emptySnapshot()

        mockMvc
            .perform(get("/v1/overview").with(readJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.activity").isArray)
            .andExpect(jsonPath("$.activity.length()").value(0))
    }

    @Test
    fun `should serialize amount as a string not a number`() {
        every { overviewService.overview() } returns snapshotWithActivity()

        mockMvc
            .perform(get("/v1/overview").with(readJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.activity[0].amount").isString)
    }

    @Test
    fun `should emit dotted type string for transaction posted`() {
        every { overviewService.overview() } returns snapshotWithActivity()

        mockMvc
            .perform(get("/v1/overview").with(readJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.activity[0].type").value("transaction.posted"))
    }

    @Test
    fun `should prefix transaction resourceId with tx_`() {
        every { overviewService.overview() } returns snapshotWithActivity()

        mockMvc
            .perform(get("/v1/overview").with(readJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.activity[0].resourceId").value(org.hamcrest.Matchers.startsWith("tx_")))
    }

    @Test
    fun `should include label and currency on activity items`() {
        every { overviewService.overview() } returns snapshotWithActivity()

        mockMvc
            .perform(get("/v1/overview").with(readJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.activity[0].label").value("wallet top-up"))
            .andExpect(jsonPath("$.activity[0].currency").value("USD"))
            .andExpect(jsonPath("$.activity[0].occurredAt").value(postedAt.toString()))
    }

    private fun readJwt() = jwt().authorities(SimpleGrantedAuthority(SCOPE_READ))

    private companion object {
        const val SCOPE_READ = "SCOPE_ledger:read"
        const val SCOPE_WRITE = "SCOPE_ledger:write"
    }
}
