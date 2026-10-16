# Design: bootstrap-project

## Context

The repository is empty apart from the license, `.gitignore` and the OpenSpec configuration. The proposal explains why this change comes first, and the delta specs under `specs/` define the required behavior. This document covers how we build it.

Constraints:
- `openspec/config.yaml` (stack, layers, conventions, Definition of Done)
- the host must need only Docker
- the result must be easy to walk through in a 30-minute interview

## Goals / Non-Goals

**Goals:**
- Decide the package layout and dependency rules once, and enforce them mechanically.
- Build the cross-cutting HTTP behaviors (errors, correlation id, CORS) in a way that later changes reuse and don't re-implement.
- Get the test infrastructure ready: one shared Testcontainers setup, plus fixtures for the architecture rules.

**Non-Goals:**
- Spring Security. The `add-user-auth` change brings the security filter chain. Until then everything is public, which is safe because there are no business endpoints yet.
- Structured (JSON) logging, distributed tracing and metrics export. They're listed as future improvements.

## Decisions

### D1. Spring Boot 4.1 on Java 21 (ADR-0006)
- **Choice:** the latest Boot 4.1.x patch. The last 3.x line (3.5) lost open-source support on 2026-06-30.
- **Ecosystem check:** every library we need already supports Boot 4 (resilience4j-spring-boot4, springdoc 3.x, Testcontainers 2.x).
- **Rejected:** Boot 3.5.x. It has more examples online, but a reviewer would rightly flag an end-of-life framework in new code.
- **Fallback:** if Boot 4 blocks a later change, we stop and ask before downgrading.

### D2. One Maven module, layers enforced by ArchUnit
- **Choice:** a single module with packages
  `com.ballastlane.pokedex.{domain, application.port.in, application.port.out, application.usecase, adapter.in.web, adapter.out.pokeapi, adapter.out.persistence, infrastructure}`.
  Each package gets a `package-info.java` that documents its role.
- **Rejected:** a Maven multi-module setup (`domain`, `application`, `adapters`, `boot`). It enforces layers at compile time, but it triples the build files and makes a small exercise harder to read and review.
- **Mitigation:** ArchUnit gives the same guarantee at test time, with better error messages. The README records this trade-off.

### D3. Framework-free `application` layer
- **Choice:** `application` depends only on `domain` and the JDK. That's stricter than "no adapters".
  - Use cases are plain classes, wired as beans in `infrastructure` (`UseCaseConfiguration`).
  - Transactions, caching and resilience are added around ports in adapters or infrastructure (Decorator/Proxy).
- **Why:** use cases can be unit-tested with no Spring context at all, and the dependency rule becomes visible in the imports.
- **Cost:** a few extra wiring lines in `infrastructure`.

### D4. Architecture rules as reusable, self-tested definitions
- **Pattern:** Specification.
- **Choice:** `ArchitectureRules` (test sources) builds the rules for a given base package:
  - a layered architecture with `withOptionalLayers(true)`, so empty layers are allowed now
  - domain and application allow-lists
  - inbound/outbound adapter isolation
  - `GeneralCodingRules`: no field injection, no standard streams, no `java.util.logging`
- `ArchitectureTest` applies the rules to production classes.
- `ArchitectureRulesSelfTest` applies them to fixture packages under `src/test/java/.../archfixtures/` that deliberately break each rule, and asserts that each rule fails. This proves no rule passes vacuously, which matters while most layers are still empty.

