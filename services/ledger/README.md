<!-- SPDX-License-Identifier: BUSL-1.1 -->

# Ledger Service

The ledger service is the double-entry accounting core of FinCore Engine. It records immutable transactions as
balanced entries, enforces the double-entry invariant (entries sum to zero per transaction and currency) at the
application, service, and database layers, maintains per-account balances, and exposes a REST API for posting
transactions, reversing them, and reading balances and entries. Idempotent writes, a transactional outbox, and an
append-only audit trail make every state change safe to retry and fully traceable.

This README is the minimum needed to build, run, and exercise the service locally. For architecture, data model,
and the full API contract, see the Wiki links at the bottom.

## Prerequisites

- Java 21+ (Eclipse Temurin)
- Docker and Docker Compose (for PostgreSQL and a local run)

## Run the tests

Fast unit tests:

```bash
./gradlew :services:ledger:test
```

Integration tests use Testcontainers and require a running Docker daemon:

```bash
./gradlew :services:ledger:integrationTest
```

## Run locally

Start PostgreSQL and the ledger service with Docker Compose from the repository root:

```bash
docker compose up -d
```

The API listens on `http://localhost:8080`. Check readiness:

```bash
curl -fsS http://localhost:8080/actuator/health/readiness
```

The OpenAPI document is served at `http://localhost:8080/v3/api-docs` and Swagger UI at
`http://localhost:8080/swagger-ui.html`.

To run the service from source instead of the image:

```bash
./gradlew :services:ledger:bootRun
```

## Hit the API

Mutating endpoints require an OAuth2 bearer token with the `ledger:write` scope (reads require `ledger:read`).
See [Getting-Started](https://github.com/tiana-code/fincore-engine/wiki/Getting-Started) for the token flow. The
examples below assume the token is in `$TOKEN`.

Create two accounts:

```bash
curl -sS -X POST http://localhost:8080/v1/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" \
  -H "Content-Type: application/json" \
  -d '{"name":"wallet-a","type":"USER_WALLET","currency":"EUR"}'
```

Post a balanced transaction (entry amounts are signed and must sum to zero per currency):

```bash
curl -sS -X POST http://localhost:8080/v1/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" \
  -H "Content-Type: application/json" \
  -d '{
        "reference": "demo-1",
        "currency": "EUR",
        "entries": [
          {"accountId": "acc_...", "direction": "DEBIT",  "amount": "100.00"},
          {"accountId": "acc_...", "direction": "CREDIT", "amount": "-100.00"}
        ]
      }'
```

Read an account balance:

```bash
curl -sS http://localhost:8080/v1/accounts/acc_.../balance \
  -H "Authorization: Bearer $TOKEN"
```

## Documentation

- [Getting-Started](https://github.com/tiana-code/fincore-engine/wiki/Getting-Started) - prerequisites, token flow, first request
- [Epic-01-Ledger](https://github.com/tiana-code/fincore-engine/wiki/Epic-01-Ledger) - scope, design, and acceptance criteria
- [API-Ledger](https://github.com/tiana-code/fincore-engine/wiki/API-Ledger) - full endpoint reference

Part of [FinCore Engine](https://github.com/tiana-code/fincore-engine).
