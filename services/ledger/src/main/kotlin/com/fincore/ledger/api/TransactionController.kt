// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.core.IdempotencyKey
import com.fincore.core.TransactionId
import com.fincore.ledger.api.dto.request.PostTransactionRequest
import com.fincore.ledger.api.dto.request.ReverseTransactionRequest
import com.fincore.ledger.api.dto.response.PageResponse
import com.fincore.ledger.api.dto.response.TransactionDetailResponse
import com.fincore.ledger.api.dto.response.TransactionResponse
import com.fincore.ledger.api.idempotency.IdempotencyAttributes
import com.fincore.ledger.api.mapper.LedgerApiMapper
import com.fincore.ledger.application.IdempotencyService
import com.fincore.ledger.application.IdempotentResult
import com.fincore.ledger.application.StoredResponse
import com.fincore.ledger.application.TransactionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@Tag(name = "Transactions", description = "Double-entry transaction posting, lookup, and reversal")
@RestController
@RequestMapping("/v1/transactions")
class TransactionController(
    private val transactionService: TransactionService,
    private val idempotencyService: IdempotencyService,
    private val mapper: LedgerApiMapper,
    private val objectMapper: ObjectMapper,
) {
    @Operation(
        summary = "Post a transaction",
        description = "Posts a balanced double-entry transaction. Requires an Idempotency-Key; entries net to zero in one currency.",
        parameters = [
            Parameter(
                `in` = ParameterIn.HEADER,
                name = "Idempotency-Key",
                required = true,
                description = "Idempotency key, 32-128 chars matching ^[A-Za-z0-9_-]+$",
            ),
        ],
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Transaction posted"),
        ApiResponse(
            responseCode = "400",
            description = "Invalid body or missing Idempotency-Key",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "Referenced account not found",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "Duplicate reference or idempotency conflict",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "422",
            description = "Double-entry or currency consistency violation",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @PostMapping
    fun post(
        @Valid @RequestBody request: PostTransactionRequest,
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestHeader(value = CORRELATION_HEADER, required = false) correlationId: String?,
        @Parameter(hidden = true) @RequestAttribute(IdempotencyAttributes.KEY) key: String,
        @Parameter(hidden = true) @RequestAttribute(IdempotencyAttributes.BODY) rawBody: String,
    ): ResponseEntity<String> {
        var location: URI? = null
        val result =
            idempotencyService.execute(IdempotencyKey.of(key), rawBody) { hash ->
                val response = mapper.toResponse(transactionService.post(mapper.toCommand(request, jwt.subject, correlationId, hash)))
                location = URI.create("/v1/transactions/${response.id}")
                StoredResponse(HttpStatus.CREATED.value(), objectMapper.writeValueAsString(response))
            }
        return respond(result, location)
    }

    @Operation(summary = "List transactions", description = "Returns a page of transactions, newest first.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page of transactions"),
        ApiResponse(
            responseCode = "400",
            description = "Invalid page or size",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @GetMapping
    fun list(
        @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size, 1..100") @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<TransactionResponse> {
        require(page >= 0) { "page must be >= 0" }
        require(size in 1..MAX_PAGE_SIZE) { "size must be 1..$MAX_PAGE_SIZE" }
        return mapper.toPageResponse(transactionService.list(page, size))
    }

    @Operation(summary = "Get a transaction", description = "Returns a transaction with its entries by id.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "The transaction with entries"),
        ApiResponse(
            responseCode = "400",
            description = "Malformed transaction id",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "Transaction not found",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @GetMapping("/{id}")
    fun get(
        @Parameter(description = "Transaction id (tx_ prefixed ULID)") @PathVariable id: String,
    ): TransactionDetailResponse = mapper.toDetailResponse(transactionService.get(TransactionId.fromString(id)))

    @Operation(
        summary = "Reverse a transaction",
        description = "Posts a compensating transaction that reverses an existing POSTED transaction. Requires an Idempotency-Key header.",
        parameters = [
            Parameter(
                `in` = ParameterIn.HEADER,
                name = "Idempotency-Key",
                required = true,
                description = "Idempotency key, 32-128 chars matching ^[A-Za-z0-9_-]+$",
            ),
        ],
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Compensating transaction posted"),
        ApiResponse(
            responseCode = "400",
            description = "Malformed id or missing Idempotency-Key",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "Transaction not found",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "Transaction already reversed",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @PostMapping("/{id}/reverse")
    fun reverse(
        @Parameter(description = "Transaction id (tx_ prefixed ULID)") @PathVariable id: String,
        @Valid @RequestBody(required = false) request: ReverseTransactionRequest?,
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestHeader(value = CORRELATION_HEADER, required = false) correlationId: String?,
        @Parameter(hidden = true) @RequestAttribute(IdempotencyAttributes.KEY) key: String,
        @Parameter(hidden = true) @RequestAttribute(IdempotencyAttributes.BODY) rawBody: String,
    ): ResponseEntity<String> {
        val transactionId = TransactionId.fromString(id)
        var location: URI? = null
        val result =
            idempotencyService.execute(IdempotencyKey.of(key), rawBody) { hash ->
                val response =
                    mapper.toResponse(
                        transactionService.reverse(transactionId, jwt.subject, correlationId, request?.reason, hash),
                    )
                location = URI.create("/v1/transactions/${response.id}")
                StoredResponse(HttpStatus.CREATED.value(), objectMapper.writeValueAsString(response))
            }
        return respond(result, location)
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
        const val PROBLEM_JSON = "application/problem+json"
    }
}
