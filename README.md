# Pokédex Web

[![CI](https://github.com/aleferod/pokedex-web/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/aleferod/pokedex-web/actions/workflows/ci.yml)
![React 19](https://img.shields.io/badge/React-19-61dafb)
![TypeScript strict](https://img.shields.io/badge/TypeScript-strict-3178c6)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

The frontend for [pokedex-api](https://github.com/aleferod/pokedex-api), the Ballast Lane Java technical exercise. It browses the Pokémon catalog and lets signed-in users manage a local Pokédex with their own notes, tags and localized names. It's built spec-first with [OpenSpec](https://github.com/Fission-AI/OpenSpec) and test-first.

> **Status:** `bootstrap-web`, `add-auth-ui`, `add-catalog-ui`, `add-detail-ui` and `add-local-pokemon-management-ui` are done: the application shell, the API client, quality gates, the Docker image, CI, registration and sign-in with protected routes, the paginated catalog (US01), the Pokémon detail page (US02), and the local Pokédex with sync, editing and optimistic concurrency (US03/US04). End-to-end tests and the full-stack Compose file come next; see [Roadmap](#roadmap).

## Contents

- [Quick start](#quick-start)
- [Configuration](#configuration)
- [Scripts](#scripts)
- [Project structure](#project-structure)
- [State management](#state-management)
- [Talking to the API](#talking-to-the-api)
- [Authentication](#authentication)
- [Catalog](#catalog)
- [Pokémon detail](#pokémon-detail)
- [My Pokédex](#my-pokédex)
- [Testing and quality gates](#testing-and-quality-gates)
- [Docker](#docker)
- [Spec-driven workflow](#spec-driven-workflow)
- [Roadmap](#roadmap)

## Quick start

You only need **Docker**. Every npm command runs in a Node 24 LTS container through [`scripts/npm-in-docker.sh`](scripts/npm-in-docker.sh), as your user, so nothing on disk ends up owned by root.

```bash
./scripts/npm-in-docker.sh ci             # install the exact locked versions
./scripts/npm-in-docker.sh run dev        # http://localhost:5173, /api proxied to localhost:8080
```

Start [pokedex-api](https://github.com/aleferod/pokedex-api) next to it (`docker compose up` in that repository), and the dev server proxies `/api` to it. The browser only ever talks to one origin.

To run the production image instead:

```bash
docker build -t pokedex-web .
docker run --rm -p 8081:80 -e API_UPSTREAM=http://host.docker.internal:8080 \
  --add-host host.docker.internal:host-gateway pokedex-web
# http://localhost:8081
```

## Configuration

| Variable              | When              | Default                 | Purpose                                                                                                                           |
| --------------------- | ----------------- | ----------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| `VITE_API_BASE_URL`   | build             | `/api/v1`               | Where the browser calls the API. Same origin by default. It's embedded in the bundle, so never put secrets in `VITE_*` variables. |
| `VITE_DEV_API_TARGET` | dev server        | `http://localhost:8080` | Where `npm run dev` proxies `/api`                                                                                                |
| `API_UPSTREAM`        | container runtime | `http://api:8080`       | Where nginx proxies `/api/`. Changing it doesn't need a rebuild.                                                                  |

Local overrides go in `.env.local` (git-ignored). [`.env.example`](.env.example) lists the variables.

## Scripts

| Script                    | What it does                                                                                                            |
| ------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `dev`                     | Vite dev server with hot reload and the `/api` proxy                                                                    |
| `build`                   | Production build into `dist/`                                                                                           |
| `preview`                 | Serves `dist/` on http://localhost:4173                                                                                 |
| `lint`                    | ESLint with **zero warnings** allowed (typescript-eslint strict type-checked, React hooks, accessibility, Fast Refresh) |
| `format` / `format:check` | Prettier write / check                                                                                                  |
| `typecheck`               | `tsc` in strict mode for the app and the build config                                                                   |
| `test` / `test:watch`     | Vitest with React Testing Library and MSW                                                                               |
| `test:coverage`           | Tests plus the coverage gate                                                                                            |

Run any of them with `./scripts/npm-in-docker.sh run <script>`.

## Project structure

```
src/
  app/        router, routes, providers (query client), layout, app-level pages
  features/   one folder per feature (auth, catalog, detail, local-pokemon), added change by change
  shared/
    api/      the API client, ApiError and problem-document parsing
    hooks/    small reusable hooks (useDocumentTitle)
    ui/       shared components (ErrorBoundary)
  test/       Vitest setup: MSW server, console guard, render helpers
```

Features don't import each other's internals; anything two features need goes to `shared/`. Imports use the `@/` alias for `src/`.

## State management

- **Server state lives in TanStack Query:** caching, pagination, retries and optimistic updates. There's no global store holding API data.
- **Default retry policy:** a query isn't retried after a 4xx answer (bad input, not found, not allowed and conflict won't change by asking again). It's retried once after a 5xx or a network failure. Data stays fresh for 60 s.
- **Shareable state lives in the URL:** page, search and filters.
- **Local UI state stays in components.**

## Talking to the API

Every call goes through `apiRequest` in [`src/shared/api`](src/shared/api):

- It prefixes `VITE_API_BASE_URL`, sends and accepts JSON, supports cancellation, and resolves `204` to nothing.
- **Every failure is an `ApiError`,** built from pokedex-api's RFC 7807 problem documents:
  - `status` and `type` (the problem's short name, such as `conflict` or `bad-request`)
  - `title`, `detail`, and a `message` ready to show
  - `fieldErrors`, grouped by field, for placing messages next to inputs
  - the `correlationId` to quote in a bug report
- **Network failures** become `status: 0` with `isNetworkError`. A cancelled request rejects with its original `AbortError`.
- **Authentication plugs in** through `configureApiClient({ getToken, onUnauthorized })`, used by the auth feature.

## Authentication

| Page        | What it does                                                                    |
| ----------- | ------------------------------------------------------------------------------- |
| `/register` | Creates an account (always `USER`) and signs it in                              |
| `/login`    | Signs in, then returns to `next` (only same-app paths; anything else goes home) |

- **The rules mirror pokedex-api exactly,** in [`features/auth/schemas.ts`](src/features/auth/schemas.ts), so the form rejects what the API would and in the same words:
  - username: 3–30 characters of letters, digits, `.`, `_` or `-`, any case
  - email: the backend's syntax
  - password: 8–72 characters, and at most 72 bytes, with upper case, lower case, a digit and a symbol. **Every** unmet rule is listed at once.
- **The API stays the authority:** its 400 field errors and the field a 409 names ("is already taken") appear next to the inputs.
- **A wrong sign-in** shows "Invalid username or password." and never says which part was wrong.
- **The session** (token, expiry, account) lives in `sessionStorage`:
  - It survives a reload in the same tab and is gone when the tab closes, so one tab is one session.
  - An expired or malformed stored session is discarded.
  - The app signs out when the token expires.
  - **Why not `localStorage` or cookies:** injected script could read either web storage, so the real defence is having no XSS sinks (there's no `dangerouslySetInnerHTML`). `sessionStorage` keeps the exposure window to one tab, and the token lives at most an hour. HttpOnly cookies would need backend changes (cookie issuance and CSRF protection).
- **Every request** carries `Authorization: Bearer …`. A 401 from any call ends the session. On a protected page the user lands on `/login?next=<that page>` with "Your session has expired. Please sign in again."
- **Signing out** clears the session and every cached query, so no protected data outlives it.
- **Demo accounts** come from pokedex-api's seed data: `trainer / Trainer@123` (USER) and `admin / Admin@123` (ADMIN, can delete).

## Catalog

`/pokemon` (US01) shows the catalog as a responsive grid, from one column at 360 px up to four on desktop. Each card shows the sprite, `#number`, name, category, weight in kg and abilities, and links to the Pokémon's page.

- **The URL is the state:** `?page` (1-based) and `?size` (20, 40 or 60), so a view can be shared and survives a reload. Defaults are left out (`/pokemon` is page 1 of 20), and nonsense values fall back to them.
- **Paging:**
  - Previous/Next and "Page N of M"; changing the page moves focus to the results heading.
  - The current page stays on screen, marked busy, while the next one loads.
  - The next page is fetched ahead of time.
- **States:**
  - skeleton cards with a "Loading Pokémon…" status
  - "There are no Pokémon on this page" past the end, with a link to the last page
  - an error with "Try again" (a 503 says PokeAPI is temporarily unavailable)
- **Find a Pokémon:** a name or Pokédex number, checked with the API's own identifier rule, opens `/pokemon/<id or name>`. The catalog API has no text search, so this is how you jump to a Pokémon; tag and name filters come with the local Pokédex.

## Pokémon detail

`/pokemon/<number or name>` (US02) shows one Pokémon in full:

- the official artwork, `#number`, name and types, one colour each with at least 4.5:1 contrast
- the category, the description, height in metres, weight in kilograms and abilities
- the six base stats as a table with bars and a total
- the evolution family as a tree

How it behaves:

- **Evolution chain:** the tree starts at the base form. Each stage links to its page and says how it's reached ("Level 16", "Use Thunder Stone"). Branching families such as Eevee's show every branch, and the stage on screen is the current page (`aria-current`). A family of one says "Ditto does not evolve."
- **One address per Pokémon:** `/pokemon/Pikachu` and `/pokemon/025` are replaced by `/pokemon/pikachu` and `/pokemon/25`.
- **States:**
  - a skeleton while loading
  - "Pokémon not found" with the finder, for a 404 or for an identifier the API would reject (that one never calls the API)
  - "Try again" for other failures, where a 503 says PokeAPI is temporarily unavailable
- **The way back:** "Back to the catalog" returns to the catalog page and size you came from, even after following the evolution chain. Opened directly, it goes to page 1.
- **Focus:** after moving to a Pokémon inside the app (a card, a stage), focus goes to its name, so keyboard and screen-reader users start at the new content. In the main navigation, "Catalog" is then the current section (`aria-current="true"`), not the current page.

## My Pokédex

`/local` (US03/US04) is the local Pokédex: Pokémon copied from PokeAPI into pokedex-api's database, with your own localized names, region, habitat notes, tags and notes. It needs a session; visitors go to sign-in and come back to the same address.

- **List:** a page at a time, with name and tag filters in the URL alongside the page and size (`/local?name=saur&tag=starter`), so a filtered view can be shared. Each tag on a record links to the list filtered by it.
- **Sync from PokeAPI:**
  - One Pokémon by name or number. Pikachu, for example, is imported (201) or refreshed (200), and the page says that your fields were kept.
  - The first 1–20 catalog entries, with a summary such as "Synced 5 Pokémon: 0 imported, 5 updated."
  - A record's page can re-sync it too.
- **A record's page:** the PokeAPI data is marked read-only (types, measures, abilities, base stats, sync date); then come your fields, who added the record and when it was updated. Tags are added and removed in place, **optimistically**, and undone with an explanation if the API refuses.
- **Create and edit:** the forms use the API's own rules and messages (lengths, the tag format, locales, at most 10 localized names). Every broken rule is listed at once, and the API's 400s land on the same fields, a localized name on its row.
- **No lost updates:**
  - Every write sends `If-Match` with the version you saw; the API's `ETag` is the record's `version` in quotes.
  - The edit form keeps the version it loaded, even if the page refreshes the record in the background.
  - When someone else saved first (409), your input stays and "Load the latest version" refills the form.
- **Roles:** only administrators see "Delete". It asks for confirmation inline (focus on "Cancel"), sends `If-Match`, and returns to the list with a message. With the demo data, sign in as `admin / Admin@123` to delete, or as `trainer / Trainer@123` for everything else.

## Testing and quality gates

```bash
./scripts/npm-in-docker.sh run test:coverage
```

- **Tooling:** Vitest, React Testing Library and **MSW** at the network layer. An unmocked request is an error, not a silent pass.
- **Zero console warnings:** any `console.error` or `console.warn` during a test fails that test. That covers React key and `act()` warnings and library deprecations. [`consoleGuard.test.tsx`](src/test/consoleGuard.test.tsx) proves a missing `key` is caught.
- **Coverage gate:** at least 85 % of lines, statements and functions, and 80 % of branches.
- **About the browser console:** the app itself logs nothing. When the API answers an expected error, such as a wrong password's 401, Chrome adds its own "Failed to load resource" line for that request. That's the browser reporting an HTTP status, not an application warning, and it appears only in those error cases.
- **Other gates:** strict TypeScript (`noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`), ESLint with zero warnings, and a Prettier check.

## Docker

The [`Dockerfile`](Dockerfile) builds in two stages: `node:24-alpine` runs `npm ci` and the build, and `nginx:1.31-alpine` serves `dist/` ([config](nginx/default.conf.template)):

- **SPA fallback:** deep links and reloads get `index.html`.
- **Caching:** hashed `/assets/*` are cached for a year as immutable; `index.html` isn't cached.
- **API proxy:** `/api/` goes to `API_UPSTREAM`, resolved per request, so the container starts even before the backend does.
- **Security headers:** `X-Content-Type-Options`, `Referrer-Policy` and `X-Frame-Options` on every response.
- **Health check:** `GET /healthz`.

## Spec-driven workflow

- **Where specs live:** every change starts as an OpenSpec proposal in `openspec/changes/<id>/` (proposal, design, tasks, spec deltas), validated with `openspec validate --strict`.
- **How it's built:** test-first, with failing `test:` commits before `feat:` commits.
- **How it ends:** the change is archived into [`openspec/specs`](openspec/specs) as the last commit of its pull request.

CI runs these jobs on every push and pull request:

- lint, format check, type-check, tests with coverage, and the production build
- the Docker image build
- spec validation
- a gitleaks secret scan

## Roadmap

1. ✅ `bootstrap-web`: shell, API client, gates, Docker, CI
2. ✅ `add-auth-ui`: register, login, token handling, protected routes
3. ✅ `add-catalog-ui`: the paginated catalog (US01)
4. ✅ `add-detail-ui`: the detail view with stats and the evolution chain (US02)
5. ✅ `add-local-pokemon-management-ui`: sync, list, filter, create, edit and delete local records (US03/US04)
6. `add-e2e-and-polish`: Playwright end-to-end tests, the full-stack Compose file, screenshots

## License

[MIT](LICENSE)
