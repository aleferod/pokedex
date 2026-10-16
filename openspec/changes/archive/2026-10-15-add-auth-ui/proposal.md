# Proposal: add-auth-ui

## Why

Everything about the local Pokédex (US03 sync, US04 CRUD) is protected by pokedex-api, and deleting needs the `ADMIN` role. The web app needs a way to register, sign in, stay signed in while the token is valid, and recover cleanly when it isn't. It also needs one place where every other feature gets the token from. This is the brief's auxiliary auth requirement for the frontend: login/register pages, auth state, `ProtectedRoute`, and a JWT interceptor with 401 handling.

## What Changes

- **Sign in** (`/login`) and **register** (`/register`) pages, built with React Hook Form and Zod.
  - The client-side rules mirror the backend exactly:
    - username: 3–30 characters of letters, digits, `.`, `_` or `-`, any case
    - email: the backend's syntax
    - password: 8–72 characters (and at most 72 bytes) with upper case, lower case, a digit and a symbol, every unmet rule listed at once
  - Server answers are shown in place:
    - 400 field errors appear next to their fields
    - a 409 "username/email taken" appears on that field
    - a 401 shows "Invalid username or password."
    - network and 5xx failures show a form-level message
  - Registering signs the new account in straight away.
- **Session:** the token, its expiry and the account are kept in `sessionStorage`, so they survive a reload in the same tab.
  - An expired or malformed stored session is discarded.
  - The app signs out on its own when the token expires.
  - Signing out clears the session and every cached query.
- **API client wiring:** every request carries `Authorization: Bearer …`. A 401 from any call signs the user out, and the user returns to the sign-in page with "Your session has expired" and a `next` link back.
- **`ProtectedRoute`:** an anonymous visitor goes to `/login?next=<current path>`, and returns there after signing in. The `next` target is sanitised, so only same-app paths are used and no open redirect is possible.
- **Header:** "Sign in" and "Register" for visitors; for a signed-in user, "Signed in as <username>" (with an "Admin" badge for admins) and "Sign out".

## Capabilities

### New Capabilities
- `authentication-ui`: sign-in, registration, session lifetime, protected routes and their error handling.

### Modified Capabilities
- None. `app-shell`'s header gains the auth controls, and `api-client`'s token and 401 seam is used as designed.

## Out of Scope

- Refresh tokens, "remember me" across browser sessions, and password reset: the backend has none (ADR-0002 in pokedex-api).
- Role management screens; accounts are registered as `USER`, and the demo `admin` comes from seed data.
- The protected screens themselves (`add-local-pokemon-management-ui`).

## Impact

- **New dependencies:** `react-hook-form` 7.90, `@hookform/resolvers` 5.9 and `zod` 4.6.
- **Code:**
  - `src/features/auth/*`
  - shared form components in `src/shared/ui/form/*`
  - an `AppProviders` component used by `App` and by tests
- **Backend:** none.
