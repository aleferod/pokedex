# Proposal: bootstrap-web

## Why

The frontend repository has only its OpenSpec context. Every user story screen (catalog, detail, local management) needs the same foundation first:
- an application shell with routing and accessible layout
- one API client that understands the backend's RFC 7807 errors
- a toolchain that fails on warnings
- a Docker image that serves the SPA and proxies `/api`
- CI

It serves no user story directly. It's the auxiliary platform requirement for US01–US04.

## What Changes

- **Project:** Vite 8 + React 19 + TypeScript 6 (strict), React Router 8 and TanStack Query 5. Tailwind CSS 4 is mobile-first.
  - Feature-based layout: `src/app`, `src/features/*` and `src/shared`.
  - Node 24 LTS, with every npm command run in Docker (`scripts/npm-in-docker.sh`).
- **App shell:**
  - a header with the brand and navigation
  - a skip-to-content link and one `main` landmark
  - a responsive container (360 px and up) and visible focus styles
  - a 404 page for unknown routes
  - an error boundary page with "Try again"
  - document titles per page, and a favicon so there's no 404 for it
- **API client (`src/shared/api`):** a typed `fetch` wrapper.
  - The base URL comes from `VITE_API_BASE_URL` (default `/api/v1`).
  - JSON in and out; 204 without a body; abort signals.
  - Every failure becomes one `ApiError`, carrying status, problem type, title, detail, field errors and the correlation id. Network failures and non-problem error bodies are included.
- **Query defaults:** don't retry 4xx answers, retry 5xx and network failures once, and keep data fresh for 60 s.
- **Quality:**
  - ESLint 10 (typescript-eslint strict type-checked, react-hooks, jsx-a11y-x, react-refresh), with zero warnings allowed
  - a Prettier check and `tsc --noEmit`
  - Vitest 5 with React Testing Library and MSW 3. Unhandled requests are errors, any `console.error` or `console.warn` fails the test, and there's a coverage gate.
- **Docker:** a multi-stage image (`node:24-alpine` builds, `nginx:alpine` serves).
  - SPA fallback; `/api/` proxied to `API_UPSTREAM` (default `http://api:8080`)
  - long-lived caching for hashed assets and no caching for `index.html`
  - basic security headers and a health check
- **CI (GitHub Actions):** lint, format check, type-check, test with coverage, build, Docker build, `openspec validate --strict`, and gitleaks.

## Capabilities

### New Capabilities
- `app-shell`: layout, navigation, routing fallbacks, error boundary, accessibility baseline.
- `api-client`: how the SPA talks to pokedex-api and how errors are represented.
- `web-delivery`: the container image, nginx behaviour and runtime configuration.
- `web-quality`: the gates every change must pass, locally and in CI.

### Modified Capabilities
- None.

## Out of Scope

- Authentication (`add-auth-ui`): JWT storage, the 401 interceptor, protected routes.
- Catalog, detail and local-management screens.
- End-to-end tests (`add-e2e-and-polish`).
- The full-stack Compose file, which comes with `add-e2e-and-polish`.

## Impact

- **Repository:** first code in `pokedex-web`.
- **Backend:** none. The SPA calls the API through the same origin (`/api` proxy in nginx and in the Vite dev server), so no CORS changes are needed.
- **Versions:** see the design (D1). TypeScript is deliberately one major behind the latest.
