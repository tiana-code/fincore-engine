// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.db

import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.testcontainers.containers.PostgreSQLContainer
import java.math.BigDecimal
import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.UUID

internal const val IMAGE = "postgres:17-alpine"
internal const val CHANGELOG = "db/changelog/db.changelog-master.yaml"
internal const val ACTOR = "test"
internal const val CCY = "USD"
internal const val ASSET = "ASSET"
internal const val POSTED = "POSTED"
internal const val DEBIT = "DEBIT"
internal const val CREDIT = "CREDIT"
internal const val AMOUNT = "100.00"
internal const val NEG_AMOUNT = "-100.00"
internal const val UNBALANCED = "-90.00"
internal const val ZERO = "0"
internal const val PRECISE = "100.123456789012345678"
internal const val NEG_PRECISE = "-100.123456789012345678"
internal const val CREATED_Q2 = "2026-06-01T12:00:00Z"
internal const val CREATED_Q3 = "2026-08-15T12:00:00Z"

private fun sha256Hex(value: String): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }

internal val KEY_HASH = sha256Hex("fincore-idempotency-key")
internal val REQUEST_HASH = sha256Hex("fincore-request-body")
internal val MINIMAL_KEY_HASH = sha256Hex("fincore-minimal-key")
internal const val EXPIRES_AT = "2026-06-02T12:00:00Z"
internal const val AGGREGATE_TYPE = "Account"
internal const val EVENT_TYPE = "AccountOpened"
internal const val EMPTY_JSON = "{}"
internal const val PENDING_STATUS = "PENDING"
internal const val PERMANENTLY_FAILED_STATUS = "PERMANENTLY_FAILED"
internal val OUTBOX_STATUSES = listOf("PENDING", "PUBLISHING", "PUBLISHED", "FAILED", "PERMANENTLY_FAILED")

