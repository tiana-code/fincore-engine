--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:compliance-013-aml-rules dbms:postgresql
CREATE TABLE IF NOT EXISTS compliance.aml_rules (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    rule_key    VARCHAR(128) NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_aml_rules PRIMARY KEY (id),
    CONSTRAINT uq_aml_rules_rule_key UNIQUE (rule_key)
);
