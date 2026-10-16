# container-delivery Specification

## Purpose
Makes the service buildable, testable and runnable on a machine with only Docker installed, and packages it as a small, hardened container image with a health check.

## Requirements

### Requirement: Hardened container image
The project SHALL provide a container image that:
- contains only a Java runtime, not a JDK or build tools
- runs as a non-root user
- listens on port 8080
- declares a health check based on the service health endpoint

#### Scenario: Non-root runtime
- **WHEN** the image is built and inspected
- **THEN** its configured user is not `root` and not UID 0

#### Scenario: Container health
- **WHEN** a container from the image is running and connected to a reachable database
- **THEN** the container runtime reports the container as `healthy`

### Requirement: One-command local stack
From a fresh clone, `cp .env.example .env && docker compose up --build` SHALL start a PostgreSQL 16 database and the API. The API SHALL start only after the database reports healthy, and database data SHALL persist across stack restarts.

#### Scenario: Clean clone start
- **WHEN** a developer runs `cp .env.example .env && docker compose up --build` on a fresh clone with nothing but Docker installed
- **THEN** the database becomes healthy first, then the API becomes healthy
- **AND** `GET http://localhost:8080/actuator/health` returns 200 with `status` `UP`

#### Scenario: Data survives a restart
- **WHEN** the stack is stopped with `docker compose down` (without removing volumes) and started again
- **THEN** the database keeps its previously applied migrations and data

### Requirement: Build and test with only Docker
The project SHALL provide one documented command that compiles, runs every test (including container-backed tests) and enforces every build gate inside a container. The host needs nothing but Docker.

#### Scenario: Containerized verification
- **WHEN** a developer runs `./scripts/verify-in-docker.sh` on a Linux host with Docker
- **THEN** the full build and test suite runs inside a Maven container, and the command's exit status is the build's result
- **AND** no file created in the working tree is owned by root
