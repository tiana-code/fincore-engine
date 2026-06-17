--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:002-decision-rules dbms:postgresql
CREATE TABLE IF NOT EXISTS decision.decision_rules (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    rule_key          VARCHAR(128) NOT NULL,
    active_version_id UUID,
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_decision_rules PRIMARY KEY (id),
    CONSTRAINT uq_decision_rules_rule_key UNIQUE (rule_key)
);
