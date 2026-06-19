--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:demo-001-accounts context:demo dbms:postgresql
INSERT INTO ledger.accounts (id, name, type, currency, created_by, updated_by) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Customer Wallet USD', 'USER_WALLET', 'USD', 'demo-seed', 'demo-seed'),
    ('00000000-0000-0000-0000-000000000002', 'Customer Wallet EUR', 'USER_WALLET', 'EUR', 'demo-seed', 'demo-seed'),
    ('00000000-0000-0000-0000-000000000003', 'Platform Fee Income', 'FEE', 'USD', 'demo-seed', 'demo-seed'),
    ('00000000-0000-0000-0000-000000000004', 'Settlement Reserve', 'RESERVE', 'USD', 'demo-seed', 'demo-seed'),
    ('00000000-0000-0000-0000-000000000005', 'Suspense', 'SUSPENSE', 'USD', 'demo-seed', 'demo-seed')
ON CONFLICT (id) DO NOTHING;
