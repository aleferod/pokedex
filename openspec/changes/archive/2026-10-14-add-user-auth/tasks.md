# Tasks: add-user-auth

TDD convention as in bootstrap-project: each `test:` commit is red on assertions, the `feat:` commit makes it green, and refactors are separate commits.

## 1. Dependencies and configuration

- [x] 1.1 Add `spring-boot-starter-security-oauth2-resource-server`, MapStruct 1.6.3 (with its processor on the compiler path), and the test starters `spring-boot-starter-security-test` and `spring-boot-starter-data-jpa-test`. Because Boot locks every endpoint as soon as Spring Security is on the classpath, also add a stateless, permit-all `SecurityConfiguration` placeholder that keeps today's behaviour, and import it into `@WebSliceTest`. Group 7 tightens it test-first. Verify: `./scripts/verify-in-docker.sh` stays green, with 0 compiler warnings.
- [x] 1.2 Add `app.security.*` defaults to `application.yml` (D9), a fixed test secret and BCrypt strength 4 to `application-test.yml`, `JWT_SECRET` and `JWT_TTL` to `.env.example` and `docker-compose.yml`, and `spring.jackson.deserialization.fail-on-unknown-properties: true`. Verify: `docker compose config` resolves `JWT_SECRET` from `.env`.

## 2. Domain: users and password policy

- [x] 2.1 Write `UsernameTest`, `EmailAddressTest`, `UserIdTest`, `PasswordHashTest` and `UserTest`:
  - trimming and lower-casing
  - length and character rules
  - `InvalidFieldException` naming the field
  - redacted `toString`
  - `User.register` assigns `USER` and equal timestamps
  - Verify: red against stubs.
- [x] 2.2 Implement the value objects, `Role`, `User` (with `register`/`restore`) and the `DomainException` hierarchy. Verify: 2.1 green; ArchUnit green (`domain` is JDK-only).
- [x] 2.3 Write `PasswordPolicyTest`:
  - each rule's violation
  - all violations collected at once
  - 72/73-character boundary
  - `Admin@123`, `Trainer@123` and `Pikachu@25` accepted
  - Verify: red.
- [x] 2.4 Implement `PasswordPolicy` with the `PasswordRule` chain and `WeakPasswordException`. Verify: 2.3 green.

## 3. Application: ports and use cases

- [x] 3.1 Write `RegisterUserUseCaseTest` (Mockito):
  - happy path: hashes, saves `USER` with clock timestamps, returns the user
  - weak password → `WeakPasswordException` with no repository call
  - duplicate username or email → `DuplicateUserException(field)` and the hasher is never called
  - Verify: red.
- [x] 3.2 Write `AuthenticateUserUseCaseTest`:
  - success returns the issued token and user
  - wrong password → `InvalidCredentialsException`
  - unknown user → `InvalidCredentialsException`, and the dummy hash is still checked
  - username lookup is case-insensitive
  - Verify: red.
- [x] 3.3 Write `GetCurrentUserUseCaseTest`: found → user; missing → `UserNotFoundException`. Verify: red.
- [x] 3.4 Implement the ports (`RegisterUser`, `AuthenticateUser`, `GetCurrentUser`, `UserRepositoryPort`, `PasswordHasherPort`, `TokenPort`, `IssuedToken`) and the three use cases (D3). Verify: 3.1–3.3 green; JaCoCo gate on domain + application ≥ 85 %.

## 4. Persistence

- [x] 4.1 Write `UserPersistenceAdapterIT` (`@DataJpaTest` + Testcontainers):
  - save/find round trip by id and username
  - `existsByUsername` / `existsByEmail`
  - a direct duplicate insert (bypassing the existence checks) → `DuplicateUserException` with the right field
  - the stored hash is not the raw password
  - Verify: red.
- [x] 4.2 Implement `V2__create_users.sql`, `UserJpaEntity`, `SpringDataUserRepository`, `UserPersistenceMapper` (MapStruct) and `UserPersistenceAdapter` (D4). Verify: 4.1 green; `SchemaMigrationIT` now sees V1 and V2; Hibernate `validate` passes.

## 5. Security adapters

