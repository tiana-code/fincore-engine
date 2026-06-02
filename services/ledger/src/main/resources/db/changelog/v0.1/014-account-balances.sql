--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:014-account-balances dbms:postgresql
CREATE TABLE IF NOT EXISTS ledger.account_balances (
    account_id      UUID            NOT NULL,
    currency        CHAR(3)         NOT NULL,
    balance         NUMERIC(38, 18) NOT NULL DEFAULT 0,
    last_posted_at  TIMESTAMPTZ     NOT NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_account_balances PRIMARY KEY (account_id, currency),
    CONSTRAINT fk_account_balances_account FOREIGN KEY (account_id)
        REFERENCES ledger.accounts(id) ON DELETE RESTRICT,
    CONSTRAINT ck_account_balances_currency CHECK (currency ~ '^[A-Z]{3}$')
);
