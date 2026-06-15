// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.observability

import com.fincore.ledger.api.observability.CorrelationIdAttributes.HEADER
import com.fincore.ledger.api.observability.CorrelationIdAttributes.MDC_KEY
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.UUID

class CorrelationIdFilterTest {
    private val filter = CorrelationIdFilter()

    @AfterEach
    fun clearMdc() = MDC.clear()

    @Test
    fun `should echo a valid inbound correlation id on the response`() {
        val inbound = "123e4567-e89b-12d3-a456-426614174000"
        val response = MockHttpServletResponse()
        filter.doFilter(requestWith(inbound), response, MockFilterChain())
        response.getHeader(HEADER) shouldBe inbound
    }

    @Test
    fun `should generate a valid uuid when no inbound id is present`() {
        val response = MockHttpServletResponse()
        filter.doFilter(MockHttpServletRequest(), response, MockFilterChain())
        val header = response.getHeader(HEADER)
        header.shouldNotBeNull()
        shouldNotThrowAny { UUID.fromString(header) }
    }

    @Test
    fun `should replace an invalid inbound id without rejecting the request`() {
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(requestWith("not-a-uuid"), response, chain)
        val header = response.getHeader(HEADER)
        header shouldNotBe "not-a-uuid"
        shouldNotThrowAny { UUID.fromString(header) }
        chain.request.shouldNotBeNull()
        response.status shouldBe 200
    }

    @Test
    fun `should expose the correlation id in the MDC during the chain`() {
        val response = MockHttpServletResponse()
        val captor = MdcCapturingChain()
        filter.doFilter(MockHttpServletRequest(), response, captor)
        captor.observed shouldBe response.getHeader(HEADER)
    }

    @Test
    fun `should clear the MDC after the request completes`() {
        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), MockFilterChain())
        MDC.get(MDC_KEY).shouldBeNull()
    }

    @Test
    fun `should clear the MDC even when the chain throws`() {
        val boom = FilterChain { _, _ -> throw IllegalStateException("boom") }
        shouldThrow<IllegalStateException> {
            filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), boom)
        }
        MDC.get(MDC_KEY).shouldBeNull()
    }

    private fun requestWith(correlationId: String): MockHttpServletRequest =
        MockHttpServletRequest().apply { addHeader(HEADER, correlationId) }

    private class MdcCapturingChain : FilterChain {
        var observed: String? = null

        override fun doFilter(
            request: ServletRequest,
            response: ServletResponse,
        ) {
            observed = MDC.get(MDC_KEY)
        }
    }
}
