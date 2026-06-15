// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.core.AccountId
import com.fincore.core.IdempotencyKey
import com.fincore.ledger.api.dto.request.CreateAccountRequest
import com.fincore.ledger.api.dto.response.AccountResponse
import com.fincore.ledger.api.dto.response.BalanceResponse
import com.fincore.ledger.api.dto.response.EntryPageResponse
import com.fincore.ledger.api.dto.response.PageResponse
import com.fincore.ledger.api.idempotency.IdempotencyAttributes
import com.fincore.ledger.api.mapper.LedgerApiMapper
import com.fincore.ledger.application.AccountService
import com.fincore.ledger.application.BalanceService
import com.fincore.ledger.application.EntryQueryService
import com.fincore.ledger.application.IdempotencyService
import com.fincore.ledger.application.IdempotentResult
import com.fincore.ledger.application.StoredResponse
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.Instant

@Tag(name = "Accounts", description = "Ledger account lifecycle, balances, and entries")
@RestController
@RequestMapping("/v1/accounts")
class AccountController(
    private val accountService: AccountService,
    private val balanceService: BalanceService,
    private val entryQueryService: EntryQueryService,
    private val idempotencyService: IdempotencyService,
    private val mapper: LedgerApiMapper,
    private val objectMapper: ObjectMapper,
) {
    @Operation(
        summary = "Create a ledger account",
        description = "Creates an account. Requires an Idempotency-Key; same key plus same body replays the original result.",
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
        ApiResponse(responseCode = "201", description = "Account created"),
        ApiResponse(
            responseCode = "400",
            description = "Invalid body or missing Idempotency-Key",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "401",
            description = "Missing or invalid bearer token",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "Idempotency-Key reused with a different body",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateAccountRequest,
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @Parameter(hidden = true) @RequestAttribute(IdempotencyAttributes.KEY) key: String,
        @Parameter(hidden = true) @RequestAttribute(IdempotencyAttributes.BODY) rawBody: String,
    ): ResponseEntity<String> {
        var location: URI? = null
        val result =
            idempotencyService.execute(IdempotencyKey.of(key), rawBody) { hash ->
                val response = mapper.toResponse(accountService.create(mapper.toCommand(request, jwt.subject, hash)))
                location = URI.create("/v1/accounts/${response.id}")
                StoredResponse(HttpStatus.CREATED.value(), objectMapper.writeValueAsString(response))
            }
        return respond(result, location)
    }

    @Operation(summary = "List accounts", description = "Returns a page of accounts, newest first.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page of accounts"),
        ApiResponse(
            responseCode = "400",
            description = "Invalid page or size",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "401",
            description = "Missing or invalid bearer token",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @GetMapping
    fun list(
        @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size, 1..100") @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<AccountResponse> {
        require(page >= 0) { "page must be >= 0" }
        require(size in 1..MAX_PAGE_SIZE) { "size must be 1..$MAX_PAGE_SIZE" }
        return mapper.toPageResponse(accountService.list(page, size))
    }

    @Operation(summary = "Get an account", description = "Returns a single account by id.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "The account"),
        ApiResponse(
            responseCode = "400",
            description = "Malformed account id",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "Account not found",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @GetMapping("/{id}")
    fun get(
        @Parameter(description = "Account id (acc_ prefixed ULID)") @PathVariable id: String,
    ): AccountResponse = mapper.toResponse(accountService.get(AccountId.fromString(id)))

    @Operation(summary = "Get an account balance", description = "Returns the current balance for an account in its currency.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "The current balance"),
        ApiResponse(
            responseCode = "404",
            description = "Account not found",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @GetMapping("/{id}/balance")
    fun balance(
        @Parameter(description = "Account id (acc_ prefixed ULID)") @PathVariable id: String,
    ): BalanceResponse {
        val accountId = AccountId.fromString(id)
        val account = accountService.get(accountId)
        return mapper.toResponse(balanceService.current(accountId, account.currency))
    }

    @Operation(
        summary = "List account entries",
        description = "Returns a page of ledger entries for an account, newest first, over an optional time window with cursor pagination.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page of entries"),
        ApiResponse(
            responseCode = "400",
            description = "Malformed id, timestamps, window, limit, or cursor",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "Account not found",
            content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @GetMapping("/{id}/entries")
    fun entries(
        @Parameter(description = "Account id (acc_ prefixed ULID)") @PathVariable id: String,
        @Parameter(description = "Inclusive window start, ISO-8601 instant") @RequestParam(required = false) from: String?,
        @Parameter(description = "Exclusive window end, ISO-8601 instant") @RequestParam(required = false) to: String?,
        @Parameter(description = "Opaque pagination cursor") @RequestParam(required = false) cursor: String?,
        @Parameter(description = "Page size, 1..200") @RequestParam(defaultValue = "50") limit: Int,
    ): EntryPageResponse {
        val page =
            entryQueryService.listAccountEntries(
                AccountId.fromString(id),
                parseInstant(from),
                parseInstant(to),
                cursor,
                limit,
            )
        return mapper.toPageResponse(page)
    }

    private fun parseInstant(raw: String?): Instant? =
        raw?.let { runCatching { Instant.parse(it) }.getOrElse { throw IllegalArgumentException("invalid timestamp: $raw") } }

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
        const val MAX_PAGE_SIZE = 100
        const val PROBLEM_JSON = "application/problem+json"
    }
}
