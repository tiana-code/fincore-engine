// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.ledger.domain.Account
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.infrastructure.persistence.AccountPersistenceAdapter
import com.fincore.ledger.infrastructure.persistence.AccountRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PostgresContainerExtension::class)
@Import(AccountListingIT.TestSecurity::class)
class AccountListingIT(
    @Autowired private val rest: TestRestTemplate,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val accountRepository: AccountRepository,
    @Autowired private val adapter: AccountPersistenceAdapter,
) {
    @TestConfiguration
    class TestSecurity {
        @Bean
        fun jwtDecoder(): JwtDecoder =
            JwtDecoder { token ->
                Jwt
                    .withTokenValue(token)
                    .header("alg", "none")
                    .subject("list-it")
                    .claim("scope", "ledger:read")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                    .build()
            }
    }

    @Test
    fun `should return accounts newest first across the page`() {
        val base = Instant.parse("2026-06-13T00:00:00Z")
        val names = listOf("list-it-a", "list-it-b", "list-it-c")
        names.forEachIndexed { i, name ->
            val account = Account(AccountId.generate(), name, AccountType.ASSET, Currency.EUR)
            accountRepository.saveAndFlush(adapter.toNewEntity(account, "list-it", base.plusSeconds(i.toLong())))
        }

        val headers = HttpHeaders().apply { setBearerAuth("list-token") }
        val response = rest.exchange("/v1/accounts?page=0&size=100", HttpMethod.GET, HttpEntity<Void>(headers), String::class.java)

        response.statusCode.value() shouldBe 200
        val tree = objectMapper.readTree(response.body)
        (tree.get("totalElements").asLong() >= 3) shouldBe true
        val ordered = tree.get("items").map { it.get("name").asText() }.filter { it in names }
        ordered shouldBe listOf("list-it-c", "list-it-b", "list-it-a")
    }

    companion object {
        private const val EXPIRY_SECONDS = 300L

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
