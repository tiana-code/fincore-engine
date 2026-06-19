--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:compliance-011-aml-alerts dbms:postgresql
CREATE TABLE IF NOT EXISTS compliance.aml_alerts (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    rule_key    VARCHAR(128) NOT NULL,
    status      VARCHAR(16) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_aml_alerts PRIMARY KEY (id),
    CONSTRAINT ck_aml_alerts_status
        CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED'))
);

CREATE INDEX IF NOT EXISTS ix_aml_alerts_status ON compliance.aml_alerts (status);
