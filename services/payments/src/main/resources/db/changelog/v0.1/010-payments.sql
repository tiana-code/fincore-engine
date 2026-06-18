--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:payments-010-payments dbms:postgresql
CREATE TABLE IF NOT EXISTS payments.payments (
    id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    reference   VARCHAR(140)    NOT NULL,
    amount      NUMERIC(38, 18) NOT NULL,
    currency    CHAR(3)         NOT NULL,
    status      VARCHAR(16)     NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT ck_payments_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_payments_status
        CHECK (status IN ('INITIATED', 'SCREENING', 'SUBMITTED', 'SETTLED', 'FAILED', 'CANCELLED'))
);
