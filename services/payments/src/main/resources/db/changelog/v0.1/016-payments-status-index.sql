--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:payments-016-status-index dbms:postgresql
CREATE INDEX IF NOT EXISTS idx_payments_status_created_at
    ON payments.payments(status, created_at)
    WHERE status IN ('INITIATED', 'SCREENING', 'SUBMITTED');
