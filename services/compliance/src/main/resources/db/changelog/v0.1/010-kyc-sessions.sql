--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:compliance-010-kyc-sessions dbms:postgresql
CREATE TABLE IF NOT EXISTS compliance.kyc_sessions (
    id                 UUID         NOT NULL DEFAULT gen_random_uuid(),
    subject_reference  VARCHAR(140) NOT NULL,
    status             VARCHAR(16)  NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_kyc_sessions PRIMARY KEY (id),
    CONSTRAINT ck_kyc_sessions_status
        CHECK (status IN ('INITIATED', 'SCREENING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS ix_kyc_sessions_status ON compliance.kyc_sessions (status);
