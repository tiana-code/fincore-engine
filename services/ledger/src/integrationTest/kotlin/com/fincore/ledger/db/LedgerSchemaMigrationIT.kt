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

    @Test
    fun `should create account_balances table with primary key foreign key and currency check`() {
        testDb.boolQuery("SELECT to_regclass('ledger.account_balances') IS NOT NULL") shouldBe true
        testDb.intQuery(
            "SELECT count(*) FROM pg_constraint " +
                "WHERE conrelid = 'ledger.account_balances'::regclass " +
                "AND conname IN ('pk_account_balances','fk_account_balances_account','ck_account_balances_currency')",
        ) shouldBe 3
    }

    @Test
    fun `should prevent deleting an account referenced by a balance row`() {
        val accountId = UUID.randomUUID()
        testDb.open().use { connection ->
            testDb.insertAccount(connection, accountId)
            testDb.insertAccountBalance(connection, accountId)
            shouldThrow<SQLException> {
                connection.prepareStatement("DELETE FROM ledger.accounts WHERE id = ?").use { statement ->
                    statement.setObject(1, accountId)
                    statement.executeUpdate()
                }
            }
        }
    }

    @Test
    fun `should reject a balance row with an invalid currency`() {
        val accountId = UUID.randomUUID()
        testDb.open().use { connection ->
            testDb.insertAccount(connection, accountId)
            shouldThrow<SQLException> { testDb.insertAccountBalance(connection, accountId, currency = "usd") }
        }
    }

    @Test
    fun `should reject a duplicate balance row for the same account and currency`() {
        val accountId = UUID.randomUUID()
        testDb.open().use { connection ->
            testDb.insertAccount(connection, accountId)
            testDb.insertAccountBalance(connection, accountId)
            shouldThrow<SQLException> { testDb.insertAccountBalance(connection, accountId) }
        }
    }

    @Test
    fun `should store and read back 18 digit balance precision`() {
        val accountId = UUID.randomUUID()
        testDb.open().use { connection ->
            testDb.insertAccount(connection, accountId)
            testDb.insertAccountBalance(connection, accountId, balance = PRECISE)
        }
        val stored =
            testDb.bigDecimalById(
                "SELECT balance FROM ledger.account_balances WHERE account_id = ?",
                accountId,
            )
        (stored.compareTo(BigDecimal(PRECISE)) == 0) shouldBe true
    }

    @Test
    fun `should default balance and version to zero when only required columns are supplied`() {
        val accountId = UUID.randomUUID()
        testDb.open().use { connection ->
            testDb.insertAccount(connection, accountId)
            testDb.insertAccountBalanceMinimal(connection, accountId)
        }
        val balance =
            testDb.bigDecimalById(
                "SELECT balance FROM ledger.account_balances WHERE account_id = ?",
                accountId,
            )
        val version =
            testDb.bigDecimalById(
                "SELECT version::numeric FROM ledger.account_balances WHERE account_id = ?",
                accountId,
            )
        (balance.compareTo(BigDecimal.ZERO) == 0) shouldBe true
        (version.compareTo(BigDecimal.ZERO) == 0) shouldBe true
    }

    @Test
    fun `should create idempotency_keys table with primary key and expires index`() {
        testDb.boolQuery("SELECT to_regclass('platform.idempotency_keys') IS NOT NULL") shouldBe true
        testDb.intQuery(
            "SELECT count(*) FROM pg_constraint " +
                "WHERE conrelid = 'platform.idempotency_keys'::regclass AND conname = 'pk_idempotency_keys'",
        ) shouldBe 1
        testDb.boolQuery("SELECT to_regclass('platform.idx_idempotency_keys_expires') IS NOT NULL") shouldBe true
    }

    @Test
    fun `should reject a duplicate idempotency key hash`() {
        testDb.open().use { connection ->
            testDb.insertIdempotencyKey(connection, keyHash = REQUEST_HASH)
            shouldThrow<SQLException> { testDb.insertIdempotencyKey(connection, keyHash = REQUEST_HASH) }
        }
    }

    @Test
    fun `should default created_at and leave optional columns null on minimal idempotency insert`() {
        testDb.open().use { connection -> testDb.insertIdempotencyKey(connection, keyHash = MINIMAL_KEY_HASH) }
        testDb.boolQuery(
            "SELECT created_at IS NOT NULL AND status_code IS NULL AND response_body IS NULL " +
                "FROM platform.idempotency_keys WHERE key_hash = '$MINIMAL_KEY_HASH'",
        ) shouldBe true
    }

    @Test
    fun `should create outbox_events table with primary key status check and dispatcher indexes`() {
        testDb.boolQuery("SELECT to_regclass('platform.outbox_events') IS NOT NULL") shouldBe true
        testDb.intQuery(
            "SELECT count(*) FROM pg_constraint " +
                "WHERE conrelid = 'platform.outbox_events'::regclass " +
                "AND conname IN ('pk_outbox_events','ck_outbox_events_status')",
        ) shouldBe 2
        testDb.boolQuery("SELECT to_regclass('platform.idx_outbox_events_pending') IS NOT NULL") shouldBe true
        testDb.boolQuery("SELECT to_regclass('platform.idx_outbox_events_aggregate') IS NOT NULL") shouldBe true
        testDb.boolQuery(
            "SELECT indexdef LIKE '%WHERE%' FROM pg_indexes " +
                "WHERE schemaname = 'platform' AND indexname = 'idx_outbox_events_pending'",
        ) shouldBe true
    }

    @Test
    fun `should accept every outbox status value defined by the enum`() {
        testDb.open().use { connection ->
            OUTBOX_STATUSES.forEach { status ->
                shouldNotThrowAny { testDb.insertOutboxEvent(connection, aggregateId = "ac5-$status", status = status) }
            }
        }
    }

    @Test
    fun `should reject an unknown outbox status value`() {
        testDb.open().use { connection ->
            shouldThrow<SQLException> { testDb.insertOutboxEvent(connection, aggregateId = "ac6", status = "BOGUS") }
        }
    }

    @Test
    fun `should store the longest outbox status without truncation`() {
        testDb.open().use { connection ->
            testDb.insertOutboxEvent(connection, aggregateId = "ac7", status = PERMANENTLY_FAILED_STATUS)
        }
        testDb.boolQuery(
            "SELECT EXISTS(SELECT 1 FROM platform.outbox_events " +
                "WHERE aggregate_id = 'ac7' AND status = 'PERMANENTLY_FAILED')",
        ) shouldBe true
    }

    @Test
    fun `should generate an outbox id when none is supplied`() {
        testDb.open().use { connection -> testDb.insertOutboxEventMinimal(connection, aggregateId = "ac8") }
        testDb.intQuery(
            "SELECT count(*) FROM platform.outbox_events WHERE aggregate_id = 'ac8' AND id IS NOT NULL",
        ) shouldBe 1
    }

    @Test
    fun `should default outbox status attempts and created_at on minimal insert`() {
        testDb.open().use { connection -> testDb.insertOutboxEventMinimal(connection, aggregateId = "ac9") }
        testDb.boolQuery(
            "SELECT status = 'PENDING' AND attempts = 0 AND created_at IS NOT NULL " +
                "AND last_error IS NULL AND published_at IS NULL " +
                "FROM platform.outbox_events WHERE aggregate_id = 'ac9'",
        ) shouldBe true
    }

    @Test
    fun `should not define a version column on outbox_events`() {
        testDb.intQuery(
            "SELECT count(*) FROM information_schema.columns " +
                "WHERE table_schema = 'platform' AND table_name = 'outbox_events' AND column_name = 'version'",
        ) shouldBe 0
    }

    @Test
    fun `should create audit_events table with expected columns widths nullability primary key and result check`() {
        testDb.boolQuery("SELECT to_regclass('platform.audit_events') IS NOT NULL") shouldBe true
        testDb.intQuery(
            "SELECT count(*) FROM information_schema.columns " +
                "WHERE table_schema = 'platform' AND table_name = 'audit_events' " +
                "AND column_name IN " +
                "('id','actor_id','correlation_id','action','resource_type','resource_id','result','request_hash','created_at')",
        ) shouldBe 9
        testDb.boolQuery(
            "SELECT character_maximum_length = 16 FROM information_schema.columns " +
                "WHERE table_schema = 'platform' AND table_name = 'audit_events' AND column_name = 'result'",
        ) shouldBe true
        testDb.boolQuery(
            "SELECT character_maximum_length = 32 FROM information_schema.columns " +
                "WHERE table_schema = 'platform' AND table_name = 'audit_events' AND column_name = 'resource_type'",
        ) shouldBe true
        testDb.boolQuery(
            "SELECT character_maximum_length = 64 FROM information_schema.columns " +
                "WHERE table_schema = 'platform' AND table_name = 'audit_events' AND column_name = 'actor_id'",
        ) shouldBe true
        testDb.boolQuery(
            "SELECT is_nullable = 'YES' FROM information_schema.columns " +
                "WHERE table_schema = 'platform' AND table_name = 'audit_events' AND column_name = 'request_hash'",
        ) shouldBe true
        testDb.intQuery(
            "SELECT count(*) FROM information_schema.columns " +
                "WHERE table_schema = 'platform' AND table_name = 'audit_events' " +
                "AND is_nullable = 'NO' AND column_name <> 'id'",
        ) shouldBe 7
        testDb.intQuery(
            "SELECT count(*) FROM pg_constraint " +
                "WHERE conrelid = 'platform.audit_events'::regclass " +
                "AND conname IN ('pk_audit_events','ck_audit_events_result')",
        ) shouldBe 2
    }

    @Test
    fun `should accept every audit result value defined by the check`() {
        testDb.open().use { connection ->
            AUDIT_RESULTS.forEach { result ->
                shouldNotThrowAny { testDb.insertAuditEvent(connection, result = result, resourceId = "tx_ac2_$result") }
            }
        }
    }

    @Test
    fun `should reject an unknown audit result value`() {
        testDb.open().use { connection ->
            shouldThrow<SQLException> { testDb.insertAuditEvent(connection, result = "UNKNOWN", resourceId = "tx_ac3") }
        }
    }

    @Test
    fun `should generate an audit id when none is supplied`() {
        testDb.open().use { connection -> testDb.insertAuditEventMinimal(connection, resourceId = "tx_ac4") }
        testDb.intQuery(
            "SELECT count(*) FROM platform.audit_events WHERE resource_id = 'tx_ac4' AND id IS NOT NULL",
        ) shouldBe 1
    }

    @Test
    fun `should default created_at and leave request_hash null on minimal audit insert`() {
        testDb.open().use { connection -> testDb.insertAuditEventMinimal(connection, resourceId = "tx_ac5") }
        testDb.boolQuery(
            "SELECT created_at IS NOT NULL AND request_hash IS NULL " +
                "FROM platform.audit_events WHERE resource_id = 'tx_ac5'",
        ) shouldBe true
    }

    @Test
    fun `should create the resource history index on audit_events`() {
        testDb.boolQuery("SELECT to_regclass('platform.idx_audit_events_resource') IS NOT NULL") shouldBe true
        testDb.boolQuery(
            "SELECT indexdef LIKE '%resource_type%resource_id%created_at%' FROM pg_indexes " +
                "WHERE schemaname = 'platform' AND indexname = 'idx_audit_events_resource'",
        ) shouldBe true
    }

    @Test
    fun `should create the actor index on audit_events`() {
        testDb.boolQuery("SELECT to_regclass('platform.idx_audit_events_actor') IS NOT NULL") shouldBe true
        testDb.boolQuery(
            "SELECT indexdef LIKE '%actor_id%' FROM pg_indexes " +
                "WHERE schemaname = 'platform' AND indexname = 'idx_audit_events_actor'",
        ) shouldBe true
    }

    @Test
    fun `should not define a version column on audit_events`() {
        testDb.intQuery(
            "SELECT count(*) FROM information_schema.columns " +
                "WHERE table_schema = 'platform' AND table_name = 'audit_events' AND column_name = 'version'",
        ) shouldBe 0
    }

    @Test
    fun `should reject an update on audit_events`() {
        testDb.open().use { connection ->
            testDb.insertAuditEvent(connection, result = "SUCCESS", resourceId = "tx_immutable_upd")
            shouldThrow<SQLException> {
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        "UPDATE platform.audit_events SET result = 'DENIED' WHERE resource_id = 'tx_immutable_upd'",
                    )
                }
            }
        }
    }

    @Test
    fun `should reject a delete on audit_events`() {
        testDb.open().use { connection ->
            testDb.insertAuditEvent(connection, result = "SUCCESS", resourceId = "tx_immutable_del")
            shouldThrow<SQLException> {
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        "DELETE FROM platform.audit_events WHERE resource_id = 'tx_immutable_del'",
                    )
                }
            }
        }
    }

    @Test
    fun `should add a nullable jsonb payload column on audit_events`() {
        testDb.boolQuery(
            "SELECT data_type = 'jsonb' AND is_nullable = 'YES' FROM information_schema.columns " +
                "WHERE table_schema = 'platform' AND table_name = 'audit_events' AND column_name = 'payload'",
        ) shouldBe true
    }
}
