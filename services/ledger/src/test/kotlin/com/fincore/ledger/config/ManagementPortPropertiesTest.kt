// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.config

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.PropertySource
import org.springframework.core.io.ClassPathResource

class ManagementPortPropertiesTest {
    private val base = load("application.yml")
    private val prod = load("application-prod.yml")

    @Test
    fun `should bind a prod management port distinct from the public application port`() {
        val appPort = base.getProperty("server.port")
        val managementPort = prod.getProperty("management.server.port")

        managementPort shouldNotBe appPort
    }

    @Test
    fun `should enable graceful shutdown with the configured drain timeout`() {
        base.getProperty("server.shutdown") shouldBe "graceful"
        base.getProperty("spring.lifecycle.timeout-per-shutdown-phase") shouldBe "30s"
    }

    @Test
    fun `should expose liveness as jvm state only and readiness with database connectivity`() {
        base.getProperty("management.endpoint.health.probes.enabled") shouldBe true
        base.getProperty("management.endpoint.health.group.liveness.include") shouldBe "livenessState"
        base.getProperty("management.endpoint.health.group.readiness.include") shouldBe "readinessState,db"
    }

    @Test
    fun `should show health components but not details on the public probe endpoints`() {
        base.getProperty("management.endpoint.health.show-components") shouldBe "always"
        base.getProperty("management.endpoint.health.show-details") shouldBe "never"
    }

    private companion object {
        fun load(resource: String): PropertySource<*> = YamlPropertySourceLoader().load(resource, ClassPathResource(resource)).first()
    }
}
