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
