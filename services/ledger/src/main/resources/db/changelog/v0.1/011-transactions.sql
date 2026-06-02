--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:011-transactions dbms:postgresql
CREATE TABLE IF NOT EXISTS ledger.transactions (
    id          UUID PRIMARY KEY,
    reference   VARCHAR(255) NOT NULL,
    description VARCHAR(2048),
    status      VARCHAR(16)  NOT NULL,
    reverses_id UUID,
    metadata    JSONB        NOT NULL DEFAULT '{}',
    posted_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(64)  NOT NULL,
    CONSTRAINT uq_transactions_reference UNIQUE (reference),
    CONSTRAINT ck_transactions_status CHECK (status IN ('POSTED','REVERSED')),
    CONSTRAINT fk_transactions_reverses FOREIGN KEY (reverses_id)
        REFERENCES ledger.transactions(id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_transactions_status    ON ledger.transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_posted_at ON ledger.transactions(posted_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_transactions_reverses_id
    ON ledger.transactions(reverses_id)
    WHERE reverses_id IS NOT NULL;
