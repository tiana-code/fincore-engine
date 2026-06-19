# FinCore Sandbox UI

Part of [FinCore Engine](https://github.com/tiana-code/fincore-engine). Data-dense fintech sandbox
dashboard (Phase E / Epic-08): Vite 6 + React 19 + TypeScript + Tailwind, dark-ops design system.

## Develop

```bash
npm install
npm run dev        # local dev server
npm run typecheck  # tsc --noEmit
npm run lint       # eslint
npm run test       # vitest
npm run build      # typecheck + production bundle
```

The Overview landing runs on a typed mock-data layer (`src/mock/`). Each screen migrates from mock to the
REST API as its backend lands.

## Run as a container

The SPA is served as static files by a non-root nginx image (multi-stage build, `web/Dockerfile`).

```bash
docker build -t fincore-web web            # build the image
docker run --rm -p 8082:8080 fincore-web   # serve on http://localhost:8082
```

Or via the umbrella stack (served alongside the backend):

```bash
docker compose up web
```

The API base URL is baked at build time from `VITE_API_BASE_URL` (defaults to a relative path); point it at the
ledger/payments/compliance/decision services as needed.
