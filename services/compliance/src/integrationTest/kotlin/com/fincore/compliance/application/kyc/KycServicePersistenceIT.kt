// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

import com.fincore.compliance.domain.enum.KycStatus
import com.fincore.compliance.infrastructure.persistence.KycSessionPersistenceAdapter
import com.fincore.compliance.infrastructure.persistence.KycSessionRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ExtendWith(PostgresContainerExtension::class)
@Import(KycServiceImpl::class, KycSessionPersistenceAdapter::class)
class KycServicePersistenceIT(
    @Autowired private val service: KycService,
    @Autowired private val repository: KycSessionRepository,
) {
    @AfterEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `should initiate then get a session round-tripping through postgres`() {
        val initiated = service.initiate(InitiateKycSessionCommand("subject-1"))

        val reloaded = service.get(initiated.id)
        reloaded.id shouldBe initiated.id
        reloaded.subjectReference shouldBe "subject-1"
        reloaded.status shouldBe KycStatus.INITIATED
    }

    @Test
    fun `should persist the screening transition`() {
        val initiated = service.initiate(InitiateKycSessionCommand("subject-2"))

        service.beginScreening(initiated.id)

        service.get(initiated.id).status shouldBe KycStatus.SCREENING
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