### D5. Error contract: Template Method plus Factory
- **Template Method:** `adapter.in.web.error.GlobalExceptionHandler` (`@RestControllerAdvice`) extends Spring's `ResponseEntityExceptionHandler`. That class already maps every Spring MVC exception (404, 405, 415, 400…) to a `ProblemDetail`. We override its hook `handleExceptionInternal` so every problem it builds gets enriched.
- **Factory:** `ProblemDetailFactory` is the single place that sets `type`, `title`, `instance`, `timestamp` and `errors[]`.
- **Problem type URIs:** `{app.problems.base-uri}/{slug}`. The default base is the `docs/problems.md` page on GitHub, so every `type` resolves to documentation.
- **Unexpected exceptions:** a catch-all `@ExceptionHandler(Exception.class)` logs at ERROR with the stack trace and returns a generic 500. Its `detail` says "quote the correlation id".
- **Errors outside the dispatcher** (container-level rejections, filter failures): `ProblemDetailErrorController` replaces Boot's `/error` JSON and uses the same factory. `server.error.include-*` is set to `never` as defence in depth.
- **Unmatched paths:** `spring.web.resources.add-mappings=false`, so unmatched paths raise `NoHandlerFoundException` and return 404 for every HTTP method. The API serves no static files; springdoc registers its own resource handlers for Swagger UI.
- **Rejected:** Boot's built-in `spring.mvc.problemdetails.enabled`. It covers only MVC exceptions, and it gives us no hook to add `timestamp` or `errors[]`.

### D6. Correlation id: Chain of Responsibility
- `adapter.in.web.correlation.CorrelationIdFilter` (`OncePerRequestFilter`) runs at `Ordered.HIGHEST_PRECEDENCE`. It does four things:
  1. Validates the incoming header against `^[A-Za-z0-9-]{1,64}$`. If the header is missing or invalid, it generates a UUID.
  2. Writes the id into the MDC under the key `correlationId`.
  3. Sets the response header *before* calling the chain, so error responses carry it too.
  4. Removes the MDC key in `finally`.
- The log pattern `logging.pattern.level` includes `%X{correlationId:-}`.
- Copying the MDC onto async threads arrives with `add-pokemon-catalog`, through a `TaskDecorator` on the bounded executor that fetches pages in parallel.
- **Rejected:** Micrometer Tracing just for ids. It adds trace and span semantics we don't need yet, plus extra dependencies.

### D7. CORS through one shared `CorsConfigurationSource`
- **Binding:** `infrastructure.web.CorsProperties` is a validated `@ConfigurationProperties` record, bound from `APP_CORS_ALLOWED_ORIGINS`. Its compact constructor rejects `*` and blank entries, so a misconfiguration fails at startup.
- **Source:** `infrastructure.web.CrossOriginConfiguration` builds the one `UrlBasedCorsConfigurationSource` bean for `/api/**`. (It's not called `CorsConfiguration`, to avoid clashing with Spring's class of that name.)
- **Filter:** `adapter.in.web.cors.ProblemDetailCorsFilter` follows the same flow as Spring's `CorsFilter`. It is registered right after the correlation filter.
  - *Refined during implementation:* Spring's `DefaultCorsProcessor` rejects a request with a plain-text body (`403 Invalid CORS request`), which breaks the `api-errors` contract. So our filter uses a processor that only sets 403, and then writes a `forbidden` problem detail through `adapter.in.web.error.ProblemDetailWriter`.
  - That writer serializes problems to the servlet response for errors raised in filters. `add-user-auth` reuses it for 401/403 from the security chain.
- **Later:** this filter runs before Spring Security's filter chain (order −100), so preflights are answered before authentication. `add-user-auth` therefore does not need `http.cors()`. If it ever enables it, `http.cors()` would pick up the same `corsConfigurationSource` bean, so the rules stay defined once.

### D8. Configuration and profiles
- **Layout:** `application.yml` holds shared defaults plus three profile files:
  - `local`: imports `optional:file:.env[.properties]`, so a developer can run `./mvnw spring-boot:run` using the same `.env` as Compose
  - `docker`: DB host `postgres`
  - `test`: datasource supplied by `@ServiceConnection`
