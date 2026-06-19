// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.case

import com.fincore.compliance.domain.enum.CaseStatus
import com.fincore.compliance.infrastructure.persistence.ComplianceCasePersistenceAdapter
import com.fincore.compliance.infrastructure.persistence.ComplianceCaseRepository
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
@Import(ComplianceCaseServiceImpl::class, ComplianceCasePersistenceAdapter::class)
class CaseServicePersistenceIT(
    @Autowired private val service: ComplianceCaseService,
    @Autowired private val repository: ComplianceCaseRepository,
) {
    @AfterEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `should open then get a case round-tripping through postgres`() {
        val opened = service.open(OpenCaseCommand("case-ref-1"))

        val reloaded = service.get(opened.id)
        reloaded.id shouldBe opened.id
        reloaded.reference shouldBe "case-ref-1"
        reloaded.status shouldBe CaseStatus.OPEN
    }

    @Test
    fun `should persist the claim transition`() {
        val opened = service.open(OpenCaseCommand("case-ref-2"))

        service.claim(opened.id)

        service.get(opened.id).status shouldBe CaseStatus.CLAIMED
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
