// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.AccountId
import com.fincore.ledger.domain.Account
import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.exception.AccountNotFoundException
import com.fincore.ledger.domain.exception.DomainException
import com.fincore.ledger.infrastructure.persistence.AccountBalanceRepository
import com.fincore.ledger.infrastructure.persistence.AccountEntity
import com.fincore.ledger.infrastructure.persistence.AccountPersistenceAdapter
import com.fincore.ledger.infrastructure.persistence.AccountRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AccountServiceImpl(
    private val accountRepository: AccountRepository,
    private val balanceRepository: AccountBalanceRepository,
    private val adapter: AccountPersistenceAdapter,
) : AccountService {
    @Transactional
    override fun create(command: CreateAccountCommand): Account {
        val account = Account(AccountId.generate(), command.name, command.type, command.currency)
        val entity = adapter.toNewEntity(account, command.actor, Instant.now())
        accountRepository.saveAndFlush(entity)
        return adapter.toDomain(entity)
    }

    @Transactional(readOnly = true)
    override fun get(id: AccountId): Account = adapter.toDomain(load(id))

    @Transactional(readOnly = true)
    override fun list(
        page: Int,
        size: Int,
    ): AccountPage {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
        val result = accountRepository.findAll(pageable)
        return AccountPage(
            items = result.content.map(adapter::toDomain),
            page = page,
            size = size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    @Transactional
    override fun rename(
        id: AccountId,
        newName: String,
        actor: String,
    ): Account {
        val entity = load(id)
        val account = adapter.toDomain(entity)
        account.rename(newName)
        entity.name = account.name
        entity.updatedBy = actor
        return adapter.toDomain(accountRepository.saveAndFlush(entity))
    }

    @Transactional
    override fun changeStatus(
        id: AccountId,
        target: AccountStatus,
        actor: String,
    ): Account {
        val entity = load(id)
        if (target == AccountStatus.CLOSED) {
            requireZeroBalance(id)
        }
        val account = adapter.toDomain(entity)
        account.transitionStatus(target)
        entity.status = account.status
        entity.updatedBy = actor
        return adapter.toDomain(accountRepository.saveAndFlush(entity))
    }

    private fun load(id: AccountId): AccountEntity = accountRepository.findById(id.value).orElseThrow { AccountNotFoundException(id) }

    private fun requireZeroBalance(id: AccountId) {
        val hasFunds = balanceRepository.findByKeyAccountId(id.value).any { it.balance.signum() != 0 }
        if (hasFunds) {
            throw DomainException("Cannot close account $id while it holds a non-zero balance")
        }
    }
}
