// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.audit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fincore.ledger.application.AuditRecord
import com.fincore.ledger.application.AuditTrailWriter
import com.fincore.ledger.domain.enum.AuditAction
import com.fincore.ledger.domain.enum.AuditResourceType
import com.fincore.ledger.domain.enum.AuditResult
import com.fincore.ledger.infrastructure.persistence.AuditEventRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestComponent
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ExtendWith(PostgresContainerExtension::class)
@Import(
    AuditTrailWriterImpl::class,
    AuditTrailWriterIT.JacksonConfig::class,
    AuditTrailWriterIT.WriteHelper::class,
)
class AuditTrailWriterIT(
    @Autowired private val writer: AuditTrailWriter,
    @Autowired private val auditRepository: AuditEventRepository,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val helper: AuditTrailWriterIT.WriteHelper,
) {
    @TestConfiguration
    class JacksonConfig {
        @Bean
        fun objectMapper(): ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    @TestComponent
    class WriteHelper(
        @Autowired private val writer: AuditTrailWriter,
    ) {
        @Transactional
        fun commit(resourceId: String) {
            writer.record(record(AuditAction.ACCOUNT_CREATE, resourceId, null))
        }

        @Transactional
        fun rollback(resourceId: String) {
            writer.record(record(AuditAction.ACCOUNT_CREATE, resourceId, null))
            throw RuntimeException("forced rollback")
        }

        @Transactional
        fun commitWithPayload(
            resourceId: String,
            payload: Map<String, String>,
        ) {
            writer.record(record(AuditAction.ACCOUNT_STATUS_CHANGE, resourceId, payload))
        }

        private fun record(
            action: AuditAction,
            resourceId: String,
            payload: Map<String, String>?,
        ) = AuditRecord(
            actorId = "auth0|test",
            action = action,
            resourceType = AuditResourceType.ACCOUNT,
            resourceId = resourceId,
            requestHash = null,
            payload = payload,
        )
    }

    @AfterEach
    fun cleanUp() {
        MDC.clear()
    }

    private fun rowFor(resourceId: String) = auditRepository.findAll().filter { it.resourceId == resourceId }

    @Test
    fun `should commit one audit row when the surrounding transaction commits`() {
        helper.commit("acc-commit-1")

        val rows = rowFor("acc-commit-1")
        rows.size shouldBe 1
        rows.first().result shouldBe AuditResult.SUCCESS
        rows.first().action shouldBe AuditAction.ACCOUNT_CREATE.name
    }

    @Test
    fun `should leave no audit row when the surrounding transaction rolls back`() {
        shouldThrow<RuntimeException> {
            helper.rollback("acc-rollback-1")
        }

        rowFor("acc-rollback-1").size shouldBe 0
    }

    @Test
    fun `should throw IllegalStateException when record is called with no active transaction`() {
        shouldThrow<IllegalStateException> {
            writer.record(
                AuditRecord(
                    actorId = "auth0|test",
                    action = AuditAction.ACCOUNT_CREATE,
                    resourceType = AuditResourceType.ACCOUNT,
                    resourceId = "acc-no-tx",
                    requestHash = null,
                ),
            )
        }

        rowFor("acc-no-tx").size shouldBe 0
    }

    @Test
    fun `should store status payload when action is ACCOUNT_STATUS_CHANGE`() {
        helper.commitWithPayload("acc-status-1", mapOf("status" to "FROZEN"))

        val rows = rowFor("acc-status-1")
        rows.size shouldBe 1
        val payloadJson = rows.first().payload.shouldNotBeNull()
        objectMapper.readTree(payloadJson).get("status").asText() shouldBe "FROZEN"
    }

    @Test
    fun `should populate correlationId from MDC when present`() {
        MDC.put("correlation_id", "corr-it-001")
        helper.commit("acc-corr-1")

        val rows = rowFor("acc-corr-1")
        rows.size shouldBe 1
        rows.first().correlationId shouldBe "corr-it-001"
    }

    @Test
    fun `should generate a non-blank correlationId when MDC is empty`() {
        MDC.clear()
        helper.commit("acc-corr-2")

        val rows = rowFor("acc-corr-2")
        rows.size shouldBe 1
        rows.first().correlationId.shouldNotBeBlank()
    }

    @Test
    fun `should store null requestHash for operations without a request body`() {
        helper.commitWithPayload("acc-hash-null", mapOf("status" to "CLOSED"))

        val rows = rowFor("acc-hash-null")
        rows.size shouldBe 1
        rows.first().requestHash.shouldBeNull()
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
