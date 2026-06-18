--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:payments-015-provider-reference dbms:postgresql
ALTER TABLE payments.payments
    ADD COLUMN IF NOT EXISTS provider_reference VARCHAR(200);
CREATE INDEX IF NOT EXISTS idx_payments_provider_reference
    ON payments.payments(provider_reference)
    WHERE provider_reference IS NOT NULL;
