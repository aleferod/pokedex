# Design: add-user-auth

## Context

`bootstrap-project` gave us the hexagonal skeleton (still empty in `domain` and `application`), an RFC 7807 error contract with `ProblemDetailFactory`/`ProblemDetailWriter`, and a CORS filter that runs before everything else. Its build gates include ArchUnit (framework-free `domain`/`application`, isolated adapters) and an 85 % line-coverage gate that this change is the first to make bite. The requirements are in `specs/`; see proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- A small, honest auth slice that shows the layering end to end: rules live in the domain, orchestration in use cases, and Spring Security only at the edges.
- Every rejection (400/401/403/409) uses the existing problem-detail contract.
- Deny-by-design for the protected areas, and keep 404 for unknown routes.

**Non-Goals:**
- Refresh tokens, revocation, rate limiting and lockout (see Risks).
- An identity provider or OAuth2 authorization server.

## Decisions

### D1. Stateless JWT, issued and verified by this service (ADR-0002)
- **Choice:**
  - Login issues a JWT signed with HMAC-SHA256 using a secret from `JWT_SECRET` (at least 32 bytes).
  - Spring Security's OAuth2 resource server verifies it on every request: signature, `exp`/`nbf` with a configurable clock skew (`app.security.jwt.clock-skew`, default 30 s; 0 in `ShortLivedTokenIT`), and `iss` = `pokedex-api`.
  - Claims: `sub` (user id), `username`, `role`, `iss`, `iat`, `exp`.
- **Rejected alternatives:**
  - **Server sessions with a cookie.** They need CSRF protection and sticky or shared session storage, and they're awkward for a separately hosted SPA.
  - **RS256 with a JWKS endpoint.** It's the better choice once several services verify tokens, but overkill for one service. It's the next step if that happens.
  - **Opaque tokens with an introspection table.** They cost a database hit per request.

### D2. Domain model (`domain.user`): Value Objects + Factory + Chain of Responsibility
- **Value Objects** (records that validate in their compact constructor and throw `InvalidFieldException(field, message)`):
  - `UserId(UUID)`
  - `Username`: trims, lower-cases, enforces `[a-z0-9._-]{3,30}`
  - `EmailAddress`: trims, lower-cases, `local@domain.tld`, ≤ 254 characters
  - `PasswordHash`: opaque and non-blank. Its `toString()` is redacted so it can never leak into logs.
  - `Role` enum: `USER`, `ADMIN`
- **Entity:** `User` is a record with identity. **Factory:** `User.register(id, username, email, hash, now)` always assigns `Role.USER` and sets both timestamps to `now`. `User.restore(...)` rebuilds a stored user.
- **Chain of Responsibility for passwords:** `PasswordPolicy` (package `domain.user.password`, which depends on nothing else in `domain.user`, so there is no package cycle) runs an ordered chain of `PasswordRule`s:
  - `LengthRule(8, 72)`. 72 is BCrypt's input limit.
  - `UpperCaseRule`, `LowerCaseRule`, `DigitRule`, `SpecialCharacterRule`
  - Each rule returns an optional violation. The policy collects every violation rather than stopping at the first, so the client sees all problems at once. A non-empty result becomes `WeakPasswordException(List<String>)`.
  - Adding a rule means adding a class; nothing else changes (open/closed).
- **Domain exceptions:** an abstract `DomainException` with subclasses:
  - `InvalidFieldException(field, message)`
  - `WeakPasswordException(violations)`
  - `DuplicateUserException(field)`
  - `InvalidCredentialsException`
  - `UserNotFoundException`
- No HTTP knowledge in the domain. The web adapter maps exceptions to status codes.

### D3. Use cases and ports
| port.in (interface) | usecase (class) | port.out used |
|---|---|---|
| `RegisterUser` | `RegisterUserUseCase` | `UserRepositoryPort`, `PasswordHasherPort`, `Clock` |
| `AuthenticateUser` | `AuthenticateUserUseCase` | `UserRepositoryPort`, `PasswordHasherPort`, `TokenPort` |
| `GetCurrentUser` | `GetCurrentUserUseCase` | `UserRepositoryPort` |

- **Register flow:**
  1. Build the value objects.
  2. Run the password policy.
  3. `existsByUsername` → `DuplicateUserException("username")`, then `existsByEmail` → `DuplicateUserException("email")`.
  4. Hash, build with `User.register`, and save.
- **Concurrent duplicates:** the persistence adapter also turns a unique-constraint violation into `DuplicateUserException`.
- **Authenticate flow:**
  - Look up the user by normalized username.
  - For an unknown user, still run `matches()` against a dummy hash computed once at construction, so response time doesn't reveal whether the user exists. Then throw `InvalidCredentialsException`.
  - On a wrong password, throw `InvalidCredentialsException`.
  - Otherwise `TokenPort.issue(user)` returns `IssuedToken(value, expiresAt)`.
