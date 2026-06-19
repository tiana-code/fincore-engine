// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.persistence

import com.fincore.compliance.domain.enum.AmlAlertStatus
import com.fincore.compliance.domain.enum.CaseStatus
import com.fincore.compliance.domain.enum.KycStatus
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ExtendWith(PostgresContainerExtension::class)
class ComplianceSchemaPersistenceIT(
    @Autowired private val kycSessions: KycSessionRepository,
    @Autowired private val amlAlerts: AmlAlertRepository,
    @Autowired private val cases: ComplianceCaseRepository,
    @Autowired private val amlRules: AmlRuleRepository,
) {
    @AfterEach
    fun cleanUp() {
        kycSessions.deleteAll()
        amlAlerts.deleteAll()
        cases.deleteAll()
        amlRules.deleteAll()
    }

    @Test
    fun `should round-trip a kyc session preserving status and version`() {
        val id = UUID.randomUUID()
        kycSessions.saveAndFlush(KycSessionEntity(id, "subject-1", KycStatus.INITIATED, Instant.now(), 0L))

        val reloaded = kycSessions.findById(id).orElseThrow()
        reloaded.subjectReference shouldBe "subject-1"
        reloaded.status shouldBe KycStatus.INITIATED
        reloaded.version shouldBe 0L
    }

    @Test
    fun `should find aml alerts by status`() {
        amlAlerts.saveAndFlush(
            AmlAlertEntity(UUID.randomUUID(), "aml.velocity", "subject-1", AmlAlertStatus.OPEN, Instant.now(), 0L),
        )

        amlAlerts.findByStatus(AmlAlertStatus.OPEN).size shouldBe 1
    }

    @Test
    fun `should round-trip a compliance case preserving status`() {
        val id = UUID.randomUUID()
        cases.saveAndFlush(ComplianceCaseEntity(id, "case-ref-1", CaseStatus.OPEN, Instant.now(), 0L))

        cases.findById(id).orElseThrow().status shouldBe CaseStatus.OPEN
    }

    @Test
    fun `should round-trip an aml rule preserving the enabled flag`() {
        val id = UUID.randomUUID()
        amlRules.saveAndFlush(AmlRuleEntity(id, "aml.structuring", false, Instant.now()))

        val reloaded = amlRules.findById(id).orElseThrow()
        reloaded.enabled shouldBe false
        amlRules.findByRuleKey("aml.structuring")?.id shouldBe id
    }

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
