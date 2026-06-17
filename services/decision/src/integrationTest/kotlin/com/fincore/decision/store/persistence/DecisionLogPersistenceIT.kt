// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.Instant
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(PostgresContainerExtension::class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DecisionLogPersistenceIT(
    @Autowired private val ruleRepository: DecisionRuleRepository,
    @Autowired private val versionRepository: RuleVersionRepository,
    @Autowired private val logRepository: DecisionLogRepository,
) {
    private val mapper = ObjectMapper()
    private val seededInstant = Instant.parse("2026-06-15T12:00:00Z")
    private val unseededInstant = Instant.parse("2030-03-10T08:00:00Z")
    private val sampleDsl = """{"condition":{"attr":"amount","op":"gte","value":100},"outcome":{"label":"approve"}}"""

    @Test
    fun `should round trip a matched decision log when persisted`() {
        val versionId = seedRuleVersion()
        val reasonCodes = """["LOW_RISK","KNOWN_CUSTOMER"]"""
        val trace = """[{"node":"comparison","attr":"amount","result":true}]"""
        val saved =
            logRepository.save(
                matchedLog(versionId, outcomeLabel = "approve", reasonCodes = reasonCodes, trace = trace),
            )

        val loaded = logRepository.findById(saved.id).orElseThrow()
        loaded.matched shouldBe true
        loaded.outcomeLabel shouldBe "approve"
        mapper.readTree(loaded.reasonCodes) shouldBe mapper.readTree(reasonCodes)
        mapper.readTree(loaded.trace) shouldBe mapper.readTree(trace)
        loaded.createdAt.shouldNotBeNull()
    }

    @Test
    fun `should store a non matching decision with null outcome when persisted`() {
        val versionId = seedRuleVersion()
        val trace = """[{"node":"comparison","attr":"amount","result":false}]"""
        val saved = logRepository.save(unmatchedLog(versionId, trace = trace))

        val loaded = logRepository.findById(saved.id).orElseThrow()
        loaded.matched shouldBe false
        loaded.outcomeLabel shouldBe null
        loaded.reasonCodes shouldBe null
        mapper.readTree(loaded.trace) shouldBe mapper.readTree(trace)
        reasonCodesIsSqlNull(saved.id) shouldBe true
    }

    @Test
    fun `should reject update and delete on a persisted decision log`() {
        val versionId = seedRuleVersion()
        val named = logRepository.save(matchedLog(versionId, evaluatedAt = seededInstant))
        val default = logRepository.save(matchedLog(versionId, evaluatedAt = unseededInstant))

        withConnection { connection ->
            assertImmutable(connection, named.id)
            assertImmutable(connection, default.id)
        }
    }

    @Test
    fun `should reject a non matching decision that carries an outcome label`() {
        val versionId = seedRuleVersion()
        withConnection { connection ->
            shouldThrow<SQLException> {
                insertRaw(connection, versionId, matched = false, outcomeLabel = "approve")
            }.sqlState shouldBe CHECK_VIOLATION
            shouldThrow<SQLException> {
                insertRaw(connection, versionId, matched = true, outcomeLabel = null)
            }.sqlState shouldBe CHECK_VIOLATION
        }
    }

    @Test
    fun `should reject an input hash that is not sha256 hex`() {
        val versionId = seedRuleVersion()
        withConnection { connection ->
            shouldThrow<SQLException> {
                insertRaw(connection, versionId, matched = true, outcomeLabel = "approve", inputHash = "g".repeat(SHA256_HEX_LENGTH))
            }.sqlState shouldBe CHECK_VIOLATION
        }
    }

    @Test
    fun `should store a decision whose month has no seeded partition`() {
        val versionId = seedRuleVersion()
        val hash = sha256Hex("unseeded-${UUID.randomUUID()}")
        logRepository.save(matchedLog(versionId, evaluatedAt = unseededInstant, inputHash = hash))

        logRepository.findByInputHash(hash).single().evaluatedAt shouldBe unseededInstant
    }

    private fun seedRuleVersion(): UUID {
        val rule = ruleRepository.save(DecisionRuleEntity(id = UUID.randomUUID(), ruleKey = "rule-${UUID.randomUUID()}"))
        return versionRepository.save(RuleVersionEntity(id = UUID.randomUUID(), ruleId = rule.id, versionNo = 1, dsl = sampleDsl)).id
    }

    private fun matchedLog(
        versionId: UUID,
        evaluatedAt: Instant = seededInstant,
        outcomeLabel: String = "approve",
        reasonCodes: String? = null,
        trace: String = "[]",
        inputHash: String = sha256Hex(UUID.randomUUID().toString()),
    ): DecisionLogEntity =
        DecisionLogEntity(
            id = UUID.randomUUID(),
            evaluatedAt = evaluatedAt,
            ruleVersionId = versionId,
            inputHash = inputHash,
            matched = true,
            outcomeLabel = outcomeLabel,
            reasonCodes = reasonCodes,
            trace = trace,
        )

    private fun unmatchedLog(
        versionId: UUID,
        trace: String = "[]",
    ): DecisionLogEntity =
        DecisionLogEntity(
            id = UUID.randomUUID(),
            evaluatedAt = seededInstant,
            ruleVersionId = versionId,
            inputHash = sha256Hex(UUID.randomUUID().toString()),
            matched = false,
            trace = trace,
        )

    private fun assertImmutable(
        connection: Connection,
        id: UUID,
    ) {
        connection.prepareStatement("UPDATE decision.decision_logs SET input_hash = input_hash WHERE id = ?").use { statement ->
            statement.setObject(1, id)
            shouldThrow<SQLException> { statement.executeUpdate() }.sqlState shouldBe APPEND_ONLY
        }
        connection.prepareStatement("DELETE FROM decision.decision_logs WHERE id = ?").use { statement ->
            statement.setObject(1, id)
            shouldThrow<SQLException> { statement.executeUpdate() }.sqlState shouldBe APPEND_ONLY
        }
    }

    @Suppress("MagicNumber") // positional JDBC parameter indices
    private fun insertRaw(
        connection: Connection,
        versionId: UUID,
        matched: Boolean,
        outcomeLabel: String?,
        inputHash: String = sha256Hex(UUID.randomUUID().toString()),
    ) {
        val sql =
            "INSERT INTO decision.decision_logs " +
                "(evaluated_at, rule_version_id, input_hash, matched, outcome_label, trace) " +
                "VALUES (?::timestamptz, ?, ?, ?, ?, ?::jsonb)"
        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, seededInstant.toString())
            statement.setObject(2, versionId)
            statement.setString(3, inputHash)
            statement.setBoolean(4, matched)
            statement.setString(5, outcomeLabel)
            statement.setString(6, "[]")
            statement.executeUpdate()
        }
    }

    private fun reasonCodesIsSqlNull(id: UUID): Boolean =
        withConnection { connection ->
            connection.prepareStatement("SELECT reason_codes IS NULL FROM decision.decision_logs WHERE id = ?").use { statement ->
                statement.setObject(1, id)
                statement.executeQuery().use { rs ->
                    check(rs.next()) { "no decision log row for id=$id" }
                    rs.getBoolean(1)
                }
            }
        }

    private fun <T> withConnection(block: (Connection) -> T): T =
        DriverManager
            .getConnection(
                PostgresContainerExtension.jdbcUrl,
                PostgresContainerExtension.username,
                PostgresContainerExtension.password,
            ).use { connection ->
                connection.autoCommit = true
                block(connection)
            }

    companion object {
        private const val APPEND_ONLY = "0A000"
        private const val CHECK_VIOLATION = "23514"
        private const val SHA256_HEX_LENGTH = 64

        private fun sha256Hex(value: String): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(value.toByteArray())
                .joinToString("") { "%02x".format(it) }

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
