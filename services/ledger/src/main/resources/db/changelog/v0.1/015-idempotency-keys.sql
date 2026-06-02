--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:015-idempotency-keys dbms:postgresql
CREATE TABLE IF NOT EXISTS platform.idempotency_keys (
    key_hash      VARCHAR(64) NOT NULL,
    request_hash  VARCHAR(64) NOT NULL,
    status_code   INT,
    response_body JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_idempotency_keys PRIMARY KEY (key_hash)
);
CREATE INDEX IF NOT EXISTS idx_idempotency_keys_expires ON platform.idempotency_keys(expires_at);
