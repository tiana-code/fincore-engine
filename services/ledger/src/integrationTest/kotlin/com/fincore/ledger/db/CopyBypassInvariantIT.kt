// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.postgresql.PGConnection
import org.testcontainers.containers.PostgreSQLContainer
import java.io.StringReader
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.UUID

private const val SQLSTATE_CHECK_VIOLATION = "23514"
private const val ENTRIES_Q2_PARTITION = "ledger.entries_2026_q2"
private const val COPY_SQL =
    "COPY ledger.entries(id,transaction_id,account_id,amount,currency,direction,created_at) FROM STDIN"
private const val INSERT_SQL =
    "INSERT INTO ledger.entries(id,transaction_id,account_id,amount,currency,direction,created_at) " +
        "VALUES (?,?,?,?::numeric,?,?,?::timestamptz)"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CopyBypassInvariantIT {
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
    fun `should reject unbalanced entries when loaded via copy`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID()
        testDb.open().use { connection ->
            connection.autoCommit = false
            testDb.insertAccount(connection, accountId)
            testDb.insertTransaction(connection, txId, txId.toString())
            copyEntries(connection, txId, accountId, AMOUNT, UNBALANCED)
            shouldThrow<SQLException> { connection.commit() }.sqlState shouldBe SQLSTATE_CHECK_VIOLATION
        }
        testDb.entryCount(txId) shouldBe 0
    }

    @Test
    fun `should reject unbalanced entries when loaded via jdbc batch`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID()
        testDb.open().use { connection ->
            connection.autoCommit = false
            testDb.insertAccount(connection, accountId)
            testDb.insertTransaction(connection, txId, txId.toString())
            batchInsertEntries(connection, txId, accountId, AMOUNT, UNBALANCED)
            shouldThrow<SQLException> { connection.commit() }.sqlState shouldBe SQLSTATE_CHECK_VIOLATION
        }
        testDb.entryCount(txId) shouldBe 0
    }

    @Test
    fun `should accept balanced entries when loaded via copy`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID()
        testDb.open().use { connection ->
            connection.autoCommit = false
            testDb.insertAccount(connection, accountId)
            testDb.insertTransaction(connection, txId, txId.toString())
            copyEntries(connection, txId, accountId, AMOUNT, NEG_AMOUNT)
            connection.commit()
        }
        testDb.entryCount(txId) shouldBe 2
    }

    @Test
    fun `should route balanced copy rows to the quarterly partition`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID()
        testDb.open().use { connection ->
            connection.autoCommit = false
            testDb.insertAccount(connection, accountId)
            testDb.insertTransaction(connection, txId, txId.toString())
            copyEntries(connection, txId, accountId, AMOUNT, NEG_AMOUNT)
            connection.commit()
        }
        testDb.stringById(
            "SELECT tableoid::regclass::text FROM ledger.entries WHERE transaction_id = ?",
            txId,
        ) shouldBe ENTRIES_Q2_PARTITION
    }

    private fun copyEntries(
        connection: Connection,
        txId: UUID,
        accountId: UUID,
        debit: String,
        credit: String,
    ) {
        val rows =
            copyLine(txId, accountId, debit, DEBIT) + copyLine(txId, accountId, credit, CREDIT)
        connection.unwrap(PGConnection::class.java).copyAPI.copyIn(COPY_SQL, StringReader(rows))
    }

    private fun copyLine(
        txId: UUID,
        accountId: UUID,
        amount: String,
        direction: String,
    ): String =
        listOf(UUID.randomUUID(), txId, accountId, amount, CCY, direction, CREATED_Q2)
            .joinToString(separator = "\t", postfix = "\n")

    private fun batchInsertEntries(
        connection: Connection,
        txId: UUID,
        accountId: UUID,
        debit: String,
        credit: String,
    ) {
        connection.prepareStatement(INSERT_SQL).use { statement ->
            bindEntry(statement, txId, accountId, debit, DEBIT)
            bindEntry(statement, txId, accountId, credit, CREDIT)
            statement.executeBatch()
        }
    }

    @Suppress("MagicNumber") // positional JDBC parameter indices
    private fun bindEntry(
        statement: PreparedStatement,
        txId: UUID,
        accountId: UUID,
        amount: String,
        direction: String,
    ) {
        statement.setObject(1, UUID.randomUUID())
        statement.setObject(2, txId)
        statement.setObject(3, accountId)
        statement.setString(4, amount)
        statement.setString(5, CCY)
        statement.setString(6, direction)
        statement.setString(7, CREATED_Q2)
        statement.addBatch()
    }
}
