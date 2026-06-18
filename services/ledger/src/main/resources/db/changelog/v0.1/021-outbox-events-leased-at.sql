--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:021-outbox-events-leased-at dbms:postgresql
ALTER TABLE platform.outbox_events
    ADD COLUMN IF NOT EXISTS leased_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_outbox_events_claimable
    ON platform.outbox_events(status, created_at)
    WHERE status IN ('PENDING', 'FAILED', 'PUBLISHING');
