# Tasks: bootstrap-project

TDD convention: in each group, the `test:` commit lands red first. It compiles and fails on assertions, and the commit body summarizes the failure. The `feat:` commit then turns it green, and an optional `refactor:` commit follows.

## 1. Project scaffolding

- [x] 1.1 Generate the Maven Wrapper (Maven 3.9.x) and `pom.xml`:
  - Spring Boot 4.1.x parent, Java 21
  - starters: webmvc, validation, data-jpa, actuator, flyway
  - runtime: `flyway-database-postgresql`, `postgresql`, springdoc-openapi 3.x
  - test: spring-boot-starter-test, ArchUnit, Testcontainers PostgreSQL
  - Verify: `./mvnw -v` inside `maven:3.9-eclipse-temurin-21` shows Maven 3.9 and Java 21.
- [x] 1.2 Create `PokedexApplication` and the package skeleton (`domain`, `application.port.in`, `application.port.out`, `application.usecase`, `adapter.in.web`, `adapter.out.pokeapi`, `adapter.out.persistence`, `infrastructure`), each with a `package-info.java` that describes the layer's responsibility. Verify: `./mvnw -B compile` succeeds.
- [x] 1.3 Add `scripts/verify-in-docker.sh` (design D13). Verify: it runs `./mvnw verify` green on the skeleton, and `find . -user root` in the working tree returns nothing.
- [x] 1.4 Write ADR-0005 (OpenSpec) and ADR-0006 (Spring Boot 4.1 over 3.5) in `docs/adr/`. Verify: both follow the ADR template (Context, Decision, Consequences) and are linked from `docs/adr/README.md`.

## 2. Architecture governance (build-governance: layer and coding rules)

- [x] 2.1 Write `ArchitectureRulesSelfTest` and the fixture packages under `src/test/java/.../archfixtures/`, one deliberately broken fixture per rule:
  - domain → Spring
  - domain → JPA
  - domain → Jackson
  - application → adapter
  - application → Spring
  - adapter.in → adapter.out
  - adapter → infrastructure
  - field injection
  - `System.out`
  - `java.util.logging`
  - Verify: red, because the `ArchitectureRules` stub returns rules that match nothing.
- [x] 2.2 Implement `ArchitectureRules` (design D4) and `ArchitectureTest` over production classes. Verify: the self-test is green (every fixture violation reported) and `ArchitectureTest` is green.
- [x] 2.3 Add the Maven Enforcer rules: Java ≥ 21, Maven ≥ 3.9, banned `com.h2database:h2` and `junit:junit`. Verify: temporarily adding H2 makes `./mvnw validate` fail and name the dependency (revert afterwards, not committed).
- [x] 2.4 Write ADR-0001 (Clean/Hexagonal Architecture, single module + ArchUnit trade-off). Verify: linked from the ADR index.

## 3. Request correlation (request-correlation)

- [x] 3.1 Write `CorrelationIdFilterTest` (unit, `MockHttpServletRequest`/`Response`) covering:
  - valid id echoed
  - missing id → UUID
  - 65-char id → UUID
  - CR/LF and `<script>` ids → UUID
  - MDC holds the id during the chain and is cleared afterwards
  - header set even when the chain throws
  - Verify: red against a pass-through stub.
- [x] 3.2 Implement `CorrelationIdFilter` (design D6) and register it at highest precedence. Verify: 3.1 is green.
- [x] 3.3 Write `CorrelationIdLoggingTest` (`@WebMvcTest` + `OutputCaptureExtension`):
  - a log line written during the request contains the id
  - a rejected id never appears in the output
  - the next request on the same thread doesn't contain the previous id
  - Verify: red until 3.4.
- [x] 3.4 Add the correlation id to the log pattern in `application.yml`. Verify: 3.3 is green.

## 4. Error contract (api-errors)

