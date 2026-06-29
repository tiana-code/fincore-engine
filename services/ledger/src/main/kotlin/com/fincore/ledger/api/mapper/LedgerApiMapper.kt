// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.mapper

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.core.TransactionId
import com.fincore.ledger.api.dto.request.CreateAccountRequest
import com.fincore.ledger.api.dto.request.PostTransactionRequest
import com.fincore.ledger.api.dto.response.AccountEntryResponse
import com.fincore.ledger.api.dto.response.AccountResponse
import com.fincore.ledger.api.dto.response.BalanceResponse
import com.fincore.ledger.api.dto.response.EntryPageResponse
import com.fincore.ledger.api.dto.response.EntryResponse
import com.fincore.ledger.api.dto.response.OverviewActivityResponse
import com.fincore.ledger.api.dto.response.OverviewResponse
import com.fincore.ledger.api.dto.response.PageResponse
import com.fincore.ledger.api.dto.response.TransactionDetailResponse
import com.fincore.ledger.api.dto.response.TransactionResponse
import com.fincore.ledger.application.AccountBalance
import com.fincore.ledger.application.AccountEntry
import com.fincore.ledger.application.AccountEntryPage
import com.fincore.ledger.application.AccountPage
import com.fincore.ledger.application.ActivityItem
import com.fincore.ledger.application.CreateAccountCommand
import com.fincore.ledger.application.EntryLine
import com.fincore.ledger.application.EntryView
import com.fincore.ledger.application.OverviewSnapshot
import com.fincore.ledger.application.PostTransactionCommand
import com.fincore.ledger.application.PostedTransaction
import com.fincore.ledger.application.TransactionDetail
import com.fincore.ledger.application.TransactionPage
import com.fincore.ledger.application.TransactionSummary
import com.fincore.ledger.domain.Account
import com.fincore.ledger.domain.enum.ActivityType
import org.springframework.stereotype.Component

// Hand-written, not MapStruct: the command/domain side uses Kotlin value classes (AccountId, Currency,
// Money) whose mangled getters and missing accessible constructors block MapStruct (issue #33 deferral).
@Component
class LedgerApiMapper {
    fun toCommand(
        request: CreateAccountRequest,
        actor: String,
        requestHash: String?,
    ): CreateAccountCommand =
        CreateAccountCommand(
            name = request.name,
            type = request.type,
            currency = Currency.of(request.currency),
            actor = actor,
            requestHash = requestHash,
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
        requestHash: String?,
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
            requestHash = requestHash,
        )

    fun toResponse(posted: PostedTransaction): TransactionResponse =
        TransactionResponse(
            id = posted.id.toString(),
            reference = posted.reference,
            status = posted.status,
            postedAt = posted.postedAt,
        )

    fun toResponse(summary: TransactionSummary): TransactionResponse =
        TransactionResponse(
            id = summary.id.toString(),
            reference = summary.reference,
            status = summary.status,
            postedAt = summary.postedAt,
        )

    fun toDetailResponse(detail: TransactionDetail): TransactionDetailResponse =
        TransactionDetailResponse(
            id = detail.id.toString(),
            reference = detail.reference,
            description = detail.description,
            status = detail.status,
            reversesId = detail.reversesId?.toString(),
            postedAt = detail.postedAt,
            entries = detail.entries.map(::toResponse),
        )

    fun toResponse(entry: EntryView): EntryResponse =
        EntryResponse(
            accountId = entry.accountId.toString(),
            direction = entry.direction,
            amount = entry.amount,
            currency = entry.currency,
        )

    fun toResponse(entry: AccountEntry): AccountEntryResponse =
        AccountEntryResponse(
            id = entry.id.toString(),
            transactionId = entry.transactionId.toString(),
            direction = entry.direction,
            amount = entry.amount,
            currency = entry.currency,
            postedAt = entry.postedAt,
        )

    fun toPageResponse(page: AccountEntryPage): EntryPageResponse =
        EntryPageResponse(
            items = page.items.map(::toResponse),
            nextCursor = page.nextCursor,
        )

    fun toPageResponse(page: TransactionPage): PageResponse<TransactionResponse> =
        PageResponse(
            items = page.items.map { toResponse(it) },
            page = page.page,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )

    fun toResponse(snapshot: OverviewSnapshot): OverviewResponse =
        OverviewResponse(
            activity = snapshot.activity.map(::toActivityResponse),
            transactionsLast24h = snapshot.transactionsLast24h,
        )

    private fun toActivityResponse(item: ActivityItem): OverviewActivityResponse {
        val (typeString, resourceId) =
            when (item.type) {
                ActivityType.TRANSACTION_POSTED -> "transaction.posted" to TransactionId(item.resourceId).toString()
                ActivityType.TRANSACTION_REVERSED -> "transaction.reversed" to TransactionId(item.resourceId).toString()
                ActivityType.ACCOUNT_CREATED -> "account.created" to AccountId(item.resourceId).toString()
            }
        return OverviewActivityResponse(
            type = typeString,
            resourceId = resourceId,
            label = item.label,
            amount = item.amount,
            currency = item.currency,
            occurredAt = item.occurredAt,
        )
    }
}
