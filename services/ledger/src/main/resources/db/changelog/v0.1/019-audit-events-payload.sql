--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:019-audit-events-payload dbms:postgresql
ALTER TABLE platform.audit_events ADD COLUMN IF NOT EXISTS payload JSONB;
