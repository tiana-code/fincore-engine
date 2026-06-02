--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:012-entries dbms:postgresql
CREATE TABLE IF NOT EXISTS ledger.entries (
    id             UUID            NOT NULL,
    transaction_id UUID            NOT NULL,
    account_id     UUID            NOT NULL,
    amount         NUMERIC(38, 18) NOT NULL,
    currency       CHAR(3)         NOT NULL,
    direction      VARCHAR(8)      NOT NULL,
    posted_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_entries PRIMARY KEY (id, created_at),
    CONSTRAINT fk_entries_transaction FOREIGN KEY (transaction_id)
        REFERENCES ledger.transactions(id) ON DELETE RESTRICT,
    CONSTRAINT fk_entries_account FOREIGN KEY (account_id)
        REFERENCES ledger.accounts(id) ON DELETE RESTRICT,
    CONSTRAINT ck_entries_direction CHECK (direction IN ('DEBIT','CREDIT')),
    CONSTRAINT ck_entries_currency  CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_entries_amount_nonzero CHECK (amount <> 0)
) PARTITION BY RANGE (created_at);

CREATE TABLE IF NOT EXISTS ledger.entries_2026_q2 PARTITION OF ledger.entries
    FOR VALUES FROM ('2026-04-01') TO ('2026-07-01');
CREATE TABLE IF NOT EXISTS ledger.entries_2026_q3 PARTITION OF ledger.entries
    FOR VALUES FROM ('2026-07-01') TO ('2026-10-01');
CREATE TABLE IF NOT EXISTS ledger.entries_2026_q4 PARTITION OF ledger.entries
    FOR VALUES FROM ('2026-10-01') TO ('2027-01-01');
CREATE TABLE IF NOT EXISTS ledger.entries_2027_q1 PARTITION OF ledger.entries
    FOR VALUES FROM ('2027-01-01') TO ('2027-04-01');

CREATE INDEX IF NOT EXISTS idx_entries_account_cursor
    ON ledger.entries(account_id, currency, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_entries_account_posted
    ON ledger.entries(account_id, currency, posted_at);
CREATE INDEX IF NOT EXISTS idx_entries_transaction_id
    ON ledger.entries(transaction_id);