- [x] 5.1 Write `BCryptPasswordHasherTest` (hash ≠ raw, starts with `$2`, matches, mismatch, two hashes of the same password differ) and `JwtTokenIssuerTest` (claims `sub`/`username`/`role`/`iss`, `exp` = fixed clock + TTL, decodable with the decoder from the same secret). Verify: red.
- [x] 5.2 Implement `BCryptPasswordHasher`, `JwtTokenIssuer`, `JwtProperties` and `JwtConfiguration` (encoder, decoder, authentication converter). Verify: 5.1 green.
- [x] 5.3 Write `JwtPropertiesTest` (secret shorter than 32 bytes → binding failure stating the minimum) and extend `ProfileConfigurationTest` (docker profile without `JWT_SECRET` → startup fails naming it). Verify: red, then green after adding `app.security.jwt.secret` to `RequiredSettingsEnvironmentPostProcessor`.

## 6. Field-level errors and domain-exception mapping

- [x] 6.1 Write `InputAndDomainErrorsTest` (a web slice next to `GlobalExceptionHandlerTest`) with a probe controller taking a validated JSON body:
  - missing field → `errors[field]`
  - unknown member → `errors[role]`
  - wrong type → `errors[field]`
  - malformed JSON → detail "malformed", `errors: []`
  - two invalid fields → two entries
  - probe-thrown `InvalidFieldException` / `WeakPasswordException` / `DuplicateUserException` / `InvalidCredentialsException` → 400/400/409/401 with the expected entries and detail
  - Verify: red.
- [x] 6.2 Implement the `GlobalExceptionHandler` overrides and domain handlers (D7). Verify: 6.1 green; existing error tests still green.

## 7. Auth API and access control

- [x] 7.1 Write `AuthControllerTest` (`@WebSliceTest`, mocked use cases), covering every `user-accounts` and `authentication` scenario that can be checked at the web layer:
  - 201 body without password
  - `role` member rejected
  - field errors
  - 409 username/email
  - 400 weak password
  - login 200 and 401 identical details
  - `/me` with a real token → 200
  - `/me` without a token → 401 problem + `WWW-Authenticate`
  - Verify: red.
- [x] 7.2 Write `AccessControlTest` (`@WebSliceTest`, real tokens from the test secret):
  - anonymous `GET /api/v1/pokemon` is not rejected (404); anonymous unknown route → 404
  - anonymous `/api/v1/local-pokemon` → 401 problem + `WWW-Authenticate: Bearer`
  - USER `DELETE` → 403 problem; ADMIN `DELETE` → not rejected (404)
  - expired or tampered token → 401 with `error="invalid_token"`
  - no `Set-Cookie`; no CSRF needed
  - Verify: red.
- [x] 7.3 Implement `AuthController`, the DTOs, `AuthWebMapper`, the problem-detail entry point and access-denied handler, `SecurityConfiguration` (D8), `UserUseCaseConfiguration`, and the OpenAPI bearer scheme. Add the security beans to `@WebSliceTest`. Verify: 7.1 and 7.2 green; all earlier web-slice and integration tests still green.

## 8. End-to-end and documentation

- [x] 8.1 Write `AuthFlowIT` (`@IntegrationTest`):
  - register → login → `/me` → protected route with token (not 401/403)
  - duplicate register → 409
  - `JWT_TTL`-style short lifetime → token rejected after expiry, using a context with ttl `PT1S`
  - Verify: red before 7.3 is complete, green after.
- [x] 8.2 Write ADR-0002 (stateless JWT with HS256; RS256/JWKS as the next step). Update the README (endpoint table with auth, auth section with curl examples, `JWT_SECRET` in the configuration table, roadmap). Update `docs/problems.md` (401 and 409 wording for login and registration). Verify: lychee offline shows no broken links.

## 9. Integration verification

- [x] 9.1 `./scripts/verify-in-docker.sh` from a clean `target/`. Verify: exit 0; record test counts, coverage of domain + application (≥ 85 %), ArchUnit green, 0 warnings, Spotless clean.
- [x] 9.2 From a fresh clone: `cp .env.example .env && docker compose up --build -d`, then curl register → login → `/me` → anonymous `/api/v1/local-pokemon` (401) → USER `DELETE` (403). Verify: the expected status codes and problem bodies; `docker compose down -v`.
- [x] 9.3 Run `openspec validate add-user-auth --strict` and gitleaks over the full history. Verify: valid, no leaks (only the `.env.example` placeholder secret, which is allow-listed if flagged).

## Workflow follow-up

- Push the branch and open a PR linking `openspec/changes/add-user-auth/`. Wait for CI to go green.
- Archive the change as the PR's last commit, wait for CI to be green again, then squash-merge.
- Send the status report.
