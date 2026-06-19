# FinCore Engine API collection

An importable [Bruno](https://www.usebruno.com/) collection covering the implemented ledger, payments and decision
endpoints, for exploring a local sandbox. Bruno is open source and stores requests as plain text, so the collection is
diffable and reviewable. Postman and Insomnia can import a Bruno collection if you prefer those tools.

## Use

1. Install Bruno, then open this folder (`api/collection`) as a collection.
2. Select the **sandbox** environment and set its variables:
   - `ledgerUrl`, `paymentsUrl`, `decisionUrl` - base URLs (defaults target the local compose stack).
   - `token` - a bearer JWT with the required scopes. **Empty by default**; the sandbox compose stack does not run an
     identity provider, so obtain a token from your own Keycloak (or run the services against one) before calling the
     authenticated endpoints.
   - resource ids (`accountId`, `secondAccountId`, `transactionId`, `paymentId`) - fill in after creating a resource.
   - `webhookSignature` - empty by default; see Webhooks below.

## Scopes

| Service | Read | Write |
|---------|------|-------|
| Ledger | `ledger:read` | `ledger:write` |
| Payments | `payments:read` | `payments:write` |
| Decision | `decision:read` | `decision:write` |

Mutating ledger requests (create account, post transaction, reverse) and payment initiation require an
`Idempotency-Key` header; the other endpoints do not.

## Webhooks

`POST /v1/payments/webhooks` is authenticated by an HMAC signature over the raw body (`X-Webhook-Signature`), not a
bearer token. The `webhookSignature` variable is empty by default, so the request is rejected until you supply a
signature computed from the body and the configured sandbox secret. The end-to-end demo script computes it for you.

## Services and the sandbox

The sandbox compose stack runs ledger (`8080`) and payments (`8081`). The decision service is not part of that stack;
`decisionUrl` is a placeholder for running it separately. Endpoints that are specified but not yet implemented
(compliance/KYC, webhook subscriptions) are intentionally omitted.

SPDX-License-Identifier: BUSL-1.1
