# Pokédex Web

[![CI](https://github.com/aleferod/pokedex-web/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/aleferod/pokedex-web/actions/workflows/ci.yml)
![React 19](https://img.shields.io/badge/React-19-61dafb)
![TypeScript strict](https://img.shields.io/badge/TypeScript-strict-3178c6)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

The frontend for [pokedex-api](https://github.com/aleferod/pokedex-api), the Ballast Lane Java technical exercise. It browses the Pokémon catalog and lets signed-in users manage a local Pokédex with their own notes, tags and localized names. It's built spec-first with [OpenSpec](https://github.com/Fission-AI/OpenSpec) and test-first.

> **Status:** `bootstrap-web` is done: the application shell, the API client, quality gates, the Docker image and CI. The screens arrive one OpenSpec change at a time; see [Roadmap](#roadmap).

## Contents

- [Quick start](#quick-start)
- [Configuration](#configuration)
- [Scripts](#scripts)
- [Project structure](#project-structure)
- [State management](#state-management)
- [Talking to the API](#talking-to-the-api)
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

## Testing and quality gates

```bash
./scripts/npm-in-docker.sh run test:coverage
```

- **Tooling:** Vitest, React Testing Library and **MSW** at the network layer. An unmocked request is an error, not a silent pass.
- **Zero console warnings:** any `console.error` or `console.warn` during a test fails that test. That covers React key and `act()` warnings and library deprecations. [`consoleGuard.test.tsx`](src/test/consoleGuard.test.tsx) proves a missing `key` is caught.
- **Coverage gate:** at least 85 % of lines, statements and functions, and 80 % of branches.
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
2. `add-auth-ui`: register, login, token handling, protected routes
3. `add-catalog-ui`: the paginated catalog (US01)
4. `add-detail-ui`: the detail view with stats and the evolution chain (US02)
5. `add-local-pokemon-management-ui`: sync, list, filter, create, edit and delete local records (US03/US04)
6. `add-e2e-and-polish`: Playwright end-to-end tests, the full-stack Compose file, screenshots

## License

[MIT](LICENSE)
