// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.mapper

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.core.Money
import com.fincore.core.TransactionId
import com.fincore.ledger.api.dto.request.CreateAccountRequest
import com.fincore.ledger.api.dto.request.EntryLineRequest
import com.fincore.ledger.api.dto.request.PostTransactionRequest
import com.fincore.ledger.application.AccountBalance
import com.fincore.ledger.application.PostedTransaction
import com.fincore.ledger.domain.Account
import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.enum.EntryDirection
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class LedgerApiMapperTest {
    private val mapper = LedgerApiMapper()

    @Test
    fun `should map create request to command with parsed currency and injected actor`() {
        val command = mapper.toCommand(CreateAccountRequest("Wallet", AccountType.USER_WALLET, "EUR"), "user-1")

        command.name shouldBe "Wallet"
        command.type shouldBe AccountType.USER_WALLET
        command.currency shouldBe Currency.EUR
        command.actor shouldBe "user-1"
    }

    @Test
    fun `should serialize account id as prefixed ulid`() {
        val account = Account(AccountId.generate(), "Wallet", AccountType.ASSET, Currency.USD, AccountStatus.FROZEN)
        val response = mapper.toResponse(account)

        response.id shouldBe account.id.toString()
        response.id shouldStartWith "acc_"
        response.currency shouldBe "USD"
        response.status shouldBe AccountStatus.FROZEN
    }

    @Test
    fun `should preserve full money precision in balance response`() {
        val accountId = AccountId.generate()
        val amount = BigDecimal("12345.678901234567890123")
        val response = mapper.toResponse(AccountBalance(accountId, Money.of(amount, Currency.EUR), null))

        response.accountId shouldBe accountId.toString()
        response.currency shouldBe "EUR"
        response.amount.compareTo(amount) shouldBe 0
        response.amount.scale() shouldBe 18
    }

    @Test
    fun `should parse entry account ids and keep signed amounts when mapping a post command`() {
        val a = AccountId.generate()
        val b = AccountId.generate()
        val request =
            PostTransactionRequest(
                reference = "ref-1",
                description = null,
                currency = "EUR",
                entries =
                    listOf(
                        EntryLineRequest(a.toString(), EntryDirection.DEBIT, BigDecimal("100.00")),
                        EntryLineRequest(b.toString(), EntryDirection.CREDIT, BigDecimal("-100.00")),
                    ),
            )

        val command = mapper.toCommand(request, "user-1", "corr-1")

        command.currency shouldBe Currency.EUR
        command.actor shouldBe "user-1"
        command.correlationId shouldBe "corr-1"
        command.entries[0].accountId shouldBe a
        command.entries[0].amount.compareTo(BigDecimal("100.00")) shouldBe 0
        command.entries[1].accountId shouldBe b
        command.entries[1].amount.compareTo(BigDecimal("-100.00")) shouldBe 0
    }

    @Test
    fun `should serialize transaction id as prefixed ulid`() {
        val posted = PostedTransaction(TransactionId.generate(), "ref-1", java.time.Instant.now())
        mapper.toResponse(posted).id shouldStartWith "tx_"
    }
}
