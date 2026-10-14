# Spec Delta: service-configuration

## Purpose

Defines how the service gets its configuration and manages its database schema, so the same build runs safely on a developer machine, in Docker and in tests without any secret in the repository.

## ADDED Requirements

### Requirement: Configuration from the environment
The service SHALL read deployment-specific settings from environment variables:
- database URL, user and password
- allowed CORS origins
- HTTP port

The repository SHALL contain no secret values, only an example environment file with placeholders.

#### Scenario: Database settings supplied
- **WHEN** the service starts with `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` set
- **THEN** it connects to that database

#### Scenario: Required setting missing
- **WHEN** the service starts with the `docker` profile and `DB_PASSWORD` is not set
- **THEN** startup fails, and the error message names the missing setting

#### Scenario: Example environment file
- **WHEN** a developer runs `cp .env.example .env` on a fresh clone
- **THEN** `.env` lists every variable the service and the Compose stack need, each with a safe placeholder value
- **AND** `.env` itself is ignored by version control

### Requirement: Runtime profiles
The service SHALL support three runtime profiles: `local` (default; runs on the host and reads the `.env` file), `docker` (runs in the Compose stack) and `test` (automated tests with disposable databases).

#### Scenario: Default profile
- **WHEN** the service starts with no active profile
- **THEN** it runs with the `local` profile and picks up variables from a `.env` file in the working directory if one exists

#### Scenario: Docker profile
- **WHEN** the service starts with `SPRING_PROFILES_ACTIVE=docker` inside the Compose stack
- **THEN** it connects to the Compose database service and listens on port 8080

### Requirement: Schema owned by versioned migrations
On startup, the service SHALL apply any pending versioned database migrations before accepting traffic, and SHALL record each applied migration in the database.

#### Scenario: Empty database
- **WHEN** the service starts against an empty database
- **THEN** all migrations are applied in version order and recorded as successful before the health endpoint reports `UP`

#### Scenario: Already-migrated database
- **WHEN** the service restarts against a database that already holds every migration
- **THEN** no migration runs again and startup succeeds

### Requirement: No automatic schema generation
The service SHALL NOT create or alter database tables from its persistence model. It SHALL instead check at startup that the schema matches the model, and refuse to start on a mismatch.

#### Scenario: Schema validation mode in every profile
- **WHEN** the service runs under any of the `local`, `docker` or `test` profiles
- **THEN** its persistence layer is set to validate the schema and never to create, update or drop it
