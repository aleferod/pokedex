# ADR-0002: Stateless JWT authentication issued by the API

- **Status:** Accepted
- **Date:** 2026-10-14

## Context

The exercise needs registration, login and "protected versus public routes". The local replica (US03/US04) needs an authenticated caller, and deleting needs an administrator. The client is a separately hosted single-page app (`pokedex-web`), and the API should stay horizontally scalable and simple to run in Docker. There's one service, which both issues and checks credentials.

## Decision

- **Login:** `POST /api/v1/auth/login` verifies the BCrypt password hash and issues a **JWT signed with HMAC-SHA256** (`HS256`):
  - claims `sub` (account id), `username`, `role`, `iss`, `iat`, `exp`
  - lifetime `JWT_TTL`, default 1 hour
  - secret `JWT_SECRET`: at least 32 bytes, required outside tests, and startup fails without it
- **Every protected request:** Spring Security's **OAuth2 resource server** verifies the token:
  - HS256 signature only
  - `exp`/`nbf` with 30 s clock skew
  - issuer
  - It maps the `role` claim to `ROLE_USER` / `ROLE_ADMIN`.
- **No session state:** no server-side session, no cookies, and so no CSRF protection is needed.
- **Error format:** 401 and 403 answers are RFC 7807 problem details. They keep the RFC 6750 `WWW-Authenticate: Bearer` challenge, with `error="invalid_token"` for bad tokens.

## Alternatives considered

- **Server-side session with a cookie.** It needs CSRF protection and a shared session store, and it's awkward for an SPA on another origin. Rejected.
- **RS256 with a published JWKS.** It's the right model once other services verify tokens or keys must rotate without downtime. For a single service it adds key management with no benefit yet, so it's the documented next step.
- **Opaque tokens with a token table.** Revocation is easy, but every request costs a database lookup. Rejected for now.
- **An external identity provider (Keycloak, Auth0).** It's the production-grade answer, but it's out of scope for a self-contained exercise and its one-command demo.

## Consequences

- Positive: horizontally scalable, with no session affinity. The frontend just stores the token and sends `Authorization: Bearer …`.
- Positive: all of this is standard Spring Security, with no hand-written JWT parsing. The application layer only sees the `TokenPort` and `PasswordHasherPort` ports.
- Negative: tokens can't be revoked before they expire. **Mitigation:** a short TTL (1 h). Refresh tokens with revocation are future work.
- Negative: issuing and verifying share one secret, and rotating it logs everyone out. **Mitigation:** move to RS256 with JWKS, as above.
- Negative: login isn't rate-limited yet. **Mitigation:** BCrypt cost slows brute force, and identical answers plus the dummy-hash check stop user enumeration. A rate limiter (for example Bucket4j) is future work.
