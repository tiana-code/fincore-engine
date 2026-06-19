# Webhook receiver

A local development tool that receives FinCore payment webhooks, verifies their HMAC signature, and logs each delivery as VERIFIED or REJECTED. The verification mirrors the payments service exactly: lowercase hex of `HMAC-SHA256(secret, rawBody)`, checked constant-time against the `X-Webhook-Signature` header, fail-closed when the secret is blank.

## Run

```bash
npm install
npm run build
FINCORE_WEBHOOK_SECRET=<the same secret as fincore.payments.webhook.hmacSecret> PORT=9099 npm start
```

Point the payments service's webhook target at `http://localhost:9099`, then trigger a payment. The receiver prints, per delivery, whether the signature verified and the delivery id and outcome. The secret must match the one the payments service signs with; it is read from the environment and never stored.

SPDX-License-Identifier: BUSL-1.1
