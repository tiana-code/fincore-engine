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
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(PostgresContainerExtension::class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DecisionRuleStoragePersistenceIT(
    @Autowired private val ruleRepository: DecisionRuleRepository,
    @Autowired private val versionRepository: RuleVersionRepository,
) {
    private val mapper = ObjectMapper()
    private val sampleDsl = """{"condition":{"attr":"amount","op":"gte","value":100},"outcome":{"label":"approve"}}"""

    @Test
    fun `should round trip a rule and its active version when persisted`() {
        val rule = ruleRepository.save(newRule())
        ruleRepository.findByRuleKey(rule.ruleKey).shouldNotBeNull().activeVersionId shouldBe null

        val version = versionRepository.save(newVersion(rule.id, 1))
        val loadedVersion = versionRepository.findByRuleId(rule.id).single()
        mapper.readTree(loadedVersion.dsl) shouldBe mapper.readTree(sampleDsl)
        loadedVersion.versionNo shouldBe 1

        rule.activeVersionId = version.id
        ruleRepository.save(rule)
        ruleRepository.findById(rule.id).orElseThrow().activeVersionId shouldBe version.id
    }

    @Test
    fun `should reject a duplicate version number for the same rule`() {
        val rule = ruleRepository.save(newRule())
        versionRepository.save(newVersion(rule.id, 1))
        shouldThrow<DataIntegrityViolationException> {
            versionRepository.save(newVersion(rule.id, 1))
        }
    }

    @Test
    fun `should reject update and delete on a persisted rule version`() {
        val rule = ruleRepository.save(newRule())
        val versionId = versionRepository.save(newVersion(rule.id, 1)).id
        DriverManager
            .getConnection(
                PostgresContainerExtension.jdbcUrl,
                PostgresContainerExtension.username,
                PostgresContainerExtension.password,
            ).use { connection ->
                connection
                    .prepareStatement("UPDATE decision.rule_versions SET version_no = version_no + 1 WHERE id = ?")
                    .use { statement ->
                        statement.setObject(1, versionId)
                        shouldThrow<SQLException> { statement.executeUpdate() }.sqlState shouldBe "0A000"
                    }
                connection.prepareStatement("DELETE FROM decision.rule_versions WHERE id = ?").use { statement ->
                    statement.setObject(1, versionId)
                    shouldThrow<SQLException> { statement.executeUpdate() }.sqlState shouldBe "0A000"
                }
            }
    }

    @Test
    fun `should raise optimistic lock failure on a stale rule update`() {
        val saved = ruleRepository.save(newRule())
        val stale = ruleRepository.findById(saved.id).orElseThrow()

        bumpRuleVersion(saved.id)

        stale.activeVersionId = UUID.randomUUID()
        shouldThrow<ObjectOptimisticLockingFailureException> { ruleRepository.save(stale) }
    }

    private fun bumpRuleVersion(ruleId: UUID) {
        DriverManager
            .getConnection(
                PostgresContainerExtension.jdbcUrl,
                PostgresContainerExtension.username,
                PostgresContainerExtension.password,
            ).use { connection ->
                connection.prepareStatement("UPDATE decision.decision_rules SET version = version + 1 WHERE id = ?").use { statement ->
                    statement.setObject(1, ruleId)
                    statement.executeUpdate()
                }
            }
    }

    @Test
    fun `should report the max version number for a rule`() {
        val rule = ruleRepository.save(newRule())
        versionRepository.save(newVersion(rule.id, 1))
        versionRepository.save(newVersion(rule.id, 2))
        versionRepository.findMaxVersionNo(rule.id) shouldBe 2
    }

    private fun newRule(): DecisionRuleEntity = DecisionRuleEntity(id = UUID.randomUUID(), ruleKey = "rule-${UUID.randomUUID()}")

    private fun newVersion(
        ruleId: UUID,
        versionNo: Int,
    ): RuleVersionEntity = RuleVersionEntity(id = UUID.randomUUID(), ruleId = ruleId, versionNo = versionNo, dsl = sampleDsl)

    companion object {
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
