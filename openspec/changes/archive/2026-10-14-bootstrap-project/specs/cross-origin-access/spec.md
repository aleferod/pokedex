# Spec Delta: cross-origin-access

## Purpose

Controls which browser origins may call the API, so the Pokédex web frontend works across origins while every other site is refused.

## ADDED Requirements

### Requirement: Configured origins only
The service SHALL allow cross-origin browser requests to paths under `/api/` only from the exact origins listed in its configuration, given as a comma-separated list. A wildcard origin SHALL NOT be accepted as configuration.

#### Scenario: Preflight from an allowed origin
- **WHEN** the configured origins include `http://localhost:5173`
- **AND** a browser sends `OPTIONS /api/v1/pokemon` with `Origin: http://localhost:5173` and `Access-Control-Request-Method: GET`
- **THEN** the response status is 200
- **AND** `Access-Control-Allow-Origin` is `http://localhost:5173`

#### Scenario: Preflight from a disallowed origin
- **WHEN** a browser sends a preflight request to `/api/v1/pokemon` with `Origin: https://evil.example`
- **THEN** the response status is 403
- **AND** the response carries no `Access-Control-Allow-Origin` header

#### Scenario: Wildcard rejected at startup
- **WHEN** the service is started with the allowed origins set to `*`
- **THEN** startup fails with a message stating that explicit origins are required

### Requirement: Allowed methods and headers
For allowed origins, the service SHALL permit:
- **methods:** `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`
- **request headers:** `Authorization`, `Content-Type`, `If-Match`, `X-Correlation-Id`
- **headers exposed to browser scripts:** `Location`, `ETag`, `X-Correlation-Id`

#### Scenario: Preflight advertises methods and headers
- **WHEN** an allowed origin sends a preflight for `PATCH` with `Access-Control-Request-Headers: Authorization, If-Match`
- **THEN** `Access-Control-Allow-Methods` includes `PATCH`
- **AND** `Access-Control-Allow-Headers` includes `Authorization` and `If-Match`

#### Scenario: Response headers readable by the frontend
- **WHEN** an allowed origin makes a simple cross-origin `GET` to a path under `/api/`
- **THEN** `Access-Control-Expose-Headers` includes `Location`, `ETag` and `X-Correlation-Id`
