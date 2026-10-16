# Spec Delta: operational-endpoints

## Purpose

Exposes the endpoints that operators, container orchestrators and API consumers need: liveness and readiness health, build information, and the interactive API documentation. Every other management surface stays hidden.

## ADDED Requirements

### Requirement: Public health endpoint
`GET /actuator/health` SHALL be reachable without credentials. It SHALL report status `UP` with HTTP 200 when the service and its database are usable, and `DOWN` with HTTP 503 when the database is unreachable. Anonymous callers SHALL NOT receive component details such as database product, version or connection data.

#### Scenario: Healthy service
- **WHEN** an anonymous client calls `GET /actuator/health` and the database is reachable
- **THEN** the response status is 200 and the body's `status` is `UP`
- **AND** the body contains no component details: only the status and the names of the probe groups (`liveness`, `readiness`)

#### Scenario: Database unreachable
- **WHEN** an anonymous client calls `GET /actuator/health` and the database is unreachable
- **THEN** the response status is 503 and the body's `status` is `DOWN`

### Requirement: Liveness and readiness probes
The service SHALL expose `GET /actuator/health/liveness` and `GET /actuator/health/readiness` without credentials. Container platforms can use them to decide on restarts and traffic routing.

#### Scenario: Probes available
- **WHEN** an anonymous client calls `GET /actuator/health/liveness` or `GET /actuator/health/readiness` on a started service
- **THEN** each response status is 200 with `status` `UP`

### Requirement: Public build information
`GET /actuator/info` SHALL be reachable without credentials. It SHALL return the application's name and version, and SHALL NOT include environment variables or configuration values.

#### Scenario: Build info
- **WHEN** an anonymous client calls `GET /actuator/info`
- **THEN** the response status is 200 and the body contains the application name `pokedex-api` and its version

### Requirement: Management surface is minimal
Over HTTP, the service SHALL expose only the health and info management endpoints. Every other management endpoint SHALL be unavailable. This includes environment, configuration properties, beans, mappings, heap dump, thread dump, loggers and shutdown.

#### Scenario: Sensitive endpoint requested
- **WHEN** a client calls `GET /actuator/env`, `GET /actuator/beans` or `POST /actuator/shutdown`
- **THEN** the response status is 404 with a problem-detail body

### Requirement: Published API documentation
The service SHALL publish an OpenAPI 3 document at `GET /v3/api-docs` and an interactive UI at `GET /swagger-ui.html`, both reachable without credentials. The document SHALL carry the API title, version, description and license.

#### Scenario: OpenAPI document
- **WHEN** an anonymous client calls `GET /v3/api-docs`
- **THEN** the response status is 200
- **AND** the JSON contains an `openapi` version of 3.x, `info.title` `Pokédex API`, and the application version in `info.version`

#### Scenario: Swagger UI
- **WHEN** an anonymous client opens `/swagger-ui.html`
- **THEN** the client ends up at the Swagger UI page (HTTP 200 after redirects), and the page loads the service's OpenAPI document
