# Proposal: add-e2e-and-polish

## Why

Every screen of US01–US04 is done and unit-tested against a mocked API, and each change was checked by hand in a browser against a real backend. Two things are still missing:
- **One command for a reviewer:** running the whole product (database, API, web app) still means cloning two repositories and starting two stacks.
- **Automated end-to-end coverage:** no test checks, on every change, that the screens and the real API still work together in a real browser.

The browser checks also found bugs that only a real browser shows: the sign-out transition and the navigation's `aria-current`. Those checks should run automatically from now on.

## What Changes

- **Full stack in one command:** `docker-compose.full.yml` runs PostgreSQL, pokedex-api (built straight from its GitHub repository, or from a local checkout) and this web app, with the demo data, behind the web app's same-origin proxy.
  - One command starts it: `cp .env.example .env && docker compose -f docker-compose.full.yml up --build`. `.env.example` gains the backend's working placeholders, the same ones pokedex-api ships.
  - The app is at <http://localhost:8000> and Swagger UI at <http://localhost:8080/swagger-ui.html>.
- **End-to-end tests with Playwright** (Chromium, in the official Docker image) against that stack, through `scripts/e2e.sh`, which starts a fresh, throwaway stack and tears it down. The journeys:
  - **a visitor browses:** the catalog, paging, a Pokémon's page, its evolution chain and the way back; the finder
  - **an account:** register, sign out, sign in, a wrong password
  - **the local Pokédex as `trainer`:** filters, sync, tags in place, an edit, and a conflict made by a second client
  - **as `admin`:** delete
  - **visitors are sent to sign in** and back
- **Accessibility checks:** axe (`@axe-core/playwright`) runs on every main page in the e2e suite, and any serious or critical violation fails it.
- **CI:** a new "End-to-end" job runs `scripts/e2e.sh` on every push and pull request, and uploads the Playwright report and traces when it fails.
- **Screenshots:** the main screens at desktop and phone widths, captured by Playwright from the real stack into `docs/screenshots/` and shown in the README.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `web-delivery`: adds "Full stack in one command".
- `web-quality`: adds "End-to-end tests" and "Automated accessibility checks"; "Continuous integration" now includes the end-to-end job.

## Out of Scope

- Visual regression testing: screenshots are documentation, not assertions.
- Other browsers: Chromium only. The app uses no browser-specific APIs, and the unit tests cover behaviour.
- Stubbing PokeAPI for the end-to-end run. The suite uses the real one, through the backend's cache, retries and circuit breaker, so it also exercises them. The cost is a dependency on PokeAPI being up, which CI retries absorb (see the design).

## Impact

- **New files:** `docker-compose.full.yml`, `scripts/e2e.sh`, `e2e/*` (Playwright config and journeys), `docs/screenshots/*`.
- **Changed:** `.env.example`, `.github/workflows/ci.yml`, `README.md`, and the tooling configs (TypeScript, ESLint and Vitest) so they know about `e2e/`.
- **Dependencies:** `@playwright/test` 1.63.0 and `@axe-core/playwright` 4.13.0 (dev only).
- **Backend:** none. pokedex-api's README will point to this full stack in a later docs change there.
