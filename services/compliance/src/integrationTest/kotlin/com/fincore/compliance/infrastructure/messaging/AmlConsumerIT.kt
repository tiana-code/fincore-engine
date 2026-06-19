// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.compliance.application.kyc.KycCheckRequest
import com.fincore.compliance.application.kyc.KycCheckResult
import com.fincore.compliance.application.kyc.KycProvider
import com.fincore.compliance.infrastructure.persistence.AmlAlertRepository
import com.fincore.events.EventEnvelope
import com.fincore.events.LedgerEvents
import com.fincore.test.containers.PostgresContainerExtension
import com.fincore.test.containers.RedpandaContainerExtension
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant

@SpringBootTest
@ExtendWith(PostgresContainerExtension::class, RedpandaContainerExtension::class)
@Import(AmlConsumerIT.TestBeans::class)
class AmlConsumerIT(
    @Autowired private val kafkaTemplate: KafkaTemplate<String, String>,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val amlAlerts: AmlAlertRepository,
) {
    @TestConfiguration
    class TestBeans {
        @Bean fun jwtDecoder(): JwtDecoder = mockk()

        @Bean fun kycProvider(): KycProvider =
            object : KycProvider {
                override fun check(request: KycCheckRequest): KycCheckResult = KycCheckResult.Pending("ref-test")
            }
    }

    @AfterEach
    fun cleanUp() {
        amlAlerts.deleteAll()
    }

    @Test
    fun `should raise an alert when a posted transaction is consumed`() {
        val transactionId = "tx_consume_1"

        publish(envelopeJson(transactionId))

        awaitAlertCount(transactionId, 1)
    }

    @Test
    fun `should raise only one alert for a duplicate envelope`() {
        val transactionId = "tx_dedupe_1"
        val json = envelopeJson(transactionId)

        publish(json)
        publish(json)

        awaitAlertCount(transactionId, 1)
        // Let the duplicate be consumed and skipped, then confirm no second alert appeared.
        Thread.sleep(SETTLE_MS)
        amlAlerts.findBySubjectReference(transactionId).size shouldBe 1
    }

    private fun publish(json: String) {
        kafkaTemplate.send(TOPIC, json).get()
    }

    private fun awaitAlertCount(
        subjectReference: String,
        expected: Int,
    ) {
        val deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline &&
            amlAlerts.findBySubjectReference(subjectReference).size < expected
        ) {
            Thread.sleep(POLL_INTERVAL_MS)
        }
        amlAlerts.findBySubjectReference(subjectReference).size shouldBe expected
    }

    private fun envelopeJson(transactionId: String): String {
        val envelope =
            EventEnvelope.of(
                source = "ledger-service",
                type = LedgerEvents.TransactionPosted,
                data =
                    LedgerTransactionPosted(
                        transactionId = transactionId,
                        reference = "order-1",
                        currency = "USD",
                        postedAt = Instant.now(),
                        entries = listOf(LedgerEntryLine("acc_1", "DEBIT", "100.00")),
                    ),
            )
        return objectMapper.writeValueAsString(envelope)
    }

    companion object {
        private const val TOPIC = "fincore.transaction"
        private const val AWAIT_TIMEOUT_MS = 15_000L
        private const val POLL_INTERVAL_MS = 250L
        private const val SETTLE_MS = 2_000L
        private const val FLAGGING_RULE =
            """{"condition":{"attr":"amount","op":"gte","value":0},"outcome":{"label":"FLAG","reasonCodes":["aml.test"]}}"""

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { PostgresContainerExtension.jdbcUrl }
            registry.add("spring.datasource.username") { PostgresContainerExtension.username }
            registry.add("spring.datasource.password") { PostgresContainerExtension.password }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
            registry.add("spring.kafka.bootstrap-servers") { RedpandaContainerExtension.bootstrapServers }
            // Producer config is test-only: the service consumes; the test publishes the input events.
            registry.add("spring.kafka.producer.key-serializer") { "org.apache.kafka.common.serialization.StringSerializer" }
            registry.add("spring.kafka.producer.value-serializer") { "org.apache.kafka.common.serialization.StringSerializer" }
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") { "https://issuer.test" }
            registry.add("fincore.compliance.aml.rule") { FLAGGING_RULE }
            registry.add("fincore.compliance.aml.flag-label") { "FLAG" }
        }
    }
}