- [x] 4.1 Write `GlobalExceptionHandlerTest` (`@WebMvcTest` with a test-only controller):
  - unknown route → 404 problem+json with all members and `errors: []`
  - `DELETE` on a GET-only route → 405 with `Allow`
  - unexpected `RuntimeException` → 500, generic detail, no class name, message or trace in the body
  - ERROR log with stack trace and correlation id (`OutputCaptureExtension`)
  - correlation header present on errors
  - Verify: red.
- [x] 4.2 Implement `ProblemDetailFactory`, `GlobalExceptionHandler` and the `app.problems.base-uri` property, and set `spring.web.resources.add-mappings=false` (design D5). Verify: 4.1 is green.
- [x] 4.3 Write `ProblemDetailErrorControllerTest`: a request forwarded to `/error` with a servlet error status (simulating a container-level rejection) returns problem+json with that status and no trace. Verify: red, then implement `ProblemDetailErrorController` and set `server.error.include-*=never`. Verify: green.
- [x] 4.4 Write `docs/problems.md` with one anchored section per problem type in use (`not-found`, `method-not-allowed`, `bad-request`, `internal-error`). Verify: every `type` slug the factory emits has a matching anchor, checked by `ProblemTypesDocumentedTest`, which parses the markdown.

## 5. Cross-origin access (cross-origin-access)

- [x] 5.1 Write `CorsPropertiesTest`:
  - comma-separated origins bind into a list
  - `*` → binding failure with a message requiring explicit origins
  - blank entry → failure
  - Verify: red.
- [x] 5.2 Write `CorsIntegrationTest` (`@WebMvcTest` + the CORS configuration):
  - allowed-origin preflight → 200 with `Access-Control-Allow-Origin`
  - disallowed origin → 403 without the header
  - PATCH preflight with `Authorization`, `If-Match` → advertised
  - simple GET exposes `Location`, `ETag` and `X-Correlation-Id`
  - Verify: red.
- [x] 5.3 Implement `CorsProperties` (validated record), the shared `CorsConfigurationSource` and the `ProblemDetailCorsFilter`, which writes its 403 rejections through `ProblemDetailWriter` (design D7). Verify: 5.1 and 5.2 are green.

## 6. Configuration, schema and operational endpoints (service-configuration, operational-endpoints)

- [x] 6.1 Write `PostgresTestcontainersConfiguration` (`@ServiceConnection`, `postgres:16-alpine`) and `OperationalEndpointsIT` (`@SpringBootTest(webEnvironment = RANDOM_PORT)`):
  - health 200 `{"status":"UP"}` with no details
  - liveness and readiness 200
  - info contains `pokedex-api` and its version
  - `/actuator/env` and `/actuator/beans` → 404 problem+json
  - `POST /actuator/shutdown` → 404
  - `/v3/api-docs` has `openapi` 3.x, title `Pokédex API` and the version
  - `/swagger-ui.html` reaches 200
  - Verify: red.
- [x] 6.2 Write `SchemaMigrationIT`:
  - `flyway_schema_history` contains V1, successful
  - a second context start against the same database runs no migration
  - the effective `spring.jpa.hibernate.ddl-auto` is `validate`
  - Verify: red.
- [x] 6.3 Write `HealthDatabaseDownIT`: with a dedicated container, stopping the container makes health return 503 `DOWN`. Verify: red.
- [x] 6.4 Write `ProfileConfigurationTest`:
  - default profile is `local`
  - `docker` profile without `DB_PASSWORD` fails, naming `DB_PASSWORD`
  - `ddl-auto=validate` resolved for `local`, `docker` and `test`
  - Verify: red.
- [x] 6.5 Implement:
  - `application.yml` and the `local`, `docker` and `test` profile files (design D8)
  - `V1__baseline.sql` (design D9)
  - actuator exposure and probes
  - `build-info` goal
  - the springdoc `OpenAPI` bean in `infrastructure` (title, description, version, MIT license)
  - Verify: 6.1–6.4 are green.
