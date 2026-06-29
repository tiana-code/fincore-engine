// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.ledger.domain.enum.ActivityType
import com.fincore.ledger.infrastructure.persistence.AccountRepository
import com.fincore.ledger.infrastructure.persistence.TransactionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class OverviewServiceImpl(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) : OverviewService {
    @Transactional(readOnly = true)
    override fun overview(): OverviewSnapshot {
        val txItems =
            transactionRepository.findRecentActivity(ACTIVITY_LIMIT).map { row ->
                ActivityItem(
                    type = if (row.reversal) ActivityType.TRANSACTION_REVERSED else ActivityType.TRANSACTION_POSTED,
                    resourceId = UUID.fromString(row.id),
                    label = row.label,
                    amount = row.amount,
                    currency = row.currency,
                    occurredAt = row.postedat,
                )
            }

        val accountItems =
            accountRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, ACTIVITY_LIMIT)).map { entity ->
                ActivityItem(
                    type = ActivityType.ACCOUNT_CREATED,
                    resourceId = entity.id,
                    label = entity.name,
                    amount = null,
                    currency = entity.currency,
                    occurredAt = entity.createdAt,
                )
            }

        val activity =
            (txItems + accountItems)
                .sortedByDescending { it.occurredAt }
                .take(ACTIVITY_LIMIT)

        val sparkline = buildSparkline()

        return OverviewSnapshot(activity = activity, transactionsLast24h = sparkline)
    }

    /**
     * Builds a dense 24-slot hourly array (oldest-first) by bucketing raw posted_at
     * Instants in the JVM, avoiding any SQL session-timezone dependency (critic finding 5).
     * Slot 0 = the hour starting 23h before the current truncated hour; slot 23 = current hour.
     */
    private fun buildSparkline(): List<Int> {
        val nowHour = Instant.now().truncatedTo(ChronoUnit.HOURS)
        val since = nowHour.minus((SPARK_HOURS - 1).toLong(), ChronoUnit.HOURS)
        val rows = transactionRepository.countByHourSince(since)

        val counts = IntArray(SPARK_HOURS)
        for (row in rows) {
            val bucketHour = row.bucket.truncatedTo(ChronoUnit.HOURS)
            val slotIndex = ChronoUnit.HOURS.between(since, bucketHour).toInt()
            if (slotIndex in 0 until SPARK_HOURS) {
                counts[slotIndex] = row.cnt.toInt()
            }
        }
        return counts.toList()
    }

    private companion object {
        const val ACTIVITY_LIMIT = 20
        const val SPARK_HOURS = 24
    }
}
