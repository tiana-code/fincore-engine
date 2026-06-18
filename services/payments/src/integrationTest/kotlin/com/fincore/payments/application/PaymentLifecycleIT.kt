// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.core.Currency
import com.fincore.core.Money
import com.fincore.core.PaymentId
import com.fincore.events.OutboxStatus
import com.fincore.events.PaymentEvents
import com.fincore.payments.application.bank.BankProvider
import com.fincore.payments.application.retry.PaymentRetryProperties
import com.fincore.payments.application.retry.PaymentRetryService
import com.fincore.payments.application.retry.PaymentRetryServiceImpl
import com.fincore.payments.application.screening.PaymentScreeningProperties
import com.fincore.payments.application.screening.ScreeningEvaluator
import com.fincore.payments.application.webhook.PaymentWebhookHandler
import com.fincore.payments.application.webhook.PaymentWebhookNotification
import com.fincore.payments.application.webhook.PaymentWebhookProperties
import com.fincore.payments.application.webhook.WebhookOutcome
import com.fincore.payments.application.webhook.WebhookSignatureException
import com.fincore.payments.application.webhook.WebhookSignatureVerifier
import com.fincore.payments.domain.enum.PaymentStatus
import com.fincore.payments.infrastructure.bank.SandboxBankProvider
import com.fincore.payments.infrastructure.outbox.PaymentOutboxEventPublisherImpl
import com.fincore.payments.infrastructure.persistence.PaymentEventRepository
import com.fincore.payments.infrastructure.persistence.PaymentIdempotencyKeyRepository
import com.fincore.payments.infrastructure.persistence.PaymentOutboxEventRepository
import com.fincore.payments.infrastructure.persistence.PaymentPersistenceAdapter
import com.fincore.payments.infrastructure.persistence.PaymentRepository
import com.fincore.payments.infrastructure.persistence.ProcessedWebhookRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Duration
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ExtendWith(PostgresContainerExtension::class)
@Import(
    PaymentServiceImpl::class,
    PaymentIdempotencyStore::class,
    PaymentPersistenceAdapter::class,
    PaymentOutboxEventPublisherImpl::class,
    PaymentOrchestrator::class,
    ScreeningEvaluator::class,
    PaymentWebhookHandler::class,
    WebhookSignatureVerifier::class,
    PaymentRetryServiceImpl::class,
    PaymentLifecycleIT.Beans::class,
)
class PaymentLifecycleIT(
    @Autowired private val service: PaymentService,
    @Autowired private val orchestrator: PaymentOrchestrator,
    @Autowired private val webhookHandler: PaymentWebhookHandler,
    @Autowired private val retry: PaymentRetryService,
    @Autowired private val payments: PaymentRepository,
    @Autowired private val paymentEvents: PaymentEventRepository,
    @Autowired private val outbox: PaymentOutboxEventRepository,
    @Autowired private val processedWebhooks: ProcessedWebhookRepository,
    @Autowired private val keys: PaymentIdempotencyKeyRepository,
) {
    @AfterEach
    fun cleanUp() {
        paymentEvents.deleteAll()
        processedWebhooks.deleteAll()
        outbox.deleteAll()
        keys.deleteAll()
        payments.deleteAll()
    }

    @Test
    fun `should settle a payment through the full initiate screen submit webhook lifecycle`() {
        val submitted = initiateAndSubmit("key-1", "order-1")
        status(submitted.id) shouldBe PaymentStatus.SUBMITTED
        providerReference(submitted.id) shouldBe "sbx-${submitted.id}"

        val delivery = "delivery-1"
        val raw = webhookPayload(delivery, providerReference(submitted.id), "SETTLED")
        webhookHandler.handle(raw, sign(raw), notification(delivery, providerReference(submitted.id), WebhookOutcome.SETTLED))

        status(submitted.id) shouldBe PaymentStatus.SETTLED
        eventTypes() shouldContainAll
            listOf(
                PaymentEvents.PaymentInitiated.fullType,
                PaymentEvents.PaymentScreened.fullType,
                PaymentEvents.PaymentSettled.fullType,
            )
        outbox.findAll().all { it.status == OutboxStatus.PENDING } shouldBe true
        processedWebhooks.count() shouldBe 1L
    }

    @Test
    fun `should fail a payment when the bank rejects the submission`() {
        val payment = service.initiate(command("key-1", "reject-this-order"))

        orchestrator.process(payment.id)

        status(payment.id) shouldBe PaymentStatus.FAILED
        eventTypes() shouldContainAll listOf(PaymentEvents.PaymentInitiated.fullType, PaymentEvents.PaymentFailed.fullType)
    }

    @Test
    fun `should fail a submitted payment when the webhook reports failure`() {
        val submitted = initiateAndSubmit("key-1", "order-1")

        val delivery = "delivery-fail"
        val raw = webhookPayload(delivery, providerReference(submitted.id), "FAILED")
        webhookHandler.handle(raw, sign(raw), notification(delivery, providerReference(submitted.id), WebhookOutcome.FAILED))

        status(submitted.id) shouldBe PaymentStatus.FAILED
    }

    @Test
    fun `should not create a second payment when initiating twice with the same key`() {
        val first = service.initiate(command("key-1", "order-1"))
        val second = service.initiate(command("key-1", "order-1"))

        second.id shouldBe first.id
        payments.count() shouldBe 1L
        outbox.findAll().count { it.eventType == PaymentEvents.PaymentInitiated.fullType } shouldBe 1
    }

    @Test
    fun `should settle once and ignore a duplicate webhook delivery`() {
        val submitted = initiateAndSubmit("key-1", "order-1")
        val delivery = "delivery-dup"
        val ref = providerReference(submitted.id)
        val raw = webhookPayload(delivery, ref, "SETTLED")

        webhookHandler.handle(raw, sign(raw), notification(delivery, ref, WebhookOutcome.SETTLED))
        webhookHandler.handle(raw, sign(raw), notification(delivery, ref, WebhookOutcome.SETTLED))

        status(submitted.id) shouldBe PaymentStatus.SETTLED
        processedWebhooks.count() shouldBe 1L
        outbox.findAll().count { it.eventType == PaymentEvents.PaymentSettled.fullType } shouldBe 1
    }

    @Test
    fun `should cancel an initiated payment`() {
        val payment = service.initiate(command("key-1", "order-1"))

        service.cancel(payment.id)

        status(payment.id) shouldBe PaymentStatus.CANCELLED
        eventTypes() shouldContainAll listOf(PaymentEvents.PaymentCancelled.fullType)
    }

    @Test
    fun `should resume a payment stuck in screening on retry`() {
        val payment = service.initiate(command("key-1", "order-1"))
        service.screen(payment.id)
        status(payment.id) shouldBe PaymentStatus.SCREENING

        retry.retryStuck()

        status(payment.id) shouldBe PaymentStatus.SUBMITTED
        providerReference(payment.id) shouldBe "sbx-${payment.id}"
    }

    @Test
    fun `should reject a webhook with an invalid signature and leave the payment unchanged`() {
        val submitted = initiateAndSubmit("key-1", "order-1")
        val delivery = "delivery-bad"
        val ref = providerReference(submitted.id)
        val raw = webhookPayload(delivery, ref, "SETTLED")

        shouldThrow<WebhookSignatureException> {
            webhookHandler.handle(raw, "deadbeef", notification(delivery, ref, WebhookOutcome.SETTLED))
        }

        status(submitted.id) shouldBe PaymentStatus.SUBMITTED
        processedWebhooks.count() shouldBe 0L
    }

    private fun initiateAndSubmit(
        key: String,
        reference: String,
    ): com.fincore.payments.domain.Payment {
        val payment = service.initiate(command(key, reference))
        orchestrator.process(payment.id)
        return payment
    }

    private fun command(
        key: String,
        reference: String,
    ): InitiatePaymentCommand = InitiatePaymentCommand(key, Money(BigDecimal(AMOUNT), Currency.USD), reference)

    private fun status(id: PaymentId): PaymentStatus = payments.findById(id.value).orElseThrow().status

    private fun providerReference(id: PaymentId): String =
        payments.findById(id.value).orElseThrow().providerReference ?: error("no provider reference for $id")

    private fun eventTypes(): List<String> = outbox.findAll().map { it.eventType }

    private fun notification(
        deliveryId: String,
        providerReference: String,
        outcome: WebhookOutcome,
    ): PaymentWebhookNotification = PaymentWebhookNotification(deliveryId, providerReference, outcome)

    private fun webhookPayload(
        deliveryId: String,
        providerReference: String,
        outcome: String,
    ): String = """{"deliveryId":"$deliveryId","providerReference":"$providerReference","outcome":"$outcome"}"""

    private fun sign(payload: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(SECRET.toByteArray(Charsets.UTF_8), HMAC_ALGORITHM))
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    @TestConfiguration
    class Beans {
        @Bean fun objectMapper(): ObjectMapper = ObjectMapper().findAndRegisterModules()

        @Bean fun screeningProperties(): PaymentScreeningProperties = PaymentScreeningProperties()

        @Bean fun webhookProperties(): PaymentWebhookProperties = PaymentWebhookProperties(hmacSecret = SECRET)

        @Bean
        fun retryProperties(): PaymentRetryProperties = PaymentRetryProperties(stuckAfter = Duration.ZERO, maxAge = Duration.ofHours(1))

        @Bean fun bankProvider(): BankProvider = SandboxBankProvider()
    }

    companion object {
        private const val AMOUNT = "100.00"
        private const val SECRET = "test-webhook-shared-secret-value"
        private const val HMAC_ALGORITHM = "HmacSHA256"

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
