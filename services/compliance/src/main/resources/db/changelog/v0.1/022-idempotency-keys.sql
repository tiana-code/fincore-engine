--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:compliance-022-idempotency-keys dbms:postgresql
CREATE TABLE IF NOT EXISTS compliance.idempotency_keys (
    key_hash        VARCHAR(64) NOT NULL,
    kyc_session_id  UUID        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_compliance_idempotency_keys PRIMARY KEY (key_hash)
);
