// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.idempotency

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.core.IdempotencyKey
import com.fincore.ledger.api.error.ProblemType
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.net.URI

@Component
class IdempotencyFilter(
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.method != HttpMethod.POST.name() || !isGuarded(request.requestURI)

    private fun isGuarded(uri: String): Boolean = uri in GUARDED_PATHS || REVERSE_PATH.matches(uri)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader(IdempotencyAttributes.HEADER)
        if (header.isNullOrBlank()) {
            writeBadRequest(request, response, "Idempotency-Key header is required")
            return
        }
        try {
            IdempotencyKey.of(header)
        } catch (ex: IllegalArgumentException) {
            writeBadRequest(request, response, ex.message)
            return
        }
        val cached = CachedBodyHttpServletRequest(request)
        cached.setAttribute(IdempotencyAttributes.KEY, header)
        cached.setAttribute(IdempotencyAttributes.BODY, String(cached.body(), Charsets.UTF_8))
        filterChain.doFilter(cached, response)
    }

    private fun writeBadRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
        detail: String?,
    ) {
        val type = ProblemType.INVALID_REQUEST
        val problem = ProblemDetail.forStatusAndDetail(type.status, detail ?: type.title)
        problem.title = type.title
        problem.type = type.type
        problem.instance = URI.create(request.requestURI)
        problem.setProperty("code", type.code)
        response.status = type.status.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        objectMapper.writeValue(response.writer, problem)
    }

    private companion object {
        val GUARDED_PATHS = setOf("/v1/accounts", "/v1/transactions")
        val REVERSE_PATH = Regex("/v1/transactions/[^/]+/reverse")
    }
}
