# Design: add-e2e-and-polish

## Context

**What already exists:**
- pokedex-api ships a Compose file (PostgreSQL 16 + the API) and a `.env.example` with working local placeholders, seeding `admin` and `trainer` plus ten local Pokémon when `SEED_DEMO_DATA=true`.
- This repository's image serves the SPA from nginx on port 80 and proxies `/api/` to `API_UPSTREAM`, resolved per request.
- The unit suite mocks the API with MSW.
- The changes so far were checked by hand with a real browser against a real backend.

**What only a real browser found:** the sign-out transition race (`add-local-pokemon-management-ui` D15) and the "Catalog" `aria-current` (`add-detail-ui` D9).

## Goals / Non-Goals

**Goals:**
- One command, from one clone, runs the whole product with demo data.
- The main journeys of US01–US04 run in a real browser against the real API on every change, with accessibility checked on the way.
- Each run is independent: a fresh database every time, and no state shared between runs or tests.

**Non-Goals:**
- Visual regression, cross-browser runs, and stubbing PokeAPI.

## Decisions

### D1. The full stack
- **Project:** `docker-compose.full.yml`, Compose project `pokedex-full`.
- **`postgres`:** the backend's own settings, with a named volume.
- **`api`:**
  - built from `${API_CONTEXT:-https://github.com/aleferod/pokedex-api.git#main}`, a Git build context, so one clone of this repository is enough. `API_CONTEXT=../pokedex-api` builds a local checkout instead.
  - `SEED_DEMO_DATA` defaults to `true`, as in the backend's Compose file.
  - its port is published as `${API_PORT:-8080}` for Swagger UI.
- **`web`:** this repository's image with `API_UPSTREAM=http://api:8080`, published as `${WEB_PORT:-8000}`.
- **Health:** every service has a healthcheck, and `web` and `api` start after their dependencies are healthy, so `up --wait` returns only once the product works.
- **Secrets:** they come only from `.env`, with `${VAR:?…}` guards. `.env.example` gains the backend's placeholders (`DB_PASSWORD=change-me`, a `JWT_SECRET` placeholder of 32+ bytes) under a comment saying they are for local use only, exactly as pokedex-api does. Vite ignores non-`VITE_` variables, so sharing `.env` with the dev server is harmless.

### D2. Running the end-to-end suite
`scripts/e2e.sh [--screenshots]` does four things:
1. It starts a throwaway stack: Compose project `pokedex-e2e`, its own ports `WEB_PORT=8100` and `API_PORT=8180`, and `up --build --wait`.
2. It runs `npx playwright test` in `mcr.microsoft.com/playwright:v1.63.0-noble`, the same version as the `@playwright/test` dependency, with the repository mounted, `--network host` and `E2E_BASE_URL=http://localhost:8100`.
3. It always tears the stack down with `down -v`, from a `trap`, even on failure.
4. It returns Playwright's exit code.

Playwright runs inside its own image, so no browser is installed on the host or by npm. The suite reuses the repository's `node_modules`: Playwright's runner and `playwright-core` are plain JavaScript, so the Alpine install works inside the Ubuntu-based image.

### D3. The journeys (`e2e/*.e2e.ts`)
**Files:** the suffix keeps them out of Vitest's default `*.test.ts` and `*.spec.ts` pattern. Vitest also excludes `e2e/` explicitly.

**The journeys:**

| File | Journey |
|---|---|
| `visitor.e2e.ts` | Catalog page 1 → Next → a card → the detail (stats, chain) → a chain stage (focus on the name) → "Back to the catalog" lands on page 2; the finder opens Pikachu |
| `account.e2e.ts` | Register a unique user (a random suffix) → signed in → sign out on a protected page lands home → a wrong password is refused → sign in |
| `local.e2e.ts` | As `trainer`: `/local?tag=starter` as a visitor goes to sign-in and back; filter by tag; sync Eevee (imported) and Pikachu (re-synced); add and remove a tag; edit the region; a second client saves first (a real 409 through `request` with `If-Match`) and "Load the latest version" recovers; no "Delete". As `admin`: delete with confirmation. |

**Isolation:**
- Each test signs in through the UI and works on its own records, such as Eevee, which only it syncs.
- Tests run in parallel within a file, except in `local.e2e.ts`, which runs serially because its steps share seeded records.

**Retries and traces:** `retries: process.env.CI ? 1 : 0`, and on a retry Playwright keeps a trace.

**Locators:** they're accessible names and roles, the same contract as the unit tests, not CSS.

### D4. Accessibility checks
- **What runs:** `e2e/a11y.e2e.ts` opens every main page with its real data, the forms with their errors showing, and the delete confirmation. On each, it runs `new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])`.
- **What fails:** any `serious` or `critical` violation fails the test, which lists the rule, the target and the help URL. Minor and moderate findings are reported, not failed.
- **If it finds something:** fix the app, or, if axe is wrong, exclude that one rule on that one element and say why.

### D5. CI
- **The job:** "End-to-end" runs `cp .env.example .env` and then `scripts/e2e.sh` on `ubuntu-latest`.
- **Speed:** BuildKit's GitHub cache isn't available to `docker compose build` without extra setup. Both images build in a few minutes, and that's accepted.
- **On failure:** the Playwright HTML report and traces are uploaded as artifacts.
- **PokeAPI:** the run depends on PokeAPI being reachable. The backend's cache, retries and circuit breaker smooth over blips, and Playwright retries once in CI. A real PokeAPI outage fails the job, and that's the honest signal.

### D6. Screenshots
`scripts/e2e.sh --screenshots` runs only `e2e/screenshots.e2e.ts` (`@screenshots`); the normal run skips it.
- **What it captures:** the catalog, a Pokémon's page, My Pokédex, a record, the edit form with a conflict, and the catalog at 360 px.
- **Where they go:** `docs/screenshots/*.png`, committed and shown in the README.
- **When they change:** they're regenerated only when the screens change, not in CI.

### D7. Tooling
- **TypeScript:** `tsconfig.e2e.json` covers `e2e/` and `playwright.config.ts`, and is referenced from the solution `tsconfig.json`. `npm run typecheck` checks it too.
- **ESLint:** the type-aware config covers `e2e/` through that project.
- **Vitest:** `exclude: ['e2e/**', …defaults]`.
- **Scripts:** a `test:e2e` npm script runs `scripts/e2e.sh`.

## Risks / Trade-offs

- [PokeAPI is unreachable in CI] → it's retried once and the trace is uploaded. A real outage fails the job, and the reason is visible.
- [Building pokedex-api from a Git URL fetches its `main` at that moment] → that's intended: the full stack is "the product as merged". `API_CONTEXT` pins anything else, such as a local checkout.
- [End-to-end runs are slower] → it's a separate job, so the fast gates still report first.
- [Shared seed data between tests] → `local.e2e.ts` runs serially, and the other files touch only their own data.