- **Default profile:** `spring.profiles.default=local`.
- **No secret defaults:** `DB_PASSWORD` has no default anywhere.
  - *Refined during implementation:* Boot's binder ignores unresolvable placeholders. It would keep the literal `${DB_PASSWORD}` as the password, and the failure would show up later as a misleading Postgres authentication error.
  - So `infrastructure.config.RequiredSettingsEnvironmentPostProcessor` (registered in `META-INF/spring.factories`) resolves the required keys right after config data is loaded. It fails with `Could not resolve placeholder 'DB_PASSWORD'… see .env.example` before any bean exists.
  - The `test` profile is exempt because Testcontainers supplies the connection. Every Spring test runs under `test`.
- **JPA settings:**
  - `spring.jpa.hibernate.ddl-auto=validate` in every profile
  - `spring.jpa.open-in-view=false`, so lazy loading can't leak into the web layer
- **Info:** `spring-boot-maven-plugin` `build-info` feeds `/actuator/info`.
- **Actuator exposure:**
  - `management.endpoints.web.exposure.include=health,info`
  - `management.endpoint.health.show-details=never`
  - `management.endpoint.health.probes.enabled=true`

### D9. Flyway owns the schema
- `V1__baseline.sql` is a deliberate no-op. It contains only a comment explaining that tables arrive with their feature changes (`users` in `add-user-auth`).
- It still proves the migration pipeline end to end (history table, ordering, re-run idempotency).
- **Rejected:** creating speculative tables now. That would break the "one capability per change" history the interview walks through.

### D10. Test infrastructure (ADR-0004)
- **Testcontainers 2.x** with `@ServiceConnection`:
  - one `@TestConfiguration` (`PostgresTestcontainersConfiguration`) declares a `postgres:16-alpine` container bean
  - Spring's context cache shares it across integration tests
- **Naming:** unit and slice tests (`*Test`) run in Surefire. Integration tests (`*IT`) run in Failsafe during `verify`.
- **No H2:** the Maven Enforcer `bannedDependencies` rule makes "No H2" mechanical.
- **Health-DOWN scenario:** `HealthDatabaseDownIT` uses its own container and stops it partway through the test.
- **TDD discipline:**
  - every `test:` commit compiles and fails on assertions, not on missing types
  - minimal stubs, like a filter that only calls `chain.doFilter`, go into the same commit
  - the red output is summarized in the commit body

### D11. Quality gates
- **Compiler:** `-Xlint:all,-processing -Werror`. `-processing` is excluded because annotation processors such as MapStruct otherwise trigger the "no processor claimed" noise.
- **Formatting:** Spotless with google-java-format, with `check` bound to `verify`. `./mvnw spotless:apply` fixes formatting.
  - **Rejected:** Checkstyle. It would also check style rules, but formatting is the part that actually causes review churn, and an auto-fixer is easier to live with.
- **Coverage:** the JaCoCo `check` execution includes only `**/domain/**` and `**/application/**`, with LINE `COVEREDRATIO >= 0.85`. The report covers unit and integration tests (`report` plus `report-integration`, merged).
- **Enforcer:** Java ≥ 21, Maven ≥ 3.9, banned `com.h2database:h2` and `junit:junit`.

### D12. Container image and stack
- **Dockerfile stages:**
  1. `maven:3.9-eclipse-temurin-21`: `./mvnw -B -DskipTests package` with a BuildKit cache mount for `~/.m2`. Tests run in CI and the verify script, because Testcontainers can't run inside `docker build`.
  2. The same image runs `java -Djarmode=tools -jar app.jar extract --layers --launcher` to split the jar into layers.
  3. `eclipse-temurin:21-jre-alpine`:
     - adds system user `app`
     - copies the layers from least to most volatile (dependencies → spring-boot-loader → snapshot-dependencies → application)
     - sets `USER app` and `EXPOSE 8080`
     - `HEALTHCHECK` runs busybox `wget` against `/actuator/health/readiness`
     - `ENTRYPOINT` runs `JarLauncher` with `-XX:MaxRAMPercentage=75`
