--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

-- Both entries of a transaction are inserted in this single changeset so the deferred
-- double-entry trigger (validated at changeset commit) sees a per-(transaction,currency)
-- sum of zero. Timestamps are NOW()-relative so a fresh sandbox shows a populated 24h
-- sparkline immediately; idempotency is provided by Liquibase changeset tracking.
--changeset fincore:demo-002-transactions context:demo dbms:postgresql
INSERT INTO ledger.transactions (id, reference, description, status, reverses_id, posted_at, created_at, created_by) VALUES
    ('00000000-0000-0000-0001-000000000001', 'demo-tx-001', 'wallet top-up',         'POSTED', NULL, NOW() - INTERVAL '26 hours',   NOW() - INTERVAL '26 hours',   'demo-seed'),
    ('00000000-0000-0000-0001-000000000002', 'demo-tx-002', 'card settlement',       'POSTED', NULL, NOW() - INTERVAL '22 hours',   NOW() - INTERVAL '22 hours',   'demo-seed'),
    ('00000000-0000-0000-0001-000000000003', 'demo-tx-003', 'service fee',           'POSTED', NULL, NOW() - INTERVAL '20 hours',   NOW() - INTERVAL '20 hours',   'demo-seed'),
    ('00000000-0000-0000-0001-000000000004', 'demo-tx-004', 'wallet top-up',         'POSTED', NULL, NOW() - INTERVAL '14 hours',   NOW() - INTERVAL '14 hours',   'demo-seed'),
    ('00000000-0000-0000-0001-000000000005', 'demo-tx-005', 'hold to suspense',      'POSTED', NULL, NOW() - INTERVAL '10 hours',   NOW() - INTERVAL '10 hours',   'demo-seed'),
    ('00000000-0000-0000-0001-000000000006', 'demo-tx-006', 'service fee',           'POSTED', NULL, NOW() - INTERVAL '8 hours',    NOW() - INTERVAL '8 hours',    'demo-seed'),
    ('00000000-0000-0000-0001-000000000007', 'demo-tx-007', 'release from suspense', 'POSTED', NULL, NOW() - INTERVAL '6 hours',    NOW() - INTERVAL '6 hours',    'demo-seed'),
    ('00000000-0000-0000-0001-000000000008', 'demo-tx-008', 'settlement payout',     'POSTED', NULL, NOW() - INTERVAL '4 hours',    NOW() - INTERVAL '4 hours',    'demo-seed'),
    ('00000000-0000-0000-0001-000000000009', 'demo-tx-009', 'service fee',           'POSTED', NULL, NOW() - INTERVAL '2 hours',    NOW() - INTERVAL '2 hours',    'demo-seed'),
    ('00000000-0000-0000-0001-000000000010', 'demo-tx-010', 'wallet top-up',         'POSTED', NULL, NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '30 minutes', 'demo-seed')
ON CONFLICT (id) DO NOTHING;

