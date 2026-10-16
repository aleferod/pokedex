# Design: bootstrap-web

## Context

`pokedex-api` exposes `/api/v1/**`. Every error is an RFC 7807 problem with `type`, `title`, `status`, `detail`, `instance`, `timestamp` and `errors[{field, message}]`, and every response carries `X-Correlation-Id`. This change sets up the frontend repository so feature changes only add screens.

## Goals / Non-Goals

**Goals:**
- One way to call the API and one error type, so every screen handles 400/401/404/409/503 the same way.
- Warnings are failures: lint, types and tests all run with zero warnings allowed.
- The production image behaves like the dev server: same-origin `/api`, and deep links work.

**Non-Goals:**
- Authentication and any feature screen (see the proposal).

## Decisions

### D1. Toolchain and versions (checked on npm, 2026-10-15)
| Package | Version | Note |
|---|---|---|
| react, react-dom | 19.3 | |
| react-router | 8.4 | library ("data router") mode: `createBrowserRouter` |
| @tanstack/react-query | 5.104 | |
| vite | 8.3 | with `@vitejs/plugin-react` 6 and `@tailwindcss/vite` 4.3 |
| typescript | **6.0** | 7.0 is the latest, but typescript-eslint 8.71 supports `<6.1`. Type-aware linting matters more than the native compiler. |
| eslint | 10.12 | with `eslint-plugin-jsx-a11y-x` 0.2, the maintained fork (es-tooling) of `eslint-plugin-jsx-a11y`. The original declares only ESLint ≤ 9, and npm marks ESLint 9 as no longer supported. |
| vitest | 5.0 | with jsdom 30, @testing-library/react 16 and MSW 3 |
| Node | 24 LTS | in Docker (`node:24-alpine`); the host needs only Docker |

### D2. Structure
- `src/app`: `App`, the router, providers (`QueryClientProvider`), the layout and app-level pages (home, not found, error).
- `src/features/<feature>`: added by later changes. Features don't import each other's internals.
- `src/shared`:
  - `api` (client, `ApiError`, problem types)
  - `ui` (small components such as `ErrorBoundary`)
  - `hooks` (`useDocumentTitle`)
- Path alias `@/` maps to `src/`.

### D3. API client
- **Core:** `apiRequest<T>(path, { method, body, signal, headers })`, built on `fetch`.
  - The base URL is `import.meta.env.VITE_API_BASE_URL ?? '/api/v1'`.
  - JSON bodies are stringified with `Content-Type: application/json`; `Accept: application/json`.
- **Successful responses:** 204, or an empty body, resolves to `undefined`. Otherwise the JSON is parsed.
- **`ApiError extends Error`:**
  - `status` (`0` for a network failure)
  - `type` (the problem slug from the URI fragment, such as `conflict`)
  - `title` and `detail`
  - `fieldErrors: Record<string, string[]>`
  - `correlationId` (from the header)
  - `isNetworkError`

  A non-problem error body becomes a generic `ApiError` with the HTTP status text. An aborted request rethrows the `AbortError` untouched, so TanStack Query can cancel quietly.
- **Not here yet:** the JWT header and 401 handling arrive in `add-auth-ui`. The client leaves a seam for them: `configureApiClient({ getToken, onUnauthorized })`.

### D4. Query defaults
`retry`: no retry for an `ApiError` with status 400–499, and once for anything else. `staleTime`: 60 s. `refetchOnWindowFocus`: off. These are the defaults; features override them.

### D5. Warnings are test failures
- **Vitest setup:**
  - jest-dom matchers
  - an MSW server with `onUnhandledFrame: 'error'` (MSW 3's name for the unhandled-request strategy)
  - `console.error` and `console.warn` replaced by functions that **throw**, so React's key, act() and prop-type warnings fail the test that caused them
- **Coverage:** v8, for `src/**` excluding entry and type files. Thresholds: 85 % for lines, statements and functions, and 80 % for branches.
- **Lint:** `eslint . --max-warnings 0`.
- **The browser console in dev and prod builds** is checked in the verification task: open both builds in a real browser and read the console.

### D6. Docker and nginx
- **Stages:** `node:24-alpine` runs `npm ci` and `npm run build`. `nginx:alpine` copies `dist/` and a config template.
- **nginx behaviour:**
  - `location /` uses `try_files $uri /index.html` (SPA fallback)
  - `location /api/` proxies to `${API_UPSTREAM}`, which the official image's envsubst-on-templates fills at start (default `http://api:8080`). The target goes through a variable, with `resolver ${NGINX_LOCAL_RESOLVERS}`, so the host is resolved per request: nginx starts even before the backend exists, and answers 502 until it does.
  - `/assets/*` (hashed) gets `Cache-Control: public, max-age=31536000, immutable`; `index.html` gets `no-cache`
  - `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin` and `X-Frame-Options: DENY`
- **Health check:** `wget` on `/healthz`, a static 200 from nginx.
- **Build-time configuration:** `VITE_API_BASE_URL` is a build argument (default `/api/v1`). The runtime proxy target is the `API_UPSTREAM` environment variable.

### D7. Dev server
Vite proxies `/api` to `VITE_DEV_API_TARGET` (default `http://localhost:8080`), so development is same-origin like production.

### D8. CI
- **Jobs:**
  - quality: `npm ci`, lint, format check, type-check, test with coverage, build
  - Docker build
  - `openspec validate --all --strict`
  - gitleaks over the full history
- **Pinned action versions,** as in pokedex-api.

## Risks / Trade-offs

- [TypeScript one major behind] → recorded here, revisited when typescript-eslint supports TypeScript 7.
- [An accessibility plugin fork] → it's young (0.x), but maintained and rule-compatible. The task list checks that its rules fire.
- [Throwing on any console output in tests] → strict, but it's the only way to keep the "zero console warnings" rule honest. A test that expects an error log must stub `console` explicitly.
- [nginx proxying the API] → the browser sees one origin, so there's no CORS. The API URL is runtime configuration of the image, not a rebuild.
