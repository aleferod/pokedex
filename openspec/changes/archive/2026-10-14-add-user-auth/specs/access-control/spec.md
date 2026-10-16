# Spec Delta: access-control

## Purpose

Decides which routes anyone may call, which need an authenticated caller, and which need the administrator role. Rejections are answered in the API's standard error format, and the API stays stateless.

## ADDED Requirements

### Requirement: Public routes
These routes SHALL be reachable without credentials:
- the Pokémon catalog and detail views (`GET /api/v1/pokemon/**`)
- `POST /api/v1/auth/register` and `POST /api/v1/auth/login`
- the OpenAPI document and Swagger UI
- `GET /actuator/health/**` and `GET /actuator/info`

Unknown routes SHALL keep answering 404 rather than 401.

#### Scenario: Anonymous catalog access
- **WHEN** an anonymous client calls `GET /api/v1/pokemon`
- **THEN** the request is not rejected for missing credentials (the response is not 401 or 403)

#### Scenario: Anonymous unknown route
- **WHEN** an anonymous client calls `GET /api/v1/does-not-exist`
- **THEN** the response status is 404

### Requirement: Authenticated routes
Every request to `/api/v1/local-pokemon/**` and to `GET /api/v1/auth/me` SHALL require a valid bearer token. Without one, the response SHALL be 401 with a problem of type `unauthorized` and a `WWW-Authenticate: Bearer` header.

#### Scenario: Anonymous access to the local replica
- **WHEN** an anonymous client calls `GET /api/v1/local-pokemon`
- **THEN** the response status is 401, the content type is `application/problem+json`, and `WWW-Authenticate` starts with `Bearer`

#### Scenario: Authenticated access to the local replica
- **WHEN** a caller with a valid `USER` token calls `GET /api/v1/local-pokemon`
- **THEN** the request is not rejected by access control (the response is not 401 or 403)

### Requirement: Administrator-only deletion
`DELETE` requests to `/api/v1/local-pokemon/**` SHALL require the `ADMIN` role. An authenticated caller without it SHALL get 403 with a problem of type `forbidden`.

#### Scenario: User tries to delete
- **WHEN** a caller with a valid `USER` token calls `DELETE /api/v1/local-pokemon/1`
- **THEN** the response status is 403 and the problem type is `forbidden`

#### Scenario: Admin deletes
- **WHEN** a caller with a valid `ADMIN` token calls `DELETE /api/v1/local-pokemon/1`
- **THEN** the request is not rejected by access control (the response is not 401 or 403)

### Requirement: Stateless API
The API SHALL keep no server-side session for authentication and SHALL NOT set cookies. Every protected request SHALL be authorized from its bearer token alone.

#### Scenario: No session cookie
- **WHEN** a client logs in and then calls a protected route
- **THEN** no response contains a `Set-Cookie` header

#### Scenario: Cookie-less protected request needs no CSRF token
- **WHEN** a caller with a valid token sends a state-changing request (for example `DELETE`) without any CSRF token
- **THEN** the request is not rejected for a missing CSRF token
