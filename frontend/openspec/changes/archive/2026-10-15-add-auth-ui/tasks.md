# Tasks: add-auth-ui

TDD convention: each `test:` commit is red on assertions against stubs, and the `feat:` commit makes it green.

## 1. Rules and storage

- [x] 1.1 Add `react-hook-form`, `@hookform/resolvers` and `zod`. Write `schemas.test.ts`, `session.test.ts` and `safeNext.test.ts` (D1, D3, D4). Verify: red against stubs.
- [x] 1.2 Implement `features/auth/schemas.ts`, `session.ts` and `safeNext.ts`. Verify: 1.1 green.

## 2. Session state and routing

- [x] 2.1 Write `auth.test.tsx` (D2, D3, D5):
  - `ProtectedRoute` redirect and return
  - restore on reload
  - 401 sends the user back to sign-in with the notice
  - expiry timer
  - sign-out clears storage and the query cache
  - header states
  - Verify: red.
- [x] 2.2 Implement `api.ts`, `AuthProvider` (with `configureApiClient` wiring), `useAuth`, `ProtectedRoute`, `AuthNav` and `AppProviders`, and wire them into `App` and the test helper. Verify: 2.1 green.

## 3. Pages

- [x] 3.1 Write `LoginPage.test.tsx` and `RegisterPage.test.tsx` (D4). Verify: red.
- [x] 3.2 Implement the shared form components, `LoginPage`, `RegisterPage`, and the `/login` and `/register` routes. Verify: 3.1 green; lint, type-check and coverage gate green.

## 4. Verification

- [x] 4.1 Run every gate in Docker from a clean checkout. Verify: all pass; record coverage.
- [x] 4.2 Against the real pokedex-api (`docker compose up` there, with the demo data) and the dev server, in a real browser:
  - register a new user
  - sign out
  - sign in as `trainer`
  - reload (still signed in)
  - check the header's admin badge as `admin`
  - a wrong password
  - read the console
  - Verify: as in the spec; zero console warnings or errors.
- [x] 4.3 Update the README (auth flow, the session-storage choice). Run `openspec validate add-auth-ui --strict` and gitleaks. Verify: valid, no leaks; lychee shows no broken links.

## Workflow follow-up

- Push, open the PR and wait for CI to go green.
- Archive as the PR's last commit, wait for CI to be green again, then squash-merge.
- Send the status report.
