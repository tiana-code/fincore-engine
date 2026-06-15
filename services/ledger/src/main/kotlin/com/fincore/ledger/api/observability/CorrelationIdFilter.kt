// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.observability

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

class CorrelationIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val correlationId = resolve(request.getHeader(CorrelationIdAttributes.HEADER))
        MDC.put(CorrelationIdAttributes.MDC_KEY, correlationId)
        response.setHeader(CorrelationIdAttributes.HEADER, correlationId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(CorrelationIdAttributes.MDC_KEY)
        }
    }

    private fun resolve(inbound: String?): String =
        inbound?.takeUnless { it.isBlank() }?.let(::canonicalUuidOrNull) ?: UUID.randomUUID().toString()

    private fun canonicalUuidOrNull(value: String): String? = runCatching { UUID.fromString(value).toString() }.getOrNull()
}
