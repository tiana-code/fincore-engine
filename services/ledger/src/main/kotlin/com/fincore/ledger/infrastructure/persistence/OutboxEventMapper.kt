// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import com.fincore.events.OutboxEvent
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface OutboxEventMapper {
    fun toDomain(entity: OutboxEventEntity): OutboxEvent

    fun toEntity(domain: OutboxEvent): OutboxEventEntity
}
