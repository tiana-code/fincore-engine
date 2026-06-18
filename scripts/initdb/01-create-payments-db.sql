-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors
-- Runs once on first postgres init (empty data dir). Gives payments its own
-- database so its Liquibase DATABASECHANGELOG/lock stay isolated from ledger.
CREATE DATABASE payments OWNER ledger;
