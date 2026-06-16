--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:020-outbox-events-published-index dbms:postgresql
CREATE INDEX IF NOT EXISTS idx_outbox_events_published
    ON platform.outbox_events(published_at)
    WHERE status = 'PUBLISHED';
