# ADR-0004: Testcontainers PostgreSQL instead of an in-memory database

- **Status:** Accepted
- **Date:** 2026-10-14

## Context

Persistence, migration and integration tests need a database. The usual shortcut is H2 in PostgreSQL compatibility mode, but it differs from PostgreSQL in ways that matter here:
- SQL dialect details
- `jsonb` and array types, which we'll use for `internalTags[]`
- UUID generation
- locking, which matters for optimistic locking and `409` on stale updates
- Flyway migration syntax

A test that passes on H2 can fail in production. Flyway owns the schema (`ddl-auto=validate`), so the migrations themselves have to be tested against the real engine.

## Decision

- **Real database:** every database-backed test runs against PostgreSQL 16 (`postgres:16-alpine`, the same image as `docker-compose.yml`), started by **Testcontainers 2.x**.
- **One shared setup:** a single `PostgresTestcontainersConfiguration` declares the container as a `@ServiceConnection` bean, so Spring Boot wires the datasource with no URL juggling.
- **Reuse:** Spring's test-context cache reuses that container across test classes. Tests that must break the database, such as `HealthDatabaseDownIT`, get a dedicated container.
- **H2 is banned:** H2, HSQLDB and Derby are banned dependencies (Maven Enforcer), so the rule doesn't depend on code review.
- **Test layout:** integration tests are named `*IT` and run in Maven Failsafe's `integration-test` phase. Unit and slice tests stay in Surefire and need no Docker.

## Consequences

- Positive: migrations, constraints and queries are tested on the engine they run on in production.
- Positive: the same image is used in tests, Compose and CI, so there is one less source of "works on my machine".
- Negative: tests need Docker, and the first run pulls the image.
  - **Mitigation:** `scripts/verify-in-docker.sh` mounts the Docker socket and uses `--network host`, so Docker is the only host requirement. GitHub Actions runners have Docker.
- Negative: slower than in-memory, with a few seconds of container start per test context.
  - **Mitigation:** the container is shared through the context cache. Unit and slice tests (the majority) don't start a container at all.
