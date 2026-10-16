# Proposal: add-user-auth

## Why

The exercise asks for "an auxiliary API for user registration, authentication, and the management of protected versus public routes". Its user stories also depend on it: syncing (US03) and changing (US04) the local replica are only for authenticated users, and deleting is only for administrators. Every later change relies on having callers with an identity and a role, so this comes straight after the platform foundation.

## What Changes

- **Users:** a `users` table (UUID id, username, email, BCrypt password hash, role `USER`/`ADMIN`, timestamps), owned by a Flyway migration.
- **Registration:** `POST /api/v1/auth/register`. It enforces username, email and password rules, returns 409 on a duplicate username or email and 400 on a weak password. New accounts always get the `USER` role.
- **Login:** `POST /api/v1/auth/login` returns a signed bearer token (JWT) with a limited lifetime. Bad credentials return 401, and the response is the same whether the username exists or not.
- **Current user:** `GET /api/v1/auth/me` returns the authenticated user.
- **Access control:**
  - Public: catalog, auth endpoints, API docs, health and info.
  - Bearer token required: `/api/v1/local-pokemon/**` and `/api/v1/auth/me`.
  - `ADMIN` required: `DELETE` on `/api/v1/local-pokemon/**`.
  - 401 and 403 answers are problem details. The API is stateless, with no session and no cookies.
- **Field-level 400 errors:** bean-validation failures, malformed JSON, wrong types and unknown fields return `errors[]` entries naming the field. This is new behaviour of the existing `api-errors` capability.
- **Signing secret:** the token-signing secret is required outside tests and must be at least 32 bytes. Startup fails naming `JWT_SECRET` otherwise. This is new behaviour of the existing `service-configuration` capability.
- **Supporting changes:** ADR-0002 (stateless JWT), the README endpoint table and auth section, `.env.example` (`JWT_SECRET`), and the OpenAPI bearer security scheme.

## Capabilities

### New Capabilities
- `user-accounts`: registering accounts, with username, email and password rules, uniqueness, roles and safe password storage.
- `authentication`: exchanging credentials for a bearer token, token properties, and identifying the current caller.
- `access-control`: which routes are public, which need an authenticated caller, which need the `ADMIN` role, and how rejections look.

### Modified Capabilities
- `api-errors`: adds a requirement for field-level entries in `errors[]` on input errors (validation, malformed JSON, wrong types, unknown fields).
- `service-configuration`: adds a requirement that the token-signing secret must be supplied and strong enough.

## Out of Scope

- Seeded demo users (`admin` / `trainer`). They arrive with `add-seed-data`.
- Refresh tokens, logout or token revocation, password reset, email verification, account lockout and rate limiting. These are listed as future improvements in design.md.
- Managing users (listing users, changing roles). There is no `ADMIN` user-management API yet.
- Pokémon endpoints. The protected local-Pokémon routes are only secured here; they are implemented in `add-local-sync` and `add-local-crud`.

## Impact

- **Serves:** the auxiliary auth API and public/protected routes from the PDF's API requirements, and the "secondary collection for user management" from its database requirement. It is a prerequisite for US03 and US04.
- **Code:**
  - `domain.user`: entity, value objects, password policy, exceptions
  - `application`: ports and three use cases
  - `adapter.out.persistence.user`
  - `adapter.out.security`: BCrypt and JWT
  - `adapter.in.web.auth`
  - `infrastructure.security`
  - `GlobalExceptionHandler`: field-level errors and domain-exception mapping
- **Database:** new migration `V2__create_users.sql`.
- **Dependencies:** Spring Security 7 OAuth2 resource server (Nimbus JOSE for JWT), MapStruct 1.6. For tests: Spring Security test and the Data JPA test slice.
- **Configuration:** new `JWT_SECRET` (required), plus optional `JWT_TTL` and `BCRYPT_STRENGTH`.