- **Wiring:** `java.time.Clock` (a JDK type) keeps tests deterministic. `infrastructure.usecase.UserUseCaseConfiguration` wires the plain classes as beans, since `application` has no Spring.

### D4. Persistence (`adapter.out.persistence.user`)
- **Migration `V2__create_users.sql`** creates the `users` table:
  - `id uuid` primary key
  - `username varchar(30)`, `email varchar(254)`, `password_hash varchar(100)`, all not null
  - `role varchar(10)` with a check `('USER','ADMIN')`
  - `created_at` and `updated_at` as `timestamptz`
  - unique constraints `uk_users_username` and `uk_users_email`
  - Values are already lower-cased by the domain, so plain unique constraints give case-insensitive uniqueness.
- **Mapping:** `UserJpaEntity` (field access, protected no-arg constructor) and `SpringDataUserRepository`. `UserPersistenceMapper` is a MapStruct mapper (`componentModel = spring`, `unmappedTargetPolicy = ERROR`), with default methods converting the value objects.
- **`UserPersistenceAdapter implements UserRepositoryPort`** (Repository pattern). It catches `DataIntegrityViolationException` and maps it to `DuplicateUserException` by constraint name.

### D5. Security adapters (`adapter.out.security`)
- **`BCryptPasswordHasher implements PasswordHasherPort`:** wraps Spring Security's `BCryptPasswordEncoder`, with strength from `app.security.password.bcrypt-strength` (default 10; 4 in tests for speed).
- **`JwtTokenIssuer implements TokenPort`:** uses a `JwtEncoder` bean (`NimbusJwtEncoder` with the HMAC secret), the clock, and `JwtProperties` values passed in as plain constructor arguments (ttl, issuer), so the adapter doesn't depend on `infrastructure`.

### D6. Web (`adapter.in.web.auth`)
- **`AuthController`:**
  - `POST /register` returns 201. There's no `Location` header, because user accounts have no public resource URL. The body is a `UserResponse`.
  - `POST /login` returns 200 with a `TokenResponse`.
  - `GET /me` reads `sub` from `@AuthenticationPrincipal Jwt`.
- **DTOs:** request records with Bean Validation that mirror the domain rules, for fast field-level feedback. The domain stays the authority, so a mismatch still ends in a 400 from `InvalidFieldException`. `AuthWebMapper` maps to and from the DTOs.
- **OpenAPI:** a `bearer-jwt` security scheme is declared in `OpenApiConfiguration`. `/me` and, later, the local-Pokémon operations reference it.

### D7. Error handling additions (`GlobalExceptionHandler`)
- **Validation errors:** override `handleMethodArgumentNotValid` to put one `ProblemFieldError` per field error into `errors[]`.
- **Unreadable bodies:** override `handleHttpMessageNotReadable` and classify the Jackson 3 cause:
  - `UnrecognizedPropertyException` → field + "is not an accepted field"
  - `MismatchedInputException` with a path → field + "has the wrong type"
  - otherwise "Malformed JSON request body" with an empty `errors[]`
- **Jackson setting:** set `spring.jackson.deserialization.fail-on-unknown-properties: true`.
- **Domain exception mapping** (each handler builds the problem through `ProblemDetailFactory`):

| Domain exception | HTTP response |
|---|---|
| `InvalidFieldException` | 400 with one entry |
| `WeakPasswordException` | 400 with one `password` entry per violation |
| `DuplicateUserException` | 409 `conflict` with an entry for the field |
| `InvalidCredentialsException` | 401 `unauthorized` with fixed detail "Invalid username or password." |
| `UserNotFoundException` on `/me` | 401 |

### D8. Security configuration (`infrastructure.security`)
- **`SecurityConfiguration`:** one `SecurityFilterChain`, configured as follows.
  - **Stateless:** `STATELESS` sessions, no `httpBasic`/`formLogin`/`logout`/`requestCache`.
  - **CSRF disabled:** there's no cookie authentication, so CSRF doesn't apply.
  - **Authorization rules**, kept as one explicit list:
    - `DELETE /api/v1/local-pokemon/**` → `hasRole('ADMIN')`
    - `/api/v1/local-pokemon/**` and `/api/v1/auth/me` → authenticated
    - everything else → `permitAll()`
  - **Why everything else is permitted:** unknown routes still reach Spring MVC and return 404 (the `api-errors` scenarios), and the public catalog needs no list. The risk is a future protected area outside these prefixes; `AccessControlTest` pins every rule, and new protected routes must be added to the list.
  - **Rejected:** `anyRequest().authenticated()`. It would turn every unknown or not-yet-built route into 401 and change the archived 404 behaviour.
