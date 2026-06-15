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
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateAccountRequest,
        @AuthenticationPrincipal jwt: Jwt,
        @RequestAttribute(IdempotencyAttributes.KEY) key: String,
        @RequestAttribute(IdempotencyAttributes.BODY) rawBody: String,
    ): ResponseEntity<String> {
        var location: URI? = null
        val result =
            idempotencyService.execute(IdempotencyKey.of(key), rawBody) {
                val response = mapper.toResponse(accountService.create(mapper.toCommand(request, jwt.subject)))
                location = URI.create("/v1/accounts/${response.id}")
                StoredResponse(HttpStatus.CREATED.value(), objectMapper.writeValueAsString(response))
            }
        return respond(result, location)
    }

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<AccountResponse> {
        require(page >= 0) { "page must be >= 0" }
        require(size in 1..MAX_PAGE_SIZE) { "size must be 1..$MAX_PAGE_SIZE" }
        return mapper.toPageResponse(accountService.list(page, size))
    }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: String,
    ): AccountResponse = mapper.toResponse(accountService.get(AccountId.fromString(id)))

    @GetMapping("/{id}/balance")
    fun balance(
        @PathVariable id: String,
    ): BalanceResponse {
        val accountId = AccountId.fromString(id)
        val account = accountService.get(accountId)
        return mapper.toResponse(balanceService.current(accountId, account.currency))
    }

    @GetMapping("/{id}/entries")
    fun entries(
        @PathVariable id: String,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "50") limit: Int,
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
    }
}
