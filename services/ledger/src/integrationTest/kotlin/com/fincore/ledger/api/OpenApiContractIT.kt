// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PostgresContainerExtension::class)
@Import(OpenApiContractIT.TestSecurity::class)
class OpenApiContractIT(
    @Autowired private val rest: TestRestTemplate,
    @Autowired private val objectMapper: ObjectMapper,
) {
    @TestConfiguration
    class TestSecurity {
        @Bean
        fun jwtDecoder(): JwtDecoder =
            JwtDecoder { token ->
                Jwt
                    .withTokenValue(token)
                    .header("alg", "none")
                    .subject("openapi-contract-it")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                    .build()
            }
    }

    @Test
    fun `should keep the generated ledger paths in sync with the committed contract`() {
        val committed = ledgerPathsFromSpec()
        val generated = generatedLedgerPaths()

        val clue =
            "OpenAPI drift. In api/openapi.yaml but not generated: ${committed - generated}. " +
                "Generated but not in api/openapi.yaml: ${generated - committed}."
        withClue(clue) {
            generated shouldBe committed
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun ledgerPathsFromSpec(): Set<String> {
        val root = Files.newBufferedReader(Path.of(SPEC_PATH)).use { Yaml().load<Map<String, Any>>(it) }
        val paths = root["paths"] as Map<String, Any>
        return paths.keys.filter(::isLedgerPath).toSet()
    }

    private fun generatedLedgerPaths(): Set<String> {
        val doc = objectMapper.readTree(rest.getForEntity("/v3/api-docs", String::class.java).body)
        return doc
            .get("paths")
            .fieldNames()
            .asSequence()
            .filter(::isLedgerPath)
            .toSet()
    }

    private fun isLedgerPath(path: String): Boolean = path.startsWith("/v1/accounts") || path.startsWith("/v1/transactions")

    companion object {
        private const val EXPIRY_SECONDS = 300L
        private const val SPEC_PATH = "../../api/openapi.yaml"

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
