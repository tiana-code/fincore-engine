--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:payments-011-payment-events dbms:postgresql
CREATE TABLE IF NOT EXISTS payments.payment_events (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    payment_id  UUID         NOT NULL,
    event_type  VARCHAR(128) NOT NULL,
    payload     JSONB        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_payment_events PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_payment_events_payment
    ON payments.payment_events(payment_id, created_at);
