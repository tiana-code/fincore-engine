# Node quickstart

Calls the FinCore Engine ledger API through the TypeScript SDK (`@fincore/sdk-typescript`): lists accounts and fetches one.

## Run

1. Start the sandbox stack (from the repo root):

   ```bash
   docker compose up -d        # or: scripts/fincore run-demo
   ```

2. Build the local SDK it depends on, then this example:

   ```bash
   ( cd ../../sdk/typescript && npm install && npm run build )
   npm install
   npm run build
   ```

3. Run it, pointing at your services and supplying a bearer token with `ledger:read`:

   ```bash
   FINCORE_API_URL=http://localhost:8080 FINCORE_TOKEN=<jwt> npm start
   ```

`FINCORE_API_URL` defaults to `http://localhost:8080`. The sandbox stack does not run an identity provider, so obtain a token from your own Keycloak (the SDK sends it as a bearer when set).

SPDX-License-Identifier: BUSL-1.1
