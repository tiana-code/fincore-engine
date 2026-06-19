// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application

import com.fincore.core.Currency
import com.fincore.core.Money
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest
@AutoConfigureObservability
@ExtendWith(PostgresContainerExtension::class)
@Import(PaymentMetricsIT.TestBeans::class)
class PaymentMetricsIT(
    @Autowired private val paymentService: PaymentService,
    @Autowired private val meterRegistry: MeterRegistry,
) {
    @TestConfiguration
    class TestBeans {
        @Bean fun jwtDecoder(): JwtDecoder = mockk()
    }

    @Test
    fun `should increment the lifecycle counter with the initiated status when a payment is initiated`() {
        paymentService.initiate(InitiatePaymentCommand(UUID.randomUUID().toString(), money(), "metrics-order"))

        val count =
            meterRegistry
                .get("payments.lifecycle")
                .tag("status", "initiated")
                .counter()
                .count()

        count shouldBeGreaterThanOrEqual 1.0
    }

    private fun money(): Money = Money(BigDecimal("100.00"), Currency.USD)

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { PostgresContainerExtension.jdbcUrl }
            registry.add("spring.datasource.username") { PostgresContainerExtension.username }
            registry.add("spring.datasource.password") { PostgresContainerExtension.password }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") { "https://issuer.test" }
            registry.add("fincore.payments.bank.sandbox.enabled") { "true" }
        }
    }
}
