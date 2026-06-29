// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.enum.ActivityType
import com.fincore.ledger.infrastructure.persistence.AccountEntity
import com.fincore.ledger.infrastructure.persistence.AccountRepository
import com.fincore.ledger.infrastructure.persistence.HourlyTransactionCountRow
import com.fincore.ledger.infrastructure.persistence.TransactionActivityRow
import com.fincore.ledger.infrastructure.persistence.TransactionRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class OverviewServiceImplTest {
    private val transactionRepository = mockk<TransactionRepository>()
    private val accountRepository = mockk<AccountRepository>()
    private val service = OverviewServiceImpl(transactionRepository, accountRepository)

    private val base = Instant.parse("2026-06-28T10:00:00Z")

    private fun txRow(
        id: UUID = UUID.randomUUID(),
        label: String = "wallet top-up",
        reversal: Boolean = false,
        postedAt: Instant = base,
        amount: BigDecimal = BigDecimal("100.00"),
        currency: String = "USD",
    ): TransactionActivityRow =
        object : TransactionActivityRow {
            override val id = id.toString()
            override val label = label
            override val reversal = reversal
            override val postedat = postedAt
            override val amount = amount
            override val currency = currency
        }

    private fun accountEntity(
        id: UUID = UUID.randomUUID(),
        name: String = "Customer Wallet",
        createdAt: Instant = base.minusSeconds(3600),
    ) = AccountEntity(
        id = id,
        name = name,
        type = AccountType.USER_WALLET,
        currency = "USD",
        status = AccountStatus.ACTIVE,
        metadata = "{}",
        version = 0,
        createdAt = createdAt,
        createdBy = "system",
        updatedAt = createdAt,
        updatedBy = "system",
    )

    private fun hourRow(
        bucket: Instant,
        cnt: Long,
    ): HourlyTransactionCountRow =
        object : HourlyTransactionCountRow {
            override val bucket = bucket
            override val cnt = cnt
        }

    @Test
    fun `should map reverses_id presence to TRANSACTION_REVERSED type`() {
        every { transactionRepository.findRecentActivity(any()) } returns listOf(
            txRow(reversal = false),
            txRow(reversal = true),
        )
        every { accountRepository.findAllByOrderByCreatedAtDesc(any<Pageable>()) } returns emptyList()
        every { transactionRepository.countByHourSince(any()) } returns emptyList()

        val snapshot = service.overview()

        snapshot.activity[0].type shouldBe ActivityType.TRANSACTION_POSTED
        snapshot.activity[1].type shouldBe ActivityType.TRANSACTION_REVERSED
    }

    @Test
    fun `should merge transactions and accounts sorted by occurredAt descending`() {
        val txTime = base
        val accountTime = base.plusSeconds(60)

        every { transactionRepository.findRecentActivity(any()) } returns listOf(txRow(postedAt = txTime))
        every { accountRepository.findAllByOrderByCreatedAtDesc(any<Pageable>()) } returns listOf(
            accountEntity(createdAt = accountTime),
        )
        every { transactionRepository.countByHourSince(any()) } returns emptyList()

        val snapshot = service.overview()

        snapshot.activity shouldHaveSize 2
        snapshot.activity[0].occurredAt shouldBe accountTime
        snapshot.activity[1].occurredAt shouldBe txTime
        snapshot.activity[0].type shouldBe ActivityType.ACCOUNT_CREATED
        snapshot.activity[1].type shouldBe ActivityType.TRANSACTION_POSTED
    }

    @Test
    fun `should limit total activity to 20 items`() {
        val txRows = (1..20).map { txRow(postedAt = base.minusSeconds(it.toLong())) }
        val accountRows = (1..20).map { accountEntity(createdAt = base.plusSeconds(it.toLong())) }

        every { transactionRepository.findRecentActivity(any()) } returns txRows
        every { accountRepository.findAllByOrderByCreatedAtDesc(any<Pageable>()) } returns accountRows
        every { transactionRepository.countByHourSince(any()) } returns emptyList()

        val snapshot = service.overview()

        snapshot.activity shouldHaveSize 20
    }

    @Test
    fun `should return 24 zeros for sparkline when no transactions exist`() {
        every { transactionRepository.findRecentActivity(any()) } returns emptyList()
        every { accountRepository.findAllByOrderByCreatedAtDesc(any<Pageable>()) } returns emptyList()
        every { transactionRepository.countByHourSince(any()) } returns emptyList()

        val snapshot = service.overview()

        snapshot.transactionsLast24h shouldHaveSize 24
        snapshot.transactionsLast24h.all { it == 0 } shouldBe true
    }

    @Test
    fun `should densify sparkline with zero-fill and place counts in correct UTC slots`() {
        val nowHour = Instant.now().truncatedTo(ChronoUnit.HOURS)
        val since = nowHour.minus(23L, ChronoUnit.HOURS)

        // Two transactions in slot 0 (oldest), one transaction in slot 23 (current hour)
        val rows = listOf(
            hourRow(bucket = since, cnt = 2L),
            hourRow(bucket = nowHour, cnt = 1L),
        )

        every { transactionRepository.findRecentActivity(any()) } returns emptyList()
        every { accountRepository.findAllByOrderByCreatedAtDesc(any<Pageable>()) } returns emptyList()
        every { transactionRepository.countByHourSince(any()) } returns rows

        val snapshot = service.overview()

        val sparkline = snapshot.transactionsLast24h
        sparkline shouldHaveSize 24
        sparkline[0] shouldBe 2        // oldest slot
        sparkline[23] shouldBe 1       // current hour
        sparkline[1] shouldBe 0        // gap filled with zero
        sparkline[22] shouldBe 0
        sparkline.sum() shouldBe 3
    }

    @Test
    fun `should map account created items with null amount`() {
        every { transactionRepository.findRecentActivity(any()) } returns emptyList()
        every { accountRepository.findAllByOrderByCreatedAtDesc(any<Pageable>()) } returns listOf(
            accountEntity(name = "Settlement Reserve"),
        )
        every { transactionRepository.countByHourSince(any()) } returns emptyList()

        val snapshot = service.overview()

        val item = snapshot.activity.single()
        item.type shouldBe ActivityType.ACCOUNT_CREATED
        item.label shouldBe "Settlement Reserve"
        item.amount shouldBe null
        item.resourceId shouldNotBe null
    }
}
