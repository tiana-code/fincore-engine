--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:compliance-012-compliance-cases dbms:postgresql
CREATE TABLE IF NOT EXISTS compliance.compliance_cases (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    reference   VARCHAR(140) NOT NULL,
    status      VARCHAR(16)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_compliance_cases PRIMARY KEY (id),
    CONSTRAINT ck_compliance_cases_status
        CHECK (status IN ('OPEN', 'CLAIMED', 'ESCALATED', 'RESOLVED'))
);

CREATE INDEX IF NOT EXISTS ix_compliance_cases_status ON compliance.compliance_cases (status);