- **Compose:**
  - `postgres:16-alpine` with a `pg_isready` healthcheck and named volume `pokedex-db-data`
  - `api` builds `.`, sets `SPRING_PROFILES_ACTIVE=docker`, and has `depends_on: postgres: condition: service_healthy`
  - *Refined during implementation:* `api` gets an explicit `environment:` list interpolated from `.env` (`DB_URL` pointing at `postgres`, `DB_USERNAME`, `DB_PASSWORD`, `APP_CORS_ALLOWED_ORIGINS`) instead of `env_file: .env`. Unrelated `.env` entries never reach the container, and a host-oriented `DB_URL` can't override the Compose one. A missing `DB_PASSWORD` stops Compose with `run cp .env.example .env`.
  - Postgres is published on `${DB_PORT:-5433}` so the `local` profile can reach it. It's 5433 rather than 5432 so it doesn't clash with a PostgreSQL already running on the host, which was the case on the build machine.

### D13. `scripts/verify-in-docker.sh`
It runs:

```
docker run --rm --network host \
  -u "$(id -u):$(id -g)" --group-add "$(stat -c %g /var/run/docker.sock)" \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v pokedex-m2:/var/maven/.m2 -e MAVEN_CONFIG=/var/maven/.m2 -e HOME=/var/maven \
  -v "$PWD":/app -w /app maven:3.9-eclipse-temurin-21 ./mvnw -B -ntp verify
```

- `--network host`: Testcontainers inside the container can reach the ports it maps on the host daemon.
- `-u` plus `--group-add`: no root-owned files in `target/`, and the build can still use the socket.
- Named volume: caches dependencies between runs.

It exits with Maven's status. The README documents running `./mvnw verify` on the host, and `TESTCONTAINERS_HOST_OVERRIDE` for Docker Desktop on macOS and Windows.

### D14. CI (GitHub Actions)
`ci.yml` runs on push to `main` and on pull requests. It has four parallel jobs:
1. `build`: Temurin 21 with Maven cache, `./mvnw -B -ntp verify`, uploads `target/site/jacoco*`, and writes the coverage percentage to the job summary.
2. `openspec`: `npx @fission-ai/openspec@1.14.1 validate --all --strict`, with telemetry disabled.
3. `docker`: `docker build` (no push).
4. `secrets`: gitleaks over the full history.

The README badge points at this workflow.

### Patterns introduced in this change
| Pattern | Where |
|---|---|
| Ports & Adapters (skeleton) | package layout, `package-info.java` |
| Template Method | `GlobalExceptionHandler` overriding `ResponseEntityExceptionHandler` hooks |
| Factory | `ProblemDetailFactory` |
| Chain of Responsibility | servlet filter chain: `CorrelationIdFilter` → `CorsFilter` → DispatcherServlet |
| Specification | `ArchitectureRules` |
| Value Object (config) | `CorsProperties` record with an invariant-checking compact constructor |

## Risks / Trade-offs

- [Boot 4 is newer, so there's less community material] → library versions are pinned and checked against Maven Central. Any incompatibility gets an ADR note, and we ask before downgrading.
- [`spring.web.resources.add-mappings=false` might stop springdoc serving Swagger UI] → `OperationalEndpointsIT` covers the Swagger UI scenario. If it fails, register springdoc's resource handler explicitly instead of re-enabling the defaults.
- [The coverage gate measures almost no classes while `domain` and `application` are empty] → it passes vacuously in this change, by design. The self-test fixtures prove that the architecture rules (the gate that matters now) aren't vacuous. Coverage starts to bite in `add-user-auth`.
- [`--network host` is Linux-only] → the README gives the Docker Desktop alternative (`TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`) and the plain `./mvnw verify` route. CI runs natively on Linux.
- [An empty `V1__baseline.sql` looks odd] → its comment explains it, and it lets the migration requirement be tested today.
- [Squash merges hide the red/green commits on `main`] → the PR commit list keeps them, and the README and presentation link to the PRs.

## Migration Plan

Not applicable: first deployment, no existing data. Rollback means reverting the squash commit.