@Suppress("MagicNumber") // positional JDBC parameter indices
internal class LedgerSchemaTestDb(
    private val postgres: PostgreSQLContainer<*>,
) {
    fun open(): Connection = DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)

    fun migrate() {
        val connection = DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)
        val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
        Liquibase(CHANGELOG, ClassLoaderResourceAccessor(), database).use { liquibase ->
            liquibase.update(Contexts(), LabelExpression())
        }
    }

    fun insertAccount(
        connection: Connection,
        id: UUID,
        type: String = ASSET,
        currency: String = CCY,
    ) {
        connection
            .prepareStatement(
                "INSERT INTO ledger.accounts(id,name,type,currency,created_by,updated_by) VALUES (?,?,?,?,?,?)",
            ).use { statement ->
                statement.setObject(1, id)
                statement.setString(2, "acct")
                statement.setString(3, type)
                statement.setString(4, currency)
                statement.setString(5, ACTOR)
                statement.setString(6, ACTOR)
                statement.executeUpdate()
            }
    }

    fun insertTransaction(
        connection: Connection,
        id: UUID,
        reference: String,
        status: String = POSTED,
        reversesId: UUID? = null,
    ) {
        connection
            .prepareStatement(
                "INSERT INTO ledger.transactions(id,reference,status,reverses_id,created_by) VALUES (?,?,?,?,?)",
            ).use { statement ->
                statement.setObject(1, id)
                statement.setString(2, reference)
                statement.setString(3, status)
                statement.setObject(4, reversesId)
                statement.setString(5, ACTOR)
                statement.executeUpdate()
            }
    }

    fun insertEntry(
        connection: Connection,
        txId: UUID,
        accountId: UUID,
        amount: String,
        direction: String,
        id: UUID = UUID.randomUUID(),
        createdAt: String = CREATED_Q2,
    ) {
        connection
            .prepareStatement(
                "INSERT INTO ledger.entries(id,transaction_id,account_id,amount,currency,direction,created_at) " +
                    "VALUES (?,?,?,?::numeric,?,?,?::timestamptz)",
            ).use { statement ->
                statement.setObject(1, id)
                statement.setObject(2, txId)
                statement.setObject(3, accountId)
                statement.setString(4, amount)
                statement.setString(5, CCY)
                statement.setString(6, direction)
                statement.setString(7, createdAt)
                statement.executeUpdate()
            }
    }

    fun insertEntryPair(
        connection: Connection,
        txId: UUID,
        accountId: UUID,
        debit: String,
        credit: String,
    ) {
        connection
            .prepareStatement(
                "INSERT INTO ledger.entries(id,transaction_id,account_id,amount,currency,direction,created_at) " +
                    "VALUES (?,?,?,?::numeric,?,?,?::timestamptz),(?,?,?,?::numeric,?,?,?::timestamptz)",
            ).use { statement ->
                bindEntry(statement, 0, txId, accountId, debit, DEBIT)
                bindEntry(statement, 7, txId, accountId, credit, CREDIT)
                statement.executeUpdate()
            }
    }

    private fun bindEntry(
        statement: PreparedStatement,
        base: Int,
        txId: UUID,
        accountId: UUID,
        amount: String,
        direction: String,
    ) {
        statement.setObject(base + 1, UUID.randomUUID())
        statement.setObject(base + 2, txId)
        statement.setObject(base + 3, accountId)
        statement.setString(base + 4, amount)
        statement.setString(base + 5, CCY)
        statement.setString(base + 6, direction)
        statement.setString(base + 7, CREATED_Q2)
    }

    fun insertAccountBalance(
        connection: Connection,
        accountId: UUID,
        currency: String = CCY,
        balance: String = ZERO,
        lastPostedAt: String = CREATED_Q2,
    ) {
        connection
            .prepareStatement(
                "INSERT INTO ledger.account_balances(account_id,currency,balance,last_posted_at) " +
                    "VALUES (?,?,?::numeric,?::timestamptz)",
            ).use { statement ->
                statement.setObject(1, accountId)
                statement.setString(2, currency)
                statement.setString(3, balance)
                statement.setString(4, lastPostedAt)
                statement.executeUpdate()
            }
    }

    fun insertAccountBalanceMinimal(
        connection: Connection,
        accountId: UUID,
        currency: String = CCY,
        lastPostedAt: String = CREATED_Q2,
    ) {
        connection
            .prepareStatement(
                "INSERT INTO ledger.account_balances(account_id,currency,last_posted_at) VALUES (?,?,?::timestamptz)",
            ).use { statement ->
                statement.setObject(1, accountId)
                statement.setString(2, currency)
                statement.setString(3, lastPostedAt)
                statement.executeUpdate()
            }
    }

    fun insertIdempotencyKey(
        connection: Connection,
        keyHash: String = KEY_HASH,
        requestHash: String = REQUEST_HASH,
        expiresAt: String = EXPIRES_AT,
    ) {
        connection
            .prepareStatement(
                "INSERT INTO platform.idempotency_keys(key_hash,request_hash,expires_at) " +
                    "VALUES (?,?,?::timestamptz)",
            ).use { statement ->
                statement.setString(1, keyHash)
                statement.setString(2, requestHash)
                statement.setString(3, expiresAt)
                statement.executeUpdate()
            }
    }

    fun insertOutboxEvent(
        connection: Connection,
        aggregateId: String,
        status: String = PENDING_STATUS,
    ) {
        connection
            .prepareStatement(
                "INSERT INTO platform.outbox_events(aggregate_type,aggregate_id,event_type,payload,status) " +
                    "VALUES (?,?,?,?::jsonb,?)",
            ).use { statement ->
                statement.setString(1, AGGREGATE_TYPE)
                statement.setString(2, aggregateId)
                statement.setString(3, EVENT_TYPE)
                statement.setString(4, EMPTY_JSON)
                statement.setString(5, status)
                statement.executeUpdate()
            }
    }

    fun insertOutboxEventMinimal(
        connection: Connection,
        aggregateId: String,
    ) {
        connection
            .prepareStatement(
                "INSERT INTO platform.outbox_events(aggregate_type,aggregate_id,event_type,payload) " +
                    "VALUES (?,?,?,?::jsonb)",
            ).use { statement ->
                statement.setString(1, AGGREGATE_TYPE)
                statement.setString(2, aggregateId)
                statement.setString(3, EVENT_TYPE)
                statement.setString(4, EMPTY_JSON)
                statement.executeUpdate()
            }
    }

    fun entryCount(txId: UUID): Int =
        open().use { connection ->
            connection.prepareStatement("SELECT count(*) FROM ledger.entries WHERE transaction_id = ?").use { statement ->
                statement.setObject(1, txId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }

    fun bigDecimalById(
        sql: String,
        id: UUID,
    ): BigDecimal =
        open().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, id)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getBigDecimal(1)
                }
            }
        }

    fun stringById(
        sql: String,
        id: UUID,
    ): String =
        open().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, id)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getString(1)
                }
            }
        }

    fun boolQuery(sql: String): Boolean =
        open().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { resultSet ->
                    resultSet.next()
                    resultSet.getBoolean(1)
                }
            }
        }

    fun intQuery(sql: String): Int =
        open().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }
}