-- Each pair: DEBIT account gets +amount, CREDIT account gets -amount (single currency, sum = 0).
-- Accounts: W=...0001 wallet USD, F=...0003 fee, R=...0004 reserve, S=...0005 suspense.
INSERT INTO ledger.entries (id, transaction_id, account_id, amount, currency, direction, posted_at, created_at) VALUES
    ('00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0000-000000000001',  500.00,   'USD', 'DEBIT',  NOW() - INTERVAL '26 hours',   NOW() - INTERVAL '26 hours'),
    ('00000000-0000-0000-0002-000000000002', '00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0000-000000000004', -500.00,   'USD', 'CREDIT', NOW() - INTERVAL '26 hours',   NOW() - INTERVAL '26 hours'),
    ('00000000-0000-0000-0002-000000000003', '00000000-0000-0000-0001-000000000002', '00000000-0000-0000-0000-000000000004',  120.50,   'USD', 'DEBIT',  NOW() - INTERVAL '22 hours',   NOW() - INTERVAL '22 hours'),
    ('00000000-0000-0000-0002-000000000004', '00000000-0000-0000-0001-000000000002', '00000000-0000-0000-0000-000000000001', -120.50,   'USD', 'CREDIT', NOW() - INTERVAL '22 hours',   NOW() - INTERVAL '22 hours'),
    ('00000000-0000-0000-0002-000000000005', '00000000-0000-0000-0001-000000000003', '00000000-0000-0000-0000-000000000001',  2.50,     'USD', 'DEBIT',  NOW() - INTERVAL '20 hours',   NOW() - INTERVAL '20 hours'),
    ('00000000-0000-0000-0002-000000000006', '00000000-0000-0000-0001-000000000003', '00000000-0000-0000-0000-000000000003', -2.50,     'USD', 'CREDIT', NOW() - INTERVAL '20 hours',   NOW() - INTERVAL '20 hours'),
    ('00000000-0000-0000-0002-000000000007', '00000000-0000-0000-0001-000000000004', '00000000-0000-0000-0000-000000000001',  1000.00,  'USD', 'DEBIT',  NOW() - INTERVAL '14 hours',   NOW() - INTERVAL '14 hours'),
    ('00000000-0000-0000-0002-000000000008', '00000000-0000-0000-0001-000000000004', '00000000-0000-0000-0000-000000000004', -1000.00,  'USD', 'CREDIT', NOW() - INTERVAL '14 hours',   NOW() - INTERVAL '14 hours'),
    ('00000000-0000-0000-0002-000000000009', '00000000-0000-0000-0001-000000000005', '00000000-0000-0000-0000-000000000005',  75.00,    'USD', 'DEBIT',  NOW() - INTERVAL '10 hours',   NOW() - INTERVAL '10 hours'),
    ('00000000-0000-0000-0002-000000000010', '00000000-0000-0000-0001-000000000005', '00000000-0000-0000-0000-000000000001', -75.00,    'USD', 'CREDIT', NOW() - INTERVAL '10 hours',   NOW() - INTERVAL '10 hours'),
    ('00000000-0000-0000-0002-000000000011', '00000000-0000-0000-0001-000000000006', '00000000-0000-0000-0000-000000000001',  1.25,     'USD', 'DEBIT',  NOW() - INTERVAL '8 hours',    NOW() - INTERVAL '8 hours'),
    ('00000000-0000-0000-0002-000000000012', '00000000-0000-0000-0001-000000000006', '00000000-0000-0000-0000-000000000003', -1.25,     'USD', 'CREDIT', NOW() - INTERVAL '8 hours',    NOW() - INTERVAL '8 hours'),
    ('00000000-0000-0000-0002-000000000013', '00000000-0000-0000-0001-000000000007', '00000000-0000-0000-0000-000000000001',  75.00,    'USD', 'DEBIT',  NOW() - INTERVAL '6 hours',    NOW() - INTERVAL '6 hours'),
    ('00000000-0000-0000-0002-000000000014', '00000000-0000-0000-0001-000000000007', '00000000-0000-0000-0000-000000000005', -75.00,    'USD', 'CREDIT', NOW() - INTERVAL '6 hours',    NOW() - INTERVAL '6 hours'),
    ('00000000-0000-0000-0002-000000000015', '00000000-0000-0000-0001-000000000008', '00000000-0000-0000-0000-000000000004',  300.00,   'USD', 'DEBIT',  NOW() - INTERVAL '4 hours',    NOW() - INTERVAL '4 hours'),
    ('00000000-0000-0000-0002-000000000016', '00000000-0000-0000-0001-000000000008', '00000000-0000-0000-0000-000000000001', -300.00,   'USD', 'CREDIT', NOW() - INTERVAL '4 hours',    NOW() - INTERVAL '4 hours'),
    ('00000000-0000-0000-0002-000000000017', '00000000-0000-0000-0001-000000000009', '00000000-0000-0000-0000-000000000001',  3.10,     'USD', 'DEBIT',  NOW() - INTERVAL '2 hours',    NOW() - INTERVAL '2 hours'),
    ('00000000-0000-0000-0002-000000000018', '00000000-0000-0000-0001-000000000009', '00000000-0000-0000-0000-000000000003', -3.10,     'USD', 'CREDIT', NOW() - INTERVAL '2 hours',    NOW() - INTERVAL '2 hours'),
    ('00000000-0000-0000-0002-000000000019', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0000-000000000001',  250.00,   'USD', 'DEBIT',  NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '30 minutes'),
    ('00000000-0000-0000-0002-000000000020', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0000-000000000004', -250.00,   'USD', 'CREDIT', NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '30 minutes');

-- Derive balances from the seeded entries so totals stay consistent with no hand-computed
-- magic numbers; ON CONFLICT keeps it safe if a balance row already exists.
--changeset fincore:demo-002-balances context:demo dbms:postgresql
INSERT INTO ledger.account_balances (account_id, currency, balance, last_posted_at, version)
SELECT e.account_id, e.currency, SUM(e.amount), MAX(e.posted_at), 0
FROM   ledger.entries e
WHERE  e.transaction_id IN (
    '00000000-0000-0000-0001-000000000001',
    '00000000-0000-0000-0001-000000000002',
    '00000000-0000-0000-0001-000000000003',
    '00000000-0000-0000-0001-000000000004',
    '00000000-0000-0000-0001-000000000005',
    '00000000-0000-0000-0001-000000000006',
    '00000000-0000-0000-0001-000000000007',
    '00000000-0000-0000-0001-000000000008',
    '00000000-0000-0000-0001-000000000009',
    '00000000-0000-0000-0001-000000000010'
)
GROUP  BY e.account_id, e.currency
ON CONFLICT (account_id, currency) DO NOTHING;
