// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.domain

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.core.EntryId
import com.fincore.core.Money
import com.fincore.core.TransactionId
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.enum.EntryDirection.CREDIT
import com.fincore.ledger.domain.enum.EntryDirection.DEBIT
import com.fincore.ledger.domain.enum.TransactionStatus
import com.fincore.ledger.domain.exception.CurrencyConsistencyViolationException
import com.fincore.ledger.domain.exception.DomainException
import com.fincore.ledger.domain.exception.DoubleEntryViolationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TransactionTest {
    private fun entry(
        accountId: AccountId = AccountId.generate(),
        direction: EntryDirection = DEBIT,
        amount: BigDecimal,
        currency: Currency = Currency.USD,
    ) = Entry(
        id = EntryId.generate(),
        accountId = accountId,
        direction = direction,
        amount = Money.of(amount, currency),
    )

    private fun buildTransaction(
        entries: List<Entry>,
        currency: Currency = Currency.USD,
    ): Transaction =
        Transaction(
            id = TransactionId.generate(),
            reference = "ref-${System.nanoTime()}",
            description = null,
            entries = entries,
            currency = currency,
        )

    @Test
    fun `should construct successfully when two entries sum to zero in USD`() {
        val accountA = AccountId.generate()
        val accountB = AccountId.generate()

        val transaction =
            buildTransaction(
                listOf(
                    entry(accountId = accountA, direction = DEBIT, amount = BigDecimal("100.00")),
                    entry(accountId = accountB, direction = CREDIT, amount = BigDecimal("-100.00")),
                ),
                currency = Currency.USD,
            )

        transaction.entries shouldHaveSize 2
        transaction.status shouldBe TransactionStatus.POSTED
    }

    @Test
    fun `should throw CurrencyConsistencyViolationException when entry currency does not match transaction currency`() {
        val exception =
            shouldThrow<CurrencyConsistencyViolationException> {
                buildTransaction(
                    listOf(
                        entry(direction = DEBIT, amount = BigDecimal("100.00"), currency = Currency.USD),
                        entry(direction = CREDIT, amount = BigDecimal("-100.00"), currency = Currency.EUR),
                    ),
                    currency = Currency.USD,
                )
            }
        exception.message.shouldNotBeBlank()
    }

    @Test
    fun `should throw CurrencyConsistencyViolationException when all entries use foreign currency even if balanced`() {
        shouldThrow<CurrencyConsistencyViolationException> {
            buildTransaction(
                listOf(
                    entry(direction = DEBIT, amount = BigDecimal("100.00"), currency = Currency.USD),
                    entry(direction = CREDIT, amount = BigDecimal("-100.00"), currency = Currency.USD),
                ),
                currency = Currency.EUR,
            )
        }
    }

    @Test
    fun `should throw DoubleEntryViolationException when entries do not sum to zero`() {
        val exception =
            shouldThrow<DoubleEntryViolationException> {
                buildTransaction(
                    listOf(
                        entry(direction = DEBIT, amount = BigDecimal("100.00")),
                        entry(direction = CREDIT, amount = BigDecimal("-90.00")),
                    ),
                    currency = Currency.USD,
                )
            }
        exception.message.shouldNotBeBlank()
    }

    @Test
    fun `should throw CurrencyConsistencyViolationException before balance violation when entries mismatch and are unbalanced`() {
        shouldThrow<CurrencyConsistencyViolationException> {
            buildTransaction(
                listOf(
                    entry(direction = DEBIT, amount = BigDecimal("100.00"), currency = Currency.EUR),
                    entry(direction = CREDIT, amount = BigDecimal("-90.00"), currency = Currency.EUR),
                ),
                currency = Currency.USD,
            )
        }
    }

    @Test
    fun `should throw CurrencyConsistencyViolationException when entries mix currencies even if balanced per currency`() {
        shouldThrow<CurrencyConsistencyViolationException> {
            buildTransaction(
                listOf(
                    entry(direction = DEBIT, amount = BigDecimal("100.00"), currency = Currency.USD),
                    entry(direction = CREDIT, amount = BigDecimal("-100.00"), currency = Currency.USD),
                    entry(direction = DEBIT, amount = BigDecimal("50.00"), currency = Currency.EUR),
                    entry(direction = CREDIT, amount = BigDecimal("-50.00"), currency = Currency.EUR),
                ),
                currency = Currency.USD,
            )
        }
    }

    @Test
    fun `should throw CurrencyConsistencyViolationException when entries mix currencies and EUR entries do not sum to zero`() {
        shouldThrow<CurrencyConsistencyViolationException> {
            buildTransaction(
                listOf(
                    entry(direction = DEBIT, amount = BigDecimal("100.00"), currency = Currency.USD),
                    entry(direction = CREDIT, amount = BigDecimal("-100.00"), currency = Currency.USD),
                    entry(direction = DEBIT, amount = BigDecimal("50.00"), currency = Currency.EUR),
                    entry(direction = CREDIT, amount = BigDecimal("-40.00"), currency = Currency.EUR),
                ),
                currency = Currency.USD,
            )
        }
    }

    @Test
    fun `should construct successfully when four entries balance to zero across two accounts`() {
        val accountA = AccountId.generate()
        val accountB = AccountId.generate()

        val transaction =
            buildTransaction(
                listOf(
                    entry(accountId = accountA, direction = DEBIT, amount = BigDecimal("50.00")),
                    entry(accountId = accountA, direction = CREDIT, amount = BigDecimal("-50.00")),
                    entry(accountId = accountB, direction = DEBIT, amount = BigDecimal("75.00")),
                    entry(accountId = accountB, direction = CREDIT, amount = BigDecimal("-75.00")),
                ),
            )

        transaction.entries shouldHaveSize 4
    }

    @Test
    fun `should throw DoubleEntryViolationException when single debit entry has no counterpart`() {
        shouldThrow<DoubleEntryViolationException> {
            buildTransaction(
                listOf(
                    entry(direction = DEBIT, amount = BigDecimal("100.00")),
                    entry(direction = DEBIT, amount = BigDecimal("100.00")),
                ),
            )
        }
    }

    @Test
    fun `should throw DoubleEntryViolationException when net is non-zero due to rounding in caller`() {
        shouldThrow<DoubleEntryViolationException> {
            buildTransaction(
                listOf(
                    entry(direction = DEBIT, amount = BigDecimal("100.001")),
                    entry(direction = CREDIT, amount = BigDecimal("-100.000")),
                ),
            )
        }
    }

    @Test
    fun `should throw DomainException when entry list is empty`() {
        shouldThrow<DomainException> {
            buildTransaction(emptyList())
        }
    }

    @Test
    fun `should throw DomainException when entry list has exactly one entry`() {
        shouldThrow<DomainException> {
            buildTransaction(
                listOf(entry(direction = DEBIT, amount = BigDecimal("100.00"))),
            )
        }
    }

    @Test
    fun `should throw DomainException when entry list exceeds 1000 entries`() {
        val entries =
            (1..501).flatMap {
                listOf(
                    entry(accountId = AccountId.generate(), direction = DEBIT, amount = BigDecimal("1.00")),
                    entry(accountId = AccountId.generate(), direction = CREDIT, amount = BigDecimal("-1.00")),
                )
            }

        shouldThrow<DomainException> {
            buildTransaction(entries)
        }
    }

    @Test
    fun `should accept exactly 1000 entries when balanced`() {
        val entries =
            (1..500).flatMap {
                listOf(
                    entry(accountId = AccountId.generate(), direction = DEBIT, amount = BigDecimal("1.00")),
                    entry(accountId = AccountId.generate(), direction = CREDIT, amount = BigDecimal("-1.00")),
                )
            }

        val transaction = buildTransaction(entries)
        transaction.entries shouldHaveSize 1000
    }

    @Test
    fun `should throw DomainException when two entries share the same accountId and direction`() {
        val sharedAccount = AccountId.generate()
        val counterAccount = AccountId.generate()

        shouldThrow<DomainException> {
            buildTransaction(
                listOf(
                    entry(accountId = sharedAccount, direction = DEBIT, amount = BigDecimal("50.00")),
                    entry(accountId = sharedAccount, direction = DEBIT, amount = BigDecimal("50.00")),
                    entry(accountId = counterAccount, direction = CREDIT, amount = BigDecimal("-100.00")),
                ),
            )
        }
    }

    @Test
    fun `should allow the same accountId with different directions`() {
        val account = AccountId.generate()

        val transaction =
            buildTransaction(
                listOf(
                    entry(accountId = account, direction = DEBIT, amount = BigDecimal("100.00")),
                    entry(accountId = account, direction = CREDIT, amount = BigDecimal("-100.00")),
                ),
            )
        transaction.entries shouldHaveSize 2
    }

    @Test
    fun `should default status to POSTED on construction`() {
        val transaction =
            buildTransaction(
                listOf(
                    entry(direction = DEBIT, amount = BigDecimal("1.00")),
                    entry(direction = CREDIT, amount = BigDecimal("-1.00")),
                ),
            )
        transaction.status shouldBe TransactionStatus.POSTED
    }

    @Test
    fun `should expose the reference supplied at construction`() {
        val ref = "pay-test-reference-001"
        val transaction =
            Transaction(
                id = TransactionId.generate(),
                reference = ref,
                description = "Test payment",
                entries =
                    listOf(
                        entry(direction = DEBIT, amount = BigDecimal("10.00")),
                        entry(direction = CREDIT, amount = BigDecimal("-10.00")),
                    ),
                currency = Currency.USD,
            )
        transaction.reference shouldBe ref
    }

    @Test
    fun `should carry optional description when provided`() {
        val desc = "Salary payment Q1"
        val transaction =
            Transaction(
                id = TransactionId.generate(),
                reference = "ref-desc-test",
                description = desc,
                entries =
                    listOf(
                        entry(direction = DEBIT, amount = BigDecimal("1.00")),
                        entry(direction = CREDIT, amount = BigDecimal("-1.00")),
                    ),
                currency = Currency.USD,
            )
        transaction.description shouldBe desc
    }

    @Test
    fun `should carry null description when not provided`() {
        val transaction =
            Transaction(
                id = TransactionId.generate(),
                reference = "ref-no-desc",
                description = null,
                entries =
                    listOf(
                        entry(direction = DEBIT, amount = BigDecimal("1.00")),
                        entry(direction = CREDIT, amount = BigDecimal("-1.00")),
                    ),
                currency = Currency.USD,
            )
        transaction.description shouldBe null
    }
}
