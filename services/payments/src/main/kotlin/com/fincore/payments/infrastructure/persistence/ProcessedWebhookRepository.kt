// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProcessedWebhookRepository : JpaRepository<ProcessedWebhookEntity, String> {
    @Modifying
    @Query(
        nativeQuery = true,
        value = "INSERT INTO payments.processed_webhooks(delivery_id, received_at) VALUES (:deliveryId, NOW()) ON CONFLICT DO NOTHING",
    )
    fun insertIfAbsent(
        @Param("deliveryId") deliveryId: String,
    ): Int
}
