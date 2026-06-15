// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.AccountId
import com.fincore.core.EntryId
import com.fincore.core.TransactionId
import com.fincore.ledger.infrastructure.persistence.EntryEntity
import com.fincore.ledger.infrastructure.persistence.EntryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
class EntryQueryServiceImpl(
    private val accountService: AccountService,
    private val entryRepository: EntryRepository,
) : EntryQueryService {
    @Transactional(readOnly = true)
    override fun listAccountEntries(
        accountId: AccountId,
        from: Instant?,
        to: Instant?,
        cursor: String?,
        limit: Int,
    ): AccountEntryPage {
        require(limit in 1..MAX_LIMIT) { "limit must be 1..$MAX_LIMIT" }
        accountService.get(accountId)
        val end = to ?: Instant.now()
        val start = from ?: end.minus(DEFAULT_WINDOW)
        require(!start.isAfter(end)) { "from must be on or before to" }
        require(Duration.between(start, end) <= MAX_WINDOW) { "range must span at most ${MAX_WINDOW.toDays()} days" }
        val decoded = cursor?.let(EntryCursor::decode)
        val rows =
            entryRepository.findAccountEntries(
                accountId.value,
                start,
                end,
                decoded?.postedAt,
                decoded?.id,
                PageRequest.of(0, limit + 1),
            )
        val hasMore = rows.size > limit
        val page = if (hasMore) rows.take(limit) else rows
        val nextCursor = if (hasMore) EntryCursor(page.last().postedAt, page.last().key.id).encode() else null
        return AccountEntryPage(page.map(::toEntry), nextCursor)
    }

    private fun toEntry(entry: EntryEntity): AccountEntry =
        AccountEntry(
            EntryId(entry.key.id),
            TransactionId(entry.transactionId),
            entry.direction,
            entry.amount,
            entry.currency,
            entry.postedAt,
        )

    private companion object {
        const val MAX_LIMIT = 200
        val DEFAULT_WINDOW: Duration = Duration.ofDays(30)
        val MAX_WINDOW: Duration = Duration.ofDays(90)
    }
}
