# Proposal: bootstrap-project

## Why

Every user story (US01–US04) and the auth API need the same foundation:
- a runnable service,
- one consistent error contract,
- request tracing,
- container-based delivery,
- a build that enforces the Clean Architecture rules and the quality bar the interview panel will look at.

Building this first, before any domain code, means each later change inherits the guardrails instead of adding them afterwards.

## What Changes

- New Java 21 / Spring Boot service skeleton (Maven Wrapper) with the hexagonal package layout `domain`, `application`, `adapter`, `infrastructure`, and no business features yet.
- One error contract for the whole API: RFC 7807 problem details with a fixed member set and no leaked internals.
- Correlation id on every request and response, carried into the logs.
- Public operational endpoints: health, build info, OpenAPI document and Swagger UI. Every other management endpoint stays hidden.
- Cross-origin access limited to configured frontend origins.
- Configuration comes only from the environment, with `local`, `docker` and `test` profiles.
- Database schema owned by versioned migrations. The service never auto-generates DDL.
- Build gates:
  - architecture dependency rules
  - ≥ 85 % line coverage on `domain` and `application`
  - zero compiler warnings
  - enforced formatting
  - banned dependencies (no H2, no JUnit 4)
- Container delivery:
  - multi-stage, non-root image with a health check
  - Compose stack with PostgreSQL 16
  - a script that runs the full build and tests with only Docker on the host
- CI pipeline:
  - build and full test suite, including container-backed tests
  - coverage report artifact
  - spec validation
  - image build
  - secret scan
- Initial README, `docs/problems.md`, and ADRs 0001 (Clean Architecture), 0004 (Testcontainers), 0005 (OpenSpec) and 0006 (Spring Boot 4).

## Capabilities

### New Capabilities
- `api-errors`: the error response contract that every endpoint, present and future, follows.
- `request-correlation`: how each request gets a correlation id, and how that id is returned to callers and written to logs.
- `operational-endpoints`: health, build info and API documentation endpoints, and which management endpoints stay hidden.
- `cross-origin-access`: which browser origins may call the API and with which methods and headers.
- `service-configuration`: environment-based configuration, runtime profiles, and migration-owned database schema.
- `build-governance`: the build gates that keep architecture, coverage, warnings, formatting and dependencies in line, and the CI pipeline that runs them.
- `container-delivery`: the container image, the Compose stack, and building and verifying with only Docker installed.

### Modified Capabilities
- None. This is the first change.

## Out of Scope

- Any Pokémon feature (US01–US04), the auth API, users table, security filter chain. These come in later changes.
- Caching, resilience and the PokeAPI client (`add-caching-resilience`, `add-pokemon-catalog`).
- The full-stack Compose file with the frontend. It needs `pokedex-web` and arrives with that repo.
- Seed data (`add-seed-data`).

## Impact

- **Code:** new repository content. There's no existing behavior to break.
- **APIs:**
  - `GET /actuator/health`
  - `GET /actuator/info`
  - `GET /v3/api-docs`
  - `GET /swagger-ui.html`
  - the error contract for all paths
- **Dependencies:**
  - Spring Boot 4.1 (Web MVC, Validation, Data JPA, Actuator), PostgreSQL driver, Flyway, springdoc-openapi
  - for tests: JUnit 5, AssertJ, Mockito, ArchUnit, Testcontainers
- **Systems:** Docker and Docker Compose for local runs, plus GitHub Actions.
- **Serves:** every user story indirectly, and the PDF's technical requirements: public repo, tests, error handling, Dockerfile, Clean Architecture.
