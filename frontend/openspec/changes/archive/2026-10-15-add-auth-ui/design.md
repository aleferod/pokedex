# Design: add-auth-ui

## Context

pokedex-api has three auth endpoints:
- `POST /api/v1/auth/register` → 201 with the account
- `POST /api/v1/auth/login` → `{accessToken, tokenType, expiresAt, user}`
- `GET /api/v1/auth/me`

Tokens are HS256 JWTs valid for one hour, with no refresh token (ADR-0002). Errors are RFC 7807 problems that the web client already turns into `ApiError` (status, type, field errors). `apiRequest` has a seam, `configureApiClient({ getToken, onUnauthorized })`.

## Goals / Non-Goals

**Goals:**
- One source of truth for "who is signed in", readable synchronously by the API client.
- Forms that reject what the API would reject, with the same words, before a request is sent.
- A 401 anywhere ends the session predictably and sends the user back where they were after signing in.

**Non-Goals:**
- Refresh tokens, persistent sessions across tabs or restarts, and password reset.

## Decisions

### D1. Where the token lives: `sessionStorage`
| Option | Survives reload | Cross-tab | XSS exposure | Notes |
|---|---|---|---|---|
| memory only | no | no | lowest | Reloading signs you out; a poor demo. |
| **sessionStorage** | **yes** | **no** | readable by injected script | Gone when the tab closes; one tab = one session. |
| localStorage | yes | yes | readable by injected script, kept for days | Longest exposure window. |
| HttpOnly cookie | yes | yes | not readable by script | Needs backend changes (cookie issuance, CSRF protection); out of scope. |

`sessionStorage` is chosen.
- **Exposure:** it keeps a stolen-token window to one tab's life, and the token itself expires within an hour.
- **XSS:** the real defence is not having XSS. React escapes output, and the app has no `dangerouslySetInnerHTML`.
- **Stored session:** stored under `pokedex.session` as `{token, expiresAt, user}`, validated with Zod on load. Expired or invalid data is removed.

### D2. Auth state: a small context, not TanStack Query
- **What it is:** `AuthProvider` holds `{status, session, notice}` in `useState`, initialised synchronously from storage, so there's no flash of "signed out" on reload.
- **Actions:** it exposes `signIn(credentials)`, `register(values)` and `signOut(notice?)`.
- **API client wiring:** it calls `configureApiClient` in a **layout** effect, with `getToken` reading a ref and `onUnauthorized` calling `signOut('expired')` only when a session exists, so a failed sign-in isn't an "expiry". The tests showed why it has to be a layout effect: React runs effects child-first, so a page's query fired before a plain effect had configured the client, got a 401 and ended a valid session right after a reload.
- **Sign-out from a protected page:** navigate home first, then end the session. Otherwise `ProtectedRoute` redirects to sign-in, which the tests also caught.
- **Expiry:** a timer signs out at `expiresAt`, capped at 24 days so it never overflows `setTimeout`.
- **Sign-out:** clears storage, sets the anonymous state and runs `queryClient.clear()`, so no protected data outlives the session.
- **Why not TanStack Query:** the session is client state that the API client must read synchronously, and the login call is a mutation. TanStack Query stays for server data.

### D3. Routing
- **New routes:** `/login` and `/register`, both inside the layout.
- **`ProtectedRoute`:** a layout route. If `status !== 'authenticated'`, it renders `<Navigate to="/login?next=…" replace />` with the current path, search and hash. Otherwise it renders `<Outlet />`. The protected children arrive with the local-management change.
- **`safeNext(value)`:** accepts only strings starting with a single `/` (not `//` or `/\`), and returns `/` otherwise. Both pages use it after success.
- **Already signed in:** visiting `/login` or `/register` redirects to `next` or home.

### D4. Forms
- **Field components:** shared in `src/shared/ui/form`:
  - `TextField`: label, input, optional hint, error list, with `aria-invalid` and `aria-describedby` wired
  - `FormAlert`: `role="alert"`
  - `SubmitButton`: shows `aria-busy` and the busy label
- **Validation:** React Hook Form with `zodResolver`, `mode: 'onTouched'`, `criteriaMode: 'all'` (every unmet password rule is listed) and `shouldFocusError`.
- **Schemas in `features/auth/schemas.ts` mirror the backend:**
  - **username:** trimmed, lower-cased, `/^[a-z0-9._-]{3,30}$/`, message "must be 3 to 30 characters: letters, digits, '.', '_' or '-'"
  - **email:** trimmed, lower-cased, `/^[a-z0-9._%+-]+@[a-z0-9-]+(\.[a-z0-9-]+)+$/`, at most 254 characters, message "must be a valid email address"
  - **password:** each rule is a separate issue with the backend's own text: length 8–72 code points and at most 72 UTF-8 bytes, an upper-case letter, a lower-case letter, a digit, and a character that is neither a letter nor a digit
  - **sign-in:** username and password only need to be present. The server decides the rest, and a wrong combination stays a 401.
- **Server errors:**
  - `ApiError.fieldErrors` → `setError(field, …)` for known fields
  - 409 → `setError` on the field the problem names (`errors[0].field`)
  - 401 on sign-in → the form-level alert
  - anything else → the form-level alert with `error.message`
- **Display:** messages start with a capital letter, through one `sentence()` helper used for client and server messages alike.

### D5. Header controls
`AuthNav`, inside the main navigation:
- **Visitors:** "Sign in" and "Register" links.
- **Signed-in users:** "Signed in as <username>" (plus an "Admin" badge when `role === 'ADMIN'`) and a "Sign out" button, which signs out and navigates home.

### D6. Testing
| Test | What it covers |
|---|---|
| `schemas.test.ts` | every backend rule, including normalisation, bytes vs characters, and all password rules listed |
| `session.test.ts` | save, load, clear; expired, malformed and wrong-shape data discarded |
| `safeNext.test.ts` | same-app paths kept; `//`, `/\`, absolute URLs and empty values become `/` |
| `LoginPage.test.tsx` (MSW) | success with `next`, 401, 400 field errors, network failure, required fields with focus, the expiry notice, already signed in |
| `RegisterPage.test.tsx` | client validation (nothing sent), success signing in, 409 on username and on email, 400 password errors from the server |
| `auth.test.tsx` | `ProtectedRoute` redirect with `next`; return after sign-in; session restored on reload; a 401 from a protected call returns to sign-in with the notice; the expiry timer (fake timers); sign-out clears storage and the query cache; header states |

## Risks / Trade-offs

- [XSS could read `sessionStorage`] → there are no HTML injection sinks, the token lives at most an hour, and storage is scoped to the tab. HttpOnly cookies are listed as future work, since they need backend support.
- [Duplicated validation rules] → they're kept in one schemas module, with tests pinned to the backend's messages. The server stays the authority: its 400s are shown anyway.
- [One tab = one session] → a deliberate consequence of D1, mentioned in the README.
