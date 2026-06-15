// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.exception.AccountNotFoundException
import com.fincore.ledger.domain.exception.DomainException
import com.fincore.ledger.infrastructure.persistence.AccountBalanceEntity
import com.fincore.ledger.infrastructure.persistence.AccountBalanceKey
import com.fincore.ledger.infrastructure.persistence.AccountBalanceRepository
import com.fincore.ledger.infrastructure.persistence.AccountEntity
import com.fincore.ledger.infrastructure.persistence.AccountPersistenceAdapter
import com.fincore.ledger.infrastructure.persistence.AccountRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import java.util.UUID

class AccountServiceImplTest {
    private val accountRepository = mockk<AccountRepository>()
    private val balanceRepository = mockk<AccountBalanceRepository>()
    private val auditWriter = mockk<AuditTrailWriter>(relaxed = true)
    private val service = AccountServiceImpl(accountRepository, balanceRepository, AccountPersistenceAdapter(), auditWriter)

    private val now = Instant.parse("2026-06-05T12:00:00Z")

    private fun entity(
        id: UUID,
        status: AccountStatus,
    ) = AccountEntity(id, "Wallet", AccountType.USER_WALLET, "USD", status, "{}", 0, now, "a", now, "a")

    @Test
    fun `should persist and return a new account`() {
        every { accountRepository.saveAndFlush(any()) } answers { firstArg() }

        val account = service.create(CreateAccountCommand("Wallet", AccountType.USER_WALLET, Currency.USD, "op"))

        account.name shouldBe "Wallet"
        account.status shouldBe AccountStatus.ACTIVE
        verify { accountRepository.saveAndFlush(any()) }
    }

    @Test
    fun `should throw when getting an unknown account`() {
        every { accountRepository.findById(any()) } returns Optional.empty()

        shouldThrow<AccountNotFoundException> { service.get(AccountId.generate()) }
    }

    @Test
    fun `should reject renaming a closed account`() {
        val id = UUID.randomUUID()
        every { accountRepository.findById(id) } returns Optional.of(entity(id, AccountStatus.CLOSED))

        shouldThrow<DomainException> { service.rename(AccountId(id), "New name", "op") }
    }

    @Test
    fun `should reject an illegal status transition`() {
        val id = UUID.randomUUID()
        every { accountRepository.findById(id) } returns Optional.of(entity(id, AccountStatus.CLOSED))

        shouldThrow<DomainException> { service.changeStatus(AccountId(id), AccountStatus.FROZEN, "op") }
    }

    @Test
    fun `should block closing an account with a non-zero balance`() {
        val id = UUID.randomUUID()
        every { accountRepository.findById(id) } returns Optional.of(entity(id, AccountStatus.ACTIVE))
        every { balanceRepository.findByKeyAccountId(id) } returns
            listOf(AccountBalanceEntity(AccountBalanceKey(id, "USD"), BigDecimal.ONE, now, 0))

        shouldThrow<DomainException> { service.changeStatus(AccountId(id), AccountStatus.CLOSED, "op") }
    }

    @Test
    fun `should close an account with zero balance`() {
        val id = UUID.randomUUID()
        every { accountRepository.findById(id) } returns Optional.of(entity(id, AccountStatus.ACTIVE))
        every { balanceRepository.findByKeyAccountId(id) } returns emptyList()
        every { accountRepository.saveAndFlush(any()) } answers { firstArg() }

        val account = service.changeStatus(AccountId(id), AccountStatus.CLOSED, "op")

        account.status shouldBe AccountStatus.CLOSED
    }
}
