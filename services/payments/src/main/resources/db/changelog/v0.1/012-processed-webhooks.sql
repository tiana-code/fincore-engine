--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:payments-012-processed-webhooks dbms:postgresql
CREATE TABLE IF NOT EXISTS payments.processed_webhooks (
    delivery_id VARCHAR(200) NOT NULL,
    received_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_processed_webhooks PRIMARY KEY (delivery_id)
);