- [x] 6.6 Write `.env.example` listing every variable with a placeholder: `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_PORT`, `APP_CORS_ALLOWED_ORIGINS` and `SERVER_PORT`, plus `DB_URL` as a documented, commented-out optional override. Verify: `git check-ignore .env` is true and `git check-ignore .env.example` is false.
- [x] 6.7 Write ADR-0004 (Testcontainers over H2). Verify: linked from the ADR index.

## 7. Quality gates (build-governance: coverage, warnings, formatting)

- [x] 7.1 Configure the compiler with `-Xlint:all,-processing -Werror`. Verify: a temporary raw-type usage fails the build (reverted, not committed).
- [x] 7.2 Configure Spotless (google-java-format) with `check` bound to `verify`, and run `spotless:apply` once. Verify: a temporarily mis-indented file fails `./mvnw verify` and names the file, and `./mvnw spotless:apply` fixes it.
- [x] 7.3 Configure JaCoCo: agent for unit and integration tests, merged HTML report, and a `check` on `domain` + `application` with LINE ≥ 0.85 (design D11). Verify: `target/site/jacoco/index.html` exists after `verify`, and a temporary untested class in `domain` fails the check with the measured ratio (reverted, not committed).

## 8. Container delivery (container-delivery)

- [x] 8.1 Write the multi-stage `Dockerfile` (design D12) and `.dockerignore` (excludes `target/`, `.git/`, `.env`, IDE files). Verify:
  - `docker build -t pokedex-api:dev .` succeeds
  - `docker image inspect --format '{{.Config.User}}' pokedex-api:dev` prints `app`
  - the image has no `javac`
- [x] 8.2 Write `docker-compose.yml` (design D12). Verify, from a fresh clone in a temp dir:
  - `cp .env.example .env && docker compose up --build -d` brings both services to `healthy`
  - `curl -s localhost:8080/actuator/health` returns `{"status":"UP"}`
  - after `docker compose down` and `up -d`, `flyway_schema_history` still holds V1
  - `docker compose down -v` cleans up
- [x] 8.3 Add the Quick Start, the verify-in-docker usage (including the macOS/Windows note) and seeded-data placeholders to the README. Verify: every command in the Quick Start runs as written from a clean clone.

## 9. Continuous integration (build-governance: CI)

- [x] 9.1 Write `.github/workflows/ci.yml` with jobs `build` (verify + JaCoCo artifact + coverage in the job summary), `openspec`, `docker` and `secrets` (design D14). Verify: the workflow passes `actionlint` (run via the `rhysd/actionlint` container).
- [x] 9.2 Add the CI status badge and the coverage-artifact note to the README. Verify: the badge URL points at `aleferod/pokedex-api` `ci.yml` on `main`.

## 10. README skeleton

- [x] 10.1 Write the README sections that are known so far:
  - overview and user-story map (endpoints marked "planned")
  - Mermaid architecture diagram and layer/package explanation
  - patterns table
  - tech stack and rationale
  - quick start, Swagger URL, error-format example
  - testing strategy and commands
  - OpenSpec workflow with a link to `openspec/specs`
  - ADR links
  - Verify: every link in the README resolves (`lychee` container in offline mode for relative links).

## 11. Integration verification

- [x] 11.1 Run `./scripts/verify-in-docker.sh` from a clean `target/`. Verify: exit 0, and the summary records test counts per suite, ArchUnit green, coverage check passed, zero compiler warnings and Spotless clean.
- [x] 11.2 Run `openspec validate bootstrap-project --strict`. Verify: no errors or warnings.
- [x] 11.3 Run the gitleaks container over the full history plus the `ghp_|github_pat_|gho_` grep. Verify: no findings.

## Workflow follow-up

- Push the branch and open the PR with `gh pr create`. The body links `openspec/changes/bootstrap-project/` and lists the TDD commit pairs.
- Wait for every CI job to pass on the PR.
- Archive the change as the PR's last commit (`openspec archive bootstrap-project --yes`), so `openspec/specs/` gains the seven capabilities. Re-check CI.
- Squash-merge, delete the branch, and send the status report: what was done, test and coverage numbers, PR link, decisions taken.
