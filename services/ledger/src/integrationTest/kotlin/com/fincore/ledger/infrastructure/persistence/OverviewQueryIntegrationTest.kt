// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.enum.TransactionStatus
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(PostgresContainerExtension::class)
class OverviewQueryIntegrationTest(
    @Autowired private val transactionRepository: TransactionRepository,
    @Autowired private val accountRepository: AccountRepository,
    @Autowired private val entryRepository: EntryRepository,
    @Autowired private val entityManager: TestEntityManager,
) {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { PostgresContainerExtension.jdbcUrl }
            registry.add("spring.datasource.username") { PostgresContainerExtension.username }
            registry.add("spring.datasource.password") { PostgresContainerExtension.password }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
        }
    }

    // An obscure window inside the last entries partition (< 2027-04-01) so this shared-container
    // test is immune to rows committed by other integration tests (ordering + bucket counts).
    private val baseHour = Instant.parse("2027-03-15T08:00:00Z").truncatedTo(ChronoUnit.HOURS)

    private fun saveAccount(): AccountEntity {
        val id = UUID.randomUUID()
        return accountRepository.saveAndFlush(
            AccountEntity(
                id = id,
                name = "Test Wallet",
                type = AccountType.USER_WALLET,
                currency = "USD",
                status = AccountStatus.ACTIVE,
                metadata = "{}",
                version = 0,
                createdAt = baseHour,
                createdBy = "test",
                updatedAt = baseHour,
                updatedBy = "test",
            ),
        )
    }

    private fun saveAccountPair(): Pair<AccountEntity, AccountEntity> = saveAccount() to saveAccount()

    private fun saveBalancedTx(
        debitAccount: AccountEntity,
        creditAccount: AccountEntity,
        amount: BigDecimal,
        reference: String,
        description: String?,
        postedAt: Instant,
    ): TransactionEntity {
        val txId = UUID.randomUUID()
        val tx =
            transactionRepository.saveAndFlush(
                TransactionEntity(
                    id = txId,
                    reference = reference,
                    description = description,
                    status = TransactionStatus.POSTED,
                    reversesId = null,
                    metadata = "{}",
                    postedAt = postedAt,
                    createdAt = postedAt,
                    createdBy = "test",
                ),
            )
        entryRepository.saveAndFlush(
            EntryEntity(
                key = EntryKey(id = UUID.randomUUID(), createdAt = postedAt),
                transactionId = txId,
                accountId = debitAccount.id,
                amount = amount,
                currency = "USD",
                direction = EntryDirection.DEBIT,
                postedAt = postedAt,
            ),
        )
        entryRepository.saveAndFlush(
            EntryEntity(
                key = EntryKey(id = UUID.randomUUID(), createdAt = postedAt),
                transactionId = txId,
                accountId = creditAccount.id,
                amount = amount.negate(),
                currency = "USD",
                direction = EntryDirection.CREDIT,
                postedAt = postedAt,
            ),
        )
        return tx
    }

    @Test
    fun `findRecentActivity returns label as COALESCE description reference`() {
        val (accountA, accountB) = saveAccountPair()
        val withDescription =
            saveBalancedTx(
                debitAccount = accountA,
                creditAccount = accountB,
                amount = BigDecimal("100.00"),
                reference = "ref-desc-${ UUID.randomUUID() }",
                description = "wallet top-up",
                postedAt = baseHour,
            )
        val withoutDescription =
            saveBalancedTx(
                debitAccount = accountA,
                creditAccount = accountB,
                amount = BigDecimal("50.00"),
                reference = "ref-only-${ UUID.randomUUID() }",
                description = null,
                postedAt = baseHour.minusSeconds(60),
            )
        entityManager.flush()
        entityManager.clear()

        val rows = transactionRepository.findRecentActivity(20)

        val descRow = rows.first { it.id == withDescription.id.toString() }
        descRow.label shouldBe "wallet top-up"

        val refRow = rows.first { it.id == withoutDescription.id.toString() }
        refRow.label shouldBe withoutDescription.reference
    }

    @Test
    fun `findRecentActivity amount equals sum of positive DEBIT entries`() {
        val (accountA, accountB) = saveAccountPair()
        val posted =
            saveBalancedTx(
                debitAccount = accountA,
                creditAccount = accountB,
                amount = BigDecimal("250.500000000000000000"),
                reference = "ref-amount-${ UUID.randomUUID() }",
                description = null,
                postedAt = baseHour,
            )
        entityManager.flush()
        entityManager.clear()

        val rows = transactionRepository.findRecentActivity(20)
        val row = rows.first { it.id == posted.id.toString() }

        (row.amount.compareTo(BigDecimal("250.500000000000000000")) == 0) shouldBe true
        row.currency shouldBe "USD"
    }

    @Test
    fun `countByHourSince returns correct bucket counts and sum equals total posted`() {
        val (accountA, accountB) = saveAccountPair()
        val hour1 = baseHour
        val hour2 = baseHour.plus(1L, ChronoUnit.HOURS)

        // 2 transactions in hour1, 1 in hour2
        repeat(2) {
            saveBalancedTx(
                debitAccount = accountA,
                creditAccount = accountB,
                amount = BigDecimal("10.00"),
                reference = "ref-h1-${ UUID.randomUUID() }",
                description = null,
                postedAt = hour1.plusSeconds(it.toLong()),
            )
        }
        saveBalancedTx(
            debitAccount = accountA,
            creditAccount = accountB,
            amount = BigDecimal("20.00"),
            reference = "ref-h2-${ UUID.randomUUID() }",
            description = null,
            postedAt = hour2,
        )
        entityManager.flush()
        entityManager.clear()

        val since = hour1.minus(1L, ChronoUnit.SECONDS)
        val rows = transactionRepository.countByHourSince(since)

        val h1Bucket = rows.find { it.bucket.truncatedTo(ChronoUnit.HOURS) == hour1 }
        val h2Bucket = rows.find { it.bucket.truncatedTo(ChronoUnit.HOURS) == hour2 }

        h1Bucket.shouldNotBeNull()
        h1Bucket.cnt shouldBeGreaterThan 1L

        h2Bucket.shouldNotBeNull()
        h2Bucket.cnt shouldBe 1L

        // Sum across all buckets equals total transactions posted after since
        val totalFromBuckets = rows.sumOf { it.cnt }
        totalFromBuckets shouldBe 3L
    }

    @Test
    fun `findRecentActivity returns results ordered by postedAt desc`() {
        val (accountA, accountB) = saveAccountPair()
        val older =
            saveBalancedTx(
                debitAccount = accountA,
                creditAccount = accountB,
                amount = BigDecimal("10.00"),
                reference = "ref-old-${ UUID.randomUUID() }",
                description = null,
                postedAt = baseHour,
            )
        val newer =
            saveBalancedTx(
                debitAccount = accountA,
                creditAccount = accountB,
                amount = BigDecimal("20.00"),
                reference = "ref-new-${ UUID.randomUUID() }",
                description = null,
                postedAt = baseHour.plus(1L, ChronoUnit.HOURS),
            )
        entityManager.flush()
        entityManager.clear()

        val rows = transactionRepository.findRecentActivity(20)
        val ids = rows.map { it.id }

        val olderIndex = ids.indexOf(older.id.toString())
        val newerIndex = ids.indexOf(newer.id.toString())
        (newerIndex < olderIndex) shouldBe true
    }
}
