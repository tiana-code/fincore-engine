// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.enum.TransactionStatus
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(PostgresContainerExtension::class)
class LedgerAggregatePersistenceIT(
    @Autowired private val accountRepository: AccountRepository,
    @Autowired private val transactionRepository: TransactionRepository,
    @Autowired private val entryRepository: EntryRepository,
    @Autowired private val balanceRepository: AccountBalanceRepository,
    @Autowired private val entityManager: TestEntityManager,
) {
    private val instant = Instant.parse("2026-06-05T12:00:00Z")

    private fun newAccount(id: UUID = UUID.randomUUID()) =
        AccountEntity(
            id = id,
            name = "Operating wallet",
            type = AccountType.USER_WALLET,
            currency = "USD",
            status = AccountStatus.ACTIVE,
            metadata = "{\"tier\": \"gold\"}",
            version = 0,
            createdAt = instant,
            createdBy = "auth0|operator",
            updatedAt = instant,
            updatedBy = "auth0|operator",
        )

    @Test
    fun `should round trip an account with enum metadata and version`() {
        val id = UUID.randomUUID()
        accountRepository.saveAndFlush(newAccount(id))
        entityManager.clear()

        val loaded = accountRepository.findById(id).orElseThrow()
        loaded.name shouldBe "Operating wallet"
        loaded.type shouldBe AccountType.USER_WALLET
        loaded.status shouldBe AccountStatus.ACTIVE
        loaded.currency shouldBe "USD"
        loaded.metadata.contains("gold") shouldBe true
        loaded.version shouldBe 0
    }

    @Test
    fun `should increment account version on update`() {
        val id = UUID.randomUUID()
        accountRepository.saveAndFlush(newAccount(id))
        entityManager.clear()

        val loaded = accountRepository.findById(id).orElseThrow()
        loaded.status = AccountStatus.FROZEN
        accountRepository.saveAndFlush(loaded)

        accountRepository.findById(id).orElseThrow().version shouldBe 1
    }

    @Test
    fun `should reject a stale account write with optimistic lock failure`() {
        val id = UUID.randomUUID()
        accountRepository.saveAndFlush(newAccount(id))
        entityManager.clear()

        val current = accountRepository.findById(id).orElseThrow()
        current.status = AccountStatus.FROZEN
        accountRepository.saveAndFlush(current)
        entityManager.clear()

        val stale = newAccount(id).apply { version = 0 }
        shouldThrow<ObjectOptimisticLockingFailureException> {
            accountRepository.saveAndFlush(stale)
        }
    }

    @Test
    fun `should round trip an insert only transaction`() {
        val id = UUID.randomUUID()
        transactionRepository.saveAndFlush(
            TransactionEntity(
                id = id,
                reference = "ref-$id",
                description = null,
                status = TransactionStatus.POSTED,
                reversesId = null,
                metadata = "{}",
                postedAt = instant,
                createdAt = instant,
                createdBy = "auth0|operator",
            ),
        )
        entityManager.clear()

        val loaded = transactionRepository.findById(id).orElseThrow()
        loaded.reference shouldBe "ref-$id"
        loaded.status shouldBe TransactionStatus.POSTED
        loaded.description shouldBe null
        loaded.reversesId shouldBe null
    }

    @Test
    fun `should round trip an entry preserving 18 digit money scale and composite key`() {
        val accountId = UUID.randomUUID()
        accountRepository.saveAndFlush(newAccount(accountId))
        val txId = UUID.randomUUID()
        transactionRepository.saveAndFlush(
            TransactionEntity(
                id = txId,
                reference = "ref-$txId",
                description = null,
                status = TransactionStatus.POSTED,
                reversesId = null,
                metadata = "{}",
                postedAt = instant,
                createdAt = instant,
                createdBy = "auth0|operator",
            ),
        )
        val amount = BigDecimal("12345.123456789012345678")
        val key = EntryKey(id = UUID.randomUUID(), createdAt = instant)
        entryRepository.saveAndFlush(
            EntryEntity(
                key = key,
                transactionId = txId,
                accountId = accountId,
                amount = amount,
                currency = "EUR",
                direction = EntryDirection.DEBIT,
                postedAt = instant,
            ),
        )
        entityManager.clear()

        val loaded = entryRepository.findById(key).orElseThrow()
        loaded.amount.scale() shouldBe 18
        (loaded.amount.compareTo(amount) == 0) shouldBe true
        loaded.direction shouldBe EntryDirection.DEBIT
        loaded.currency shouldBe "EUR"
    }

    @Test
    fun `should round trip an account balance with precision and version`() {
        val accountId = UUID.randomUUID()
        accountRepository.saveAndFlush(newAccount(accountId))
        val key = AccountBalanceKey(accountId = accountId, currency = "USD")
        val balance = BigDecimal("999.000000000000000001")
        balanceRepository.saveAndFlush(
            AccountBalanceEntity(
                key = key,
                balance = balance,
                lastPostedAt = instant,
                version = 0,
            ),
        )
        entityManager.clear()

        val loaded = balanceRepository.findById(key).orElseThrow()
        loaded.balance.scale() shouldBe 18
        (loaded.balance.compareTo(balance) == 0) shouldBe true
        loaded.version shouldBe 0
    }

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
}