- **401/403 bodies:** `ProblemDetailAuthenticationEntryPoint` delegates to `BearerTokenAuthenticationEntryPoint` for the RFC 6750 `WWW-Authenticate` header, then writes an `unauthorized` problem through `ProblemDetailWriter`. `ProblemDetailAccessDeniedHandler` does the same with `BearerTokenAccessDeniedHandler` and `forbidden`.
- **`JwtConfiguration`:**
  - `JwtProperties` record (`secret`, `ttl`, `issuer`) validates the secret is at least 32 bytes in its compact constructor.
  - `JwtEncoder` and `JwtDecoder` beans share one `SecretKey`. The decoder validators are the default timestamp check plus the issuer.
  - `JwtAuthenticationConverter` maps the `role` claim to `ROLE_<role>`, and the principal name is the `username` claim.
- **Required settings:** `RequiredSettingsEnvironmentPostProcessor` now also requires `app.security.jwt.secret`, which yields the "names `JWT_SECRET`" message.
- **Filter order is unchanged:** correlation id, then CORS, then Spring Security. CORS preflights are answered before authentication, as recorded in bootstrap-project D7.

### D9. Configuration
```yaml
app.security.jwt.secret: ${JWT_SECRET}            # required outside tests
app.security.jwt.ttl: ${JWT_TTL:PT1H}
app.security.jwt.issuer: pokedex-api
app.security.password.bcrypt-strength: ${BCRYPT_STRENGTH:10}
```
- The `test` profile sets a fixed 48-byte test secret and strength 4.
- `.env.example` gets a placeholder plus the hint `openssl rand -base64 48`.
- Compose passes `JWT_SECRET` (required) and `JWT_TTL` through.

### D10. Testing
| Level | Tests |
|---|---|
| Domain unit | `UsernameTest`, `EmailAddressTest`, `PasswordPolicyTest`, `UserTest` |
| Use case unit (Mockito) | `RegisterUserUseCaseTest`, `AuthenticateUserUseCaseTest`, `GetCurrentUserUseCaseTest` |
| Persistence (`@DataJpaTest` + Testcontainers) | `UserPersistenceAdapterIT`: round trip, unique constraints, concurrent-duplicate translation |
| Adapter unit | `BCryptPasswordHasherTest`, `JwtTokenIssuerTest` (claims, expiry with a fixed clock, decodable by the decoder) |
| Web slice | `AuthControllerTest` (validation, 201/200/401/409, field errors, malformed JSON, unknown member), `AccessControlTest` (public/401/403 per rule, no `Set-Cookie`, no CSRF requirement), `JwtPropertiesTest`, extended `ProfileConfigurationTest` |
| Integration | `AuthFlowIT`: register → login → me → protected route with token; expired and tampered tokens |

- **Web slice setup:** `@WebSliceTest` additionally imports the security configuration and the JWT beans. Tests build real tokens with the test secret, so they exercise the production decoder rather than mocked principals.

### Patterns introduced in this change
| Pattern | Where |
|---|---|
| Value Object | `Username`, `EmailAddress`, `UserId`, `PasswordHash` |
| Factory | `User.register` / `User.restore` |
| Chain of Responsibility | `PasswordPolicy` → `PasswordRule` chain |
| Repository | `UserRepositoryPort` ← `UserPersistenceAdapter` |
| Adapter | `BCryptPasswordHasher`, `JwtTokenIssuer` behind ports |
| DTO + Mapper | `AuthWebMapper`, `UserPersistenceMapper` (MapStruct) |

## Risks / Trade-offs

- [The HMAC secret is shared between issuing and verifying, and rotating it needs a restart that invalidates all tokens] → acceptable for one service; RS256 + JWKS is the documented next step (ADR-0002).
- [No refresh token, so users log in again after the TTL (1 h)] → short TTL limits the damage from a leaked token. Refresh or revocation is listed as future work.
- [No rate limiting or lockout, so brute force is only slowed by BCrypt] → future work (for example Bucket4j). The dummy-hash comparison already prevents user enumeration through timing.
- [The explicit protected-route list relies on discipline] → `AccessControlTest` pins it; protected prefixes are named constants; reviewers check the list in each new change.
- [The BCrypt cost in tests slows the suite] → strength 4 in the `test` profile.
- [MapStruct and `-Xlint:all -Werror`] → `-processing` is already excluded. The generated sources compile warning-free, and the build fails if they don't.

## Migration Plan

Additive: Flyway applies `V2__create_users.sql` on startup. Existing data is unaffected (there are no tables yet). Rollback: revert the commit and drop the `users` table. In development, `docker compose down -v` does both.
