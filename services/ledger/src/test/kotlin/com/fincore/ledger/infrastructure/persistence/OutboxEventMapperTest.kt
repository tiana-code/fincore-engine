// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import com.fincore.events.OutboxEvent
import com.fincore.events.OutboxStatus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class OutboxEventMapperTest {
    private val mapper = OutboxEventMapperImpl()

    @Test
    fun `should round trip an outbox event between domain and entity`() {
        val domain =
            OutboxEvent(
                id = UUID.fromString("00000000-0000-0000-0000-0000000000aa"),
                aggregateType = "Account",
                aggregateId = "acc_01HZX",
                eventType = "com.fincore.ledger.account.created.v1",
                payload = "{\"name\":\"acct\"}",
                status = OutboxStatus.PENDING,
                createdAt = Instant.parse("2026-06-05T12:00:00Z"),
                publishedAt = null,
                attempts = 0,
                lastError = null,
            )

        mapper.toDomain(mapper.toEntity(domain)) shouldBe domain
    }

    @Test
    fun `should map every field onto the entity`() {
        val domain =
            OutboxEvent(
                id = UUID.fromString("00000000-0000-0000-0000-0000000000bb"),
                aggregateType = "Transaction",
                aggregateId = "tx_01HZY",
                eventType = "com.fincore.ledger.transaction.posted.v1",
                payload = "{}",
                status = OutboxStatus.FAILED,
                createdAt = Instant.parse("2026-06-05T13:00:00Z"),
                publishedAt = Instant.parse("2026-06-05T13:05:00Z"),
                attempts = 3,
                lastError = "boom",
            )

        val entity = mapper.toEntity(domain)

        entity.id shouldBe domain.id
        entity.aggregateType shouldBe domain.aggregateType
        entity.aggregateId shouldBe domain.aggregateId
        entity.eventType shouldBe domain.eventType
        entity.payload shouldBe domain.payload
        entity.status shouldBe domain.status
        entity.createdAt shouldBe domain.createdAt
        entity.publishedAt shouldBe domain.publishedAt
        entity.attempts shouldBe domain.attempts
        entity.lastError shouldBe domain.lastError
    }
}
