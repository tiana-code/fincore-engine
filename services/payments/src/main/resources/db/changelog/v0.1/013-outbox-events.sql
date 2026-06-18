--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:payments-013-outbox-events dbms:postgresql
CREATE TABLE IF NOT EXISTS payments.outbox_events (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(64)  NOT NULL,
    aggregate_id   VARCHAR(64)  NOT NULL,
    event_type     VARCHAR(128) NOT NULL,
    payload        JSONB        NOT NULL,
    status         VARCHAR(24)  NOT NULL DEFAULT 'PENDING',
    attempts       INT          NOT NULL DEFAULT 0,
    last_error     TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    published_at   TIMESTAMPTZ,
    leased_at      TIMESTAMPTZ,
    CONSTRAINT pk_payments_outbox_events PRIMARY KEY (id),
    CONSTRAINT ck_payments_outbox_status
        CHECK (status IN ('PENDING', 'PUBLISHING', 'PUBLISHED', 'FAILED', 'PERMANENTLY_FAILED'))
);
CREATE INDEX IF NOT EXISTS idx_payments_outbox_pending
    ON payments.outbox_events(status, created_at)
    WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_payments_outbox_claimable
    ON payments.outbox_events(status, created_at)
    WHERE status IN ('PENDING', 'FAILED', 'PUBLISHING');
