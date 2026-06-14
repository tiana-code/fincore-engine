// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.mapper

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.ledger.api.dto.request.CreateAccountRequest
import com.fincore.ledger.api.dto.request.PostTransactionRequest
import com.fincore.ledger.api.dto.response.AccountResponse
import com.fincore.ledger.api.dto.response.BalanceResponse
import com.fincore.ledger.api.dto.response.PageResponse
import com.fincore.ledger.api.dto.response.TransactionResponse
import com.fincore.ledger.application.AccountBalance
import com.fincore.ledger.application.AccountPage
import com.fincore.ledger.application.CreateAccountCommand
import com.fincore.ledger.application.EntryLine
import com.fincore.ledger.application.PostTransactionCommand
import com.fincore.ledger.application.PostedTransaction
import com.fincore.ledger.domain.Account
import org.springframework.stereotype.Component

// Hand-written, not MapStruct: the command/domain side uses Kotlin value classes (AccountId, Currency,
// Money) whose mangled getters and missing accessible constructors block MapStruct (issue #33 deferral).
@Component
class LedgerApiMapper {
    fun toCommand(
        request: CreateAccountRequest,
        actor: String,
    ): CreateAccountCommand =
        CreateAccountCommand(
            name = request.name,
            type = request.type,
            currency = Currency.of(request.currency),
            actor = actor,
        )

    fun toResponse(account: Account): AccountResponse =
        AccountResponse(
            id = account.id.toString(),
            name = account.name,
            type = account.type,
            currency = account.currency.code,
            status = account.status,
        )

    fun toPageResponse(page: AccountPage): PageResponse<AccountResponse> =
        PageResponse(
            items = page.items.map { toResponse(it) },
            page = page.page,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )

    fun toResponse(balance: AccountBalance): BalanceResponse =
        BalanceResponse(
            accountId = balance.accountId.toString(),
            currency = balance.amount.currency.code,
            amount = balance.amount.amount,
            lastPostedAt = balance.lastPostedAt,
        )

    fun toCommand(
        request: PostTransactionRequest,
        actor: String,
        correlationId: String?,
    ): PostTransactionCommand =
        PostTransactionCommand(
            reference = request.reference,
            description = request.description,
            currency = Currency.of(request.currency),
            entries =
                request.entries.map { line ->
                    EntryLine(
                        accountId = AccountId.fromString(line.accountId),
                        direction = line.direction,
                        amount = line.amount,
                    )
                },
            actor = actor,
            correlationId = correlationId,
        )

    fun toResponse(posted: PostedTransaction): TransactionResponse =
        TransactionResponse(
            id = posted.id.toString(),
            reference = posted.reference,
            postedAt = posted.postedAt,
        )
}
