// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.idempotency

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class IdempotencyFilterTest {
    private val filter = IdempotencyFilter(ObjectMapper())

    private fun request(
        method: String,
        uri: String,
        body: String = "{}",
    ): MockHttpServletRequest =
        MockHttpServletRequest(method, uri).apply {
            setContent(body.toByteArray())
            contentType = "application/json"
        }

    @Test
    fun `should not filter GET requests`() {
        val chain = MockFilterChain()
        filter.doFilter(request("GET", "/v1/accounts/acc_x"), MockHttpServletResponse(), chain)
        chain.request shouldNotBe null
    }

    @Test
    fun `should not filter unguarded POST paths`() {
        val chain = MockFilterChain()
        filter.doFilter(request("POST", "/v1/other"), MockHttpServletResponse(), chain)
        chain.request shouldNotBe null
    }

    @Test
    fun `should reject a guarded POST without the idempotency key header`() {
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(request("POST", "/v1/accounts"), response, chain)

        response.status shouldBe 400
        response.contentType shouldContain "application/problem+json"
        chain.request shouldBe null
    }

    @Test
    fun `should reject a syntactically invalid idempotency key`() {
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        val req = request("POST", "/v1/accounts").apply { addHeader(IdempotencyAttributes.HEADER, "short") }
        filter.doFilter(req, response, chain)

        response.status shouldBe 400
        chain.request shouldBe null
    }

    @Test
    fun `should expose the key and buffered body to the chain on a valid request`() {
        val body = """{"name":"Wallet"}"""
        val req =
            request("POST", "/v1/accounts", body).apply {
                addHeader(IdempotencyAttributes.HEADER, "k".repeat(40))
            }
        val chain = MockFilterChain()
        filter.doFilter(req, MockHttpServletResponse(), chain)

        val passed = chain.request!!
        passed.getAttribute(IdempotencyAttributes.KEY) shouldBe "k".repeat(40)
        passed.getAttribute(IdempotencyAttributes.BODY) shouldBe body
        passed.inputStream.readBytes().toString(Charsets.UTF_8) shouldBe body
    }
}
