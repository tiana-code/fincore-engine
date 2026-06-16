// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.core.IdempotencyKey
import com.fincore.ledger.api.observability.CorrelationIdAttributes
import com.fincore.ledger.config.IdempotencyProperties
import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.enum.AuditAction
import com.fincore.ledger.domain.enum.AuditResourceType
import com.fincore.ledger.domain.enum.AuditResult
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.infrastructure.audit.AuditTrailWriterImpl
import com.fincore.ledger.infrastructure.outbox.OutboxEventPublisherImpl
import com.fincore.ledger.infrastructure.persistence.AccountPersistenceAdapter
import com.fincore.ledger.infrastructure.persistence.AuditEventRepository
import com.fincore.ledger.infrastructure.persistence.TransactionPersistenceAdapter
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.math.BigDecimal
import java.security.MessageDigest

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(PostgresContainerExtension::class)
@EnableConfigurationProperties(IdempotencyProperties::class)
@Import(
    AccountServiceImpl::class,
    AccountPersistenceAdapter::class,
    TransactionServiceImpl::class,
    TransactionPoster::class,
    TransactionPersistenceAdapter::class,
    BalanceServiceImpl::class,
    IdempotencyServiceImpl::class,
    IdempotencyStore::class,
    AuditTrailWriterImpl::class,
    OutboxEventPublisherImpl::class,
    AuditWritePathIT.JacksonConfig::class,
)
class AuditWritePathIT(
    @Autowired private val accountService: AccountService,
    @Autowired private val transactionService: TransactionService,
    @Autowired private val idempotencyService: IdempotencyService,
    @Autowired private val auditRepository: AuditEventRepository,
    @Autowired private val objectMapper: ObjectMapper,
) {
    @TestConfiguration
    class JacksonConfig {
        @Bean
        fun objectMapper(): ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    @AfterEach
    fun cleanUp() {
        MDC.clear()
    }

    private fun rowsFor(
        resourceId: String,
        action: AuditAction,
    ) = auditRepository.findAll().filter { it.resourceId == resourceId && it.action == action.name }

    private fun newAccount(): com.fincore.ledger.domain.Account =
        accountService.create(
            CreateAccountCommand("Test Account", AccountType.USER_WALLET, Currency.USD, ACTOR),
        )

    private fun postBalanced(
        reference: String,
        debit: AccountId,
        credit: AccountId,
        requestHash: String? = null,
    ): PostedTransaction =
        transactionService.post(
            PostTransactionCommand(
                reference = reference,
                description = null,
                currency = Currency.USD,
                entries =
                    listOf(
                        EntryLine(debit, EntryDirection.DEBIT, BigDecimal("100.00")),
                        EntryLine(credit, EntryDirection.CREDIT, BigDecimal("-100.00")),
                    ),
                actor = ACTOR,
                correlationId = CORR_ID,
                requestHash = requestHash,
            ),
        )

    // AC-1: one ACCOUNT_CREATE row with correct fields and request_hash
    @Test
    fun `should write exactly one ACCOUNT_CREATE audit row with correct fields when account create succeeds`() {
        MDC.put(CorrelationIdAttributes.MDC_KEY, CORR_ID)
        val requestBody = """{"name":"Test Account","type":"USER_WALLET","currency":"USD"}"""
        val expectedHash = sha256Hex(requestBody)

        val account =
            accountService.create(
                CreateAccountCommand(
                    name = "Test Account",
                    type = AccountType.USER_WALLET,
                    currency = Currency.USD,
                    actor = ACTOR,
                    requestHash = expectedHash,
                ),
            )

        val rows = rowsFor(account.id.toString(), AuditAction.ACCOUNT_CREATE)
        rows.size shouldBe 1
        val row = rows.first()
        row.action shouldBe AuditAction.ACCOUNT_CREATE.name
        row.resourceType shouldBe AuditResourceType.ACCOUNT.name
        row.resourceId shouldBe account.id.toString()
        row.result shouldBe AuditResult.SUCCESS
        row.actorId shouldBe ACTOR
        row.correlationId shouldBe CORR_ID
        row.requestHash.shouldNotBeNull() shouldHaveLength 64
        row.requestHash shouldBe expectedHash
    }

    // AC-2: ACCOUNT_STATUS_CHANGE row, request_hash NULL, payload has status
    @Test
    fun `should write exactly one ACCOUNT_STATUS_CHANGE row with status payload when changeStatus succeeds`() {
        val account = newAccount()

        accountService.changeStatus(account.id, AccountStatus.FROZEN, ACTOR)

        val rows = rowsFor(account.id.toString(), AuditAction.ACCOUNT_STATUS_CHANGE)
        rows.size shouldBe 1
        val row = rows.first()
        row.action shouldBe AuditAction.ACCOUNT_STATUS_CHANGE.name
        row.resourceId shouldBe account.id.toString()
        row.result shouldBe AuditResult.SUCCESS
        row.requestHash.shouldBeNull()
        row.correlationId.shouldNotBeBlank()
        val payloadJson = row.payload.shouldNotBeNull()
        val tree = objectMapper.readTree(payloadJson)
        tree.get("status").asText() shouldBe "FROZEN"
    }

    // AC-3: ACCOUNT_RENAME row, request_hash NULL
    @Test
    fun `should write exactly one ACCOUNT_RENAME row when rename succeeds`() {
        val account = newAccount()

        accountService.rename(account.id, "Renamed Account", ACTOR)

        val rows = rowsFor(account.id.toString(), AuditAction.ACCOUNT_RENAME)
        rows.size shouldBe 1
        val row = rows.first()
        row.action shouldBe AuditAction.ACCOUNT_RENAME.name
        row.resourceId shouldBe account.id.toString()
        row.result shouldBe AuditResult.SUCCESS
        row.requestHash.shouldBeNull()
        row.correlationId.shouldNotBeBlank()
    }

    // AC-4: TRANSACTION_POST row with request_hash
    @Test
    fun `should write exactly one TRANSACTION_POST audit row with request_hash when post succeeds`() {
        MDC.put(CorrelationIdAttributes.MDC_KEY, CORR_ID)
        val debit = newAccount()
        val credit = newAccount()

        val requestBody = """{"reference":"ref-audit-4","currency":"USD"}"""
        val expectedHash = sha256Hex(requestBody)
        val posted = postBalanced("ref-audit-4", debit.id, credit.id, requestHash = expectedHash)

        val rows = rowsFor(posted.id.toString(), AuditAction.TRANSACTION_POST)
        rows.size shouldBe 1
        val row = rows.first()
        row.action shouldBe AuditAction.TRANSACTION_POST.name
        row.resourceType shouldBe AuditResourceType.TRANSACTION.name
        row.resourceId shouldBe posted.id.toString()
        row.result shouldBe AuditResult.SUCCESS
        row.requestHash.shouldNotBeNull() shouldHaveLength 64
        row.requestHash shouldBe expectedHash
    }

    // AC-5: TRANSACTION_REVERSE with reason, resource_id = original tx id, payload has reason + compensating id
    @Test
    fun `should write TRANSACTION_REVERSE row with reason and compensating id when reverse with reason succeeds`() {
        val debit = newAccount()
        val credit = newAccount()
        val original = postBalanced("ref-audit-5", debit.id, credit.id)

        val requestBody = """{"reason":"duplicate posting"}"""
        val expectedHash = sha256Hex(requestBody)
        val compensating =
            transactionService.reverse(
                original.id,
                ACTOR,
                CORR_ID,
                reason = "duplicate posting",
                requestHash = expectedHash,
            )

        val rows = rowsFor(original.id.toString(), AuditAction.TRANSACTION_REVERSE)
        rows.size shouldBe 1
        val row = rows.first()
        row.action shouldBe AuditAction.TRANSACTION_REVERSE.name
        row.resourceId shouldBe original.id.toString()
        row.result shouldBe AuditResult.SUCCESS
        val payloadJson = row.payload.shouldNotBeNull()
        val tree = objectMapper.readTree(payloadJson)
        tree.has("reason").shouldBeTrue()
        tree.get("reason").asText() shouldBe "duplicate posting"
        tree.has("compensatingTransactionId").shouldBeTrue()
        tree.get("compensatingTransactionId").asText() shouldBe compensating.id.toString()
    }

    // AC-6: TRANSACTION_REVERSE without reason, payload has no reason key
    @Test
    fun `should write TRANSACTION_REVERSE row with no reason key in payload when reverse has no reason`() {
        val debit = newAccount()
        val credit = newAccount()
        val original = postBalanced("ref-audit-6", debit.id, credit.id)

        val compensating =
            transactionService.reverse(
                original.id,
                ACTOR,
                CORR_ID,
                reason = null,
                requestHash = null,
            )

        val rows = rowsFor(original.id.toString(), AuditAction.TRANSACTION_REVERSE)
        rows.size shouldBe 1
        val row = rows.first()
        row.action shouldBe AuditAction.TRANSACTION_REVERSE.name
        row.resourceId shouldBe original.id.toString()
        val payloadJson = row.payload.shouldNotBeNull()
        val tree = objectMapper.readTree(payloadJson)
        tree.has("reason").shouldBeFalse()
        tree.has("compensatingTransactionId").shouldBeTrue()
        tree.get("compensatingTransactionId").asText() shouldBe compensating.id.toString()
    }

    // AC-9: idempotent replay writes no second audit row
    @Test
    fun `should write no second audit row when idempotent replay is executed`() {
        val key = IdempotencyKey.of("audit-idem-key-0000000000000000000000")
        val requestBody = """{"name":"Idempotent Account","type":"USER_WALLET","currency":"USD"}"""
        var runs = 0
        var createdId = ""

        idempotencyService.execute(key, requestBody) { hash ->
            runs++
            val cmd =
                CreateAccountCommand(
                    name = "Idempotent Account",
                    type = AccountType.USER_WALLET,
                    currency = Currency.USD,
                    actor = ACTOR,
                    requestHash = hash,
                )
            val account = accountService.create(cmd)
            createdId = account.id.toString()
            StoredResponse(201, """{"id":"${account.id}"}""")
        }

        idempotencyService.execute(key, requestBody) { hash ->
            runs++
            StoredResponse(500, "should-not-run")
        }

        runs shouldBe 1
        rowsFor(createdId, AuditAction.ACCOUNT_CREATE).size shouldBe 1
    }

    companion object {
        const val ACTOR = "auth0|test-actor"
        const val CORR_ID = "corr-audit-it-001"

        fun sha256Hex(value: String): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
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
