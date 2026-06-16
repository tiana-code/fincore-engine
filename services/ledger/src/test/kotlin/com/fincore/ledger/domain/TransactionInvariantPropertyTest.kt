// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.domain

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.core.EntryId
import com.fincore.core.Money
import com.fincore.core.TransactionId
import com.fincore.ledger.domain.enum.EntryDirection.CREDIT
import com.fincore.ledger.domain.enum.EntryDirection.DEBIT
import com.fincore.ledger.domain.exception.CurrencyConsistencyViolationException
import com.fincore.ledger.domain.exception.DomainException
import com.fincore.ledger.domain.exception.DoubleEntryViolationException
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TransactionInvariantPropertyTest {
    private val currencies = Arb.element(Currency.EUR, Currency.USD)
    private val magnitudesCents = Arb.list(Arb.int(MIN_CENTS..MAX_CENTS), MIN_HALVES..MAX_HALVES)
    private val deltaCents = Arb.int(MIN_CENTS..MAX_CENTS)
    private val config = PropTestConfig(iterations = ITERATIONS)

    @Test
    fun `should accept and hold the zero-sum invariant for any balanced multi-leg transaction`() {
        runBlocking {
            checkAll(config, magnitudesCents, currencies) { cents, currency ->
                val entries = balancedEntries(cents, currency)
                val transaction = transactionOf(entries, currency)

                transaction.entries shouldHaveSize entries.size
                netAmount(entries).compareTo(BigDecimal.ZERO) shouldBe 0
                entries.all { it.amount.amount.signum() != 0 } shouldBe true
                entries.all { it.amount.currency == currency } shouldBe true
                entries.map { it.accountId to it.direction }.toSet() shouldHaveSize entries.size
            }
        }
    }

    @Test
    fun `should reject with DoubleEntryViolationException when one leg is perturbed off zero`() {
        runBlocking {
            checkAll(config, magnitudesCents, currencies, deltaCents) { cents, currency, delta ->
                val entries = balancedEntries(cents, currency).toMutableList()
                val leg = entries[0]
                entries[0] = leg.copyWithAmount(leg.amount.amount.add(amountOf(delta)), currency)

                shouldThrowExactly<DoubleEntryViolationException> { transactionOf(entries, currency) }
            }
        }
    }

    @Test
    fun `should reject with CurrencyConsistencyViolationException when one entry uses a foreign currency`() {
        runBlocking {
            checkAll(config, magnitudesCents, currencies) { cents, currency ->
                val entries = balancedEntries(cents, currency).toMutableList()
                val other = if (currency == Currency.EUR) Currency.USD else Currency.EUR
                val leg = entries[0]
                entries[0] = leg.copyWithAmount(leg.amount.amount, other)

                shouldThrowExactly<CurrencyConsistencyViolationException> { transactionOf(entries, currency) }
            }
        }
    }

    @Test
    fun `should reject with DomainException when two entries share an account and direction`() {
        runBlocking {
            checkAll(config, magnitudesCents, currencies) { cents, currency ->
                val entries = balancedEntries(cents, currency).toMutableList()
                val leg = entries[0]
                entries.add(Entry(EntryId.generate(), leg.accountId, leg.direction, Money.of(amountOf(MIN_CENTS), currency)))

                shouldThrowExactly<DomainException> { transactionOf(entries, currency) }
            }
        }
    }

    private fun balancedEntries(
        cents: List<Int>,
        currency: Currency,
    ): List<Entry> =
        cents.flatMap { magnitude ->
            val amount = amountOf(magnitude)
            listOf(
                Entry(EntryId.generate(), AccountId.generate(), DEBIT, Money.of(amount, currency)),
                Entry(EntryId.generate(), AccountId.generate(), CREDIT, Money.of(amount.negate(), currency)),
            )
        }

    private fun transactionOf(
        entries: List<Entry>,
        currency: Currency,
    ): Transaction =
        Transaction(
            id = TransactionId.generate(),
            reference = "prop-ref",
            description = null,
            entries = entries,
            currency = currency,
        )

    private fun Entry.copyWithAmount(
        amount: BigDecimal,
        currency: Currency,
    ): Entry = Entry(id, accountId, direction, Money.of(amount, currency))

    private fun amountOf(cents: Int): BigDecimal = BigDecimal(cents).movePointLeft(2)

    private fun netAmount(entries: List<Entry>): BigDecimal = entries.fold(BigDecimal.ZERO) { acc, entry -> acc.add(entry.amount.amount) }

    companion object {
        private const val ITERATIONS = 1000
        private const val MIN_HALVES = 1
        private const val MAX_HALVES = 8
        private const val MIN_CENTS = 1
        private const val MAX_CENTS = 1_000_000
    }
}
