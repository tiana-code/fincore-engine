// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.db

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import java.math.BigDecimal
import java.sql.SQLException
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LedgerSchemaMigrationIT {
    private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(IMAGE)
    private val testDb = LedgerSchemaTestDb(postgres)

    @BeforeAll
    fun setUp() {
        postgres.start()
        testDb.migrate()
    }

    @AfterAll
    fun tearDown() {
        postgres.stop()
    }

    @Test
    fun `should create ledger and platform schemas and all tables when changelog applied`() {
        testDb.intQuery(
            "SELECT count(*) FROM information_schema.schemata " +
                "WHERE schema_name IN ('ledger','platform')",
        ) shouldBe 2
        testDb.boolQuery("SELECT to_regclass('ledger.accounts') IS NOT NULL") shouldBe true
        testDb.boolQuery("SELECT to_regclass('ledger.transactions') IS NOT NULL") shouldBe true
        testDb.boolQuery("SELECT to_regclass('ledger.entries') IS NOT NULL") shouldBe true
        testDb.boolQuery("SELECT to_regclass('ledger.entries_2026_q2') IS NOT NULL") shouldBe true
        testDb.boolQuery("SELECT to_regclass('ledger.entries_2027_q1') IS NOT NULL") shouldBe true
    }

    @Test
    fun `should create deferred invariant trigger and helper functions when changelog applied`() {
        testDb.intQuery(
            "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace " +
                "WHERE (n.nspname = 'platform' AND p.proname = 'set_updated_at') " +
                "OR (n.nspname = 'ledger' AND p.proname = 'verify_double_entry_invariant')",
        ) shouldBe 2
        testDb.boolQuery(
            "SELECT bool_and(tgdeferrable AND tginitdeferred) FROM pg_trigger " +
                "WHERE tgname = 'trg_entries_invariant'",
        ) shouldBe true
    }

    @Test
    fun `should apply changelog idempotently when run again on populated database`() {
        testDb.open().use { connection ->
            connection.createStatement().use { it.execute("TRUNCATE databasechangelog") }
        }
        shouldNotThrowAny { testDb.migrate() }
    }

    @Test
    fun `should reject unbalanced transaction at commit`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID()
        testDb.open().use { connection ->
            connection.autoCommit = false
            testDb.insertAccount(connection, accountId)
            testDb.insertTransaction(connection, txId, txId.toString())
            testDb.insertEntry(connection, txId, accountId, AMOUNT, DEBIT)
            testDb.insertEntry(connection, txId, accountId, UNBALANCED, CREDIT)
            shouldThrow<SQLException> { connection.commit() }
        }
        testDb.entryCount(txId) shouldBe 0
    }

    @Test
    fun `should accept balanced transaction inserted in a single statement`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID()
        testDb.open().use { connection ->
            connection.autoCommit = false
            testDb.insertAccount(connection, accountId)
            testDb.insertTransaction(connection, txId, txId.toString())
            testDb.insertEntryPair(connection, txId, accountId, AMOUNT, NEG_AMOUNT)
            connection.commit()
        }
        testDb.entryCount(txId) shouldBe 2
    }

    @Test
    fun `should accept balanced transaction with legs in separate statements`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID()
        testDb.open().use { connection ->
            connection.autoCommit = false
            testDb.insertAccount(connection, accountId)
            testDb.insertTransaction(connection, txId, txId.toString())
            testDb.insertEntry(connection, txId, accountId, AMOUNT, DEBIT)
            testDb.insertEntry(connection, txId, accountId, NEG_AMOUNT, CREDIT)
            connection.commit()
        }
        testDb.entryCount(txId) shouldBe 2
    }

    @Test
    fun `should reject account with invalid type`() {
        testDb.open().use { connection ->
            shouldThrow<SQLException> { testDb.insertAccount(connection, UUID.randomUUID(), type = "BOGUS") }
        }
    }

    @Test
    fun `should reject entry with zero amount`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID()
        testDb.open().use { connection ->
            testDb.insertAccount(connection, accountId)
            testDb.insertTransaction(connection, txId, txId.toString())
            shouldThrow<SQLException> { testDb.insertEntry(connection, txId, accountId, ZERO, DEBIT) }
        }
    }

    @Test
    fun `should reject a second reversal of the same transaction`() {
        val original = UUID.randomUUID()
        val firstReversal = UUID.randomUUID()
        val secondReversal = UUID.randomUUID()
        testDb.open().use { connection ->
            testDb.insertTransaction(connection, original, original.toString())
            testDb.insertTransaction(connection, firstReversal, firstReversal.toString(), reversesId = original)
            shouldThrow<SQLException> {
                testDb.insertTransaction(connection, secondReversal, secondReversal.toString(), reversesId = original)
            }
        }
    }

    @Test
    fun `should prevent deleting an account referenced by entries`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID()
        testDb.open().use { connection ->
            connection.autoCommit = false
            testDb.insertAccount(connection, accountId)
            testDb.insertTransaction(connection, txId, txId.toString())
            testDb.insertEntryPair(connection, txId, accountId, AMOUNT, NEG_AMOUNT)
            connection.commit()
        }
        testDb.open().use { connection ->
            shouldThrow<SQLException> {
                connection.prepareStatement("DELETE FROM ledger.accounts WHERE id = ?").use { statement ->
                    statement.setObject(1, accountId)
                    statement.executeUpdate()
                }
            }
        }
    }

    @Test
    fun `should store and read back 18 digit numeric precision`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID()
        val entryId = UUID.randomUUID()
        testDb.open().use { connection ->
            connection.autoCommit = false
            testDb.insertAccount(connection, accountId)
            testDb.insertTransaction(connection, txId, txId.toString())
            testDb.insertEntry(connection, txId, accountId, PRECISE, DEBIT, id = entryId)
            testDb.insertEntry(connection, txId, accountId, NEG_PRECISE, CREDIT)
            connection.commit()
        }
        val stored = testDb.bigDecimalById("SELECT amount FROM ledger.entries WHERE id = ?", entryId)
        (stored.compareTo(BigDecimal(PRECISE)) == 0) shouldBe true
    }

    @Test
    fun `should route entry to the matching quarterly partition`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID()
        val entryId = UUID.randomUUID()
        testDb.open().use { connection ->
            connection.autoCommit = false
            testDb.insertAccount(connection, accountId)
            testDb.insertTransaction(connection, txId, txId.toString())
            testDb.insertEntry(connection, txId, accountId, AMOUNT, DEBIT, id = entryId, createdAt = CREATED_Q3)
            testDb.insertEntry(connection, txId, accountId, NEG_AMOUNT, CREDIT, createdAt = CREATED_Q3)
            connection.commit()
        }
        testDb.stringById(
            "SELECT tableoid::regclass::text FROM ledger.entries WHERE id = ?",
            entryId,
        ) shouldBe "ledger.entries_2026_q3"
    }
}
