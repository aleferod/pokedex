# Tasks: bootstrap-web

TDD convention: each `test:` commit is red on assertions against stubs, and the `feat:` commit makes it green.

## 1. Toolchain

- [x] 1.1 Add `scripts/npm-in-docker.sh`, `package.json` with pinned versions (D1), TypeScript strict configs, the Vite config (React, Tailwind, `@/` alias, dev proxy), ESLint flat config, Prettier, and Vitest setup (jest-dom, MSW with unhandled = error, console guard, coverage thresholds) (D2, D5, D7). Verify: `npm ci`, lint, type-check and an empty test run work in Docker.

## 2. API client

- [x] 2.1 Write `client.test.ts` with MSW: base URL and headers, JSON body, 204, problem → `ApiError` (400 field errors grouped, 409 type and detail, correlation id), non-problem 502, network failure, abort passthrough; and `queryClient.test.ts` for the retry policy (D3, D4). Verify: red against stubs.
- [x] 2.2 Implement `src/shared/api` (`apiRequest`, `ApiError`, problem types, `configureApiClient` seam) and `src/app/queryClient.ts`. Verify: 2.1 green.

## 3. App shell

- [x] 3.1 Write tests for the layout (skip link first in tab order and focusing `main`, landmarks, nav labelled "Main"), home title, the not-found route (text, home link, title), the error boundary (fallback, "Try again" recovers, layout kept), and a guard test that a React key warning fails a test (D5). Verify: red against stubs.
- [x] 3.2 Implement `App`, router, `Layout`, `HomePage`, `NotFoundPage`, `ErrorBoundary`, `useDocumentTitle`, Tailwind base styles with visible focus, and the favicon. Verify: 3.1 green; coverage gate met.

## 4. Delivery and CI

- [x] 4.1 Add the `Dockerfile`, `nginx/default.conf.template` (SPA fallback, `/api/` proxy via `API_UPSTREAM`, caching, security headers, `/healthz`), `.dockerignore` and `.env.example` (D6). Verify: build the image, run it, `curl` `/healthz`, a deep link, the headers and a proxied `/api/` call against a stub upstream.
- [x] 4.2 Add `.github/workflows/ci.yml` (D8) and the README (setup, env vars, scripts, structure, API communication, testing, Docker). Verify: workflow YAML parses; lychee shows no broken links.

## 5. Verification

- [x] 5.1 Run `npm ci`, `lint`, `format:check`, `typecheck`, `test:coverage` and `build` in Docker from a clean checkout. Verify: all pass; record the coverage numbers.
- [x] 5.2 Open the dev server and the production image in a real browser; tab through, open a deep link and a 404, and read the console. Verify: zero console warnings or errors, and no favicon 404.
- [x] 5.3 Run `openspec validate bootstrap-web --strict` and gitleaks. Verify: valid, no leaks.

## Workflow follow-up

- Push, open the PR and wait for CI to go green.
- Archive as the PR's last commit, wait for CI to be green again, then squash-merge.
- Send the status report.
