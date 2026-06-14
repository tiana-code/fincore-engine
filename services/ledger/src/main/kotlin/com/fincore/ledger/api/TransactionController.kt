// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.core.IdempotencyKey
import com.fincore.ledger.api.dto.request.PostTransactionRequest
import com.fincore.ledger.api.dto.response.PageResponse
import com.fincore.ledger.api.dto.response.TransactionResponse
import com.fincore.ledger.api.idempotency.IdempotencyAttributes
import com.fincore.ledger.api.mapper.LedgerApiMapper
import com.fincore.ledger.application.IdempotencyService
import com.fincore.ledger.application.IdempotentResult
import com.fincore.ledger.application.StoredResponse
import com.fincore.ledger.application.TransactionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/v1/transactions")
class TransactionController(
    private val transactionService: TransactionService,
    private val idempotencyService: IdempotencyService,
    private val mapper: LedgerApiMapper,
    private val objectMapper: ObjectMapper,
) {
    @PostMapping
    fun post(
        @Valid @RequestBody request: PostTransactionRequest,
        @AuthenticationPrincipal jwt: Jwt,
        @RequestHeader(value = CORRELATION_HEADER, required = false) correlationId: String?,
        @RequestAttribute(IdempotencyAttributes.KEY) key: String,
        @RequestAttribute(IdempotencyAttributes.BODY) rawBody: String,
    ): ResponseEntity<String> {
        var location: URI? = null
        val result =
            idempotencyService.execute(IdempotencyKey.of(key), rawBody) {
                val response = mapper.toResponse(transactionService.post(mapper.toCommand(request, jwt.subject, correlationId)))
                location = URI.create("/v1/transactions/${response.id}")
                StoredResponse(HttpStatus.CREATED.value(), objectMapper.writeValueAsString(response))
            }
        return respond(result, location)
    }

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<TransactionResponse> {
        require(page >= 0) { "page must be >= 0" }
        require(size in 1..MAX_PAGE_SIZE) { "size must be 1..$MAX_PAGE_SIZE" }
        return mapper.toPageResponse(transactionService.list(page, size))
    }

    private fun respond(
        result: IdempotentResult,
        location: URI?,
    ): ResponseEntity<String> {
        val status = requireNotNull(result.statusCode) { "idempotent result missing status" }
        val body = requireNotNull(result.responseBody) { "idempotent result missing body" }
        val builder = ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
        if (!result.replayed) location?.let { builder.location(it) }
        return builder.body(body)
    }

    private companion object {
        const val CORRELATION_HEADER = "X-Correlation-Id"
        const val MAX_PAGE_SIZE = 100
    }
}
