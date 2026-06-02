--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:010-accounts dbms:postgresql
CREATE TABLE IF NOT EXISTS ledger.accounts (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(32)  NOT NULL,
    currency    CHAR(3)      NOT NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    metadata    JSONB        NOT NULL DEFAULT '{}',
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(64)  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  VARCHAR(64)  NOT NULL,
    CONSTRAINT ck_accounts_type CHECK (type IN (
        'ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE',
        'USER_WALLET','FEE','RESERVE','SUSPENSE'
    )),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE','FROZEN','CLOSED')),
    CONSTRAINT ck_accounts_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_accounts_name_length CHECK (char_length(name) BETWEEN 1 AND 255)
);
CREATE INDEX IF NOT EXISTS idx_accounts_currency ON ledger.accounts(currency);
CREATE INDEX IF NOT EXISTS idx_accounts_type     ON ledger.accounts(type);
CREATE INDEX IF NOT EXISTS idx_accounts_status   ON ledger.accounts(status);
CREATE INDEX IF NOT EXISTS idx_accounts_created  ON ledger.accounts(created_at DESC);

--changeset fincore:010-accounts-updated-at-trigger dbms:postgresql runOnChange:true splitStatements:false
CREATE OR REPLACE TRIGGER trg_accounts_updated_at
    BEFORE UPDATE ON ledger.accounts
    FOR EACH ROW EXECUTE FUNCTION platform.set_updated_at();
