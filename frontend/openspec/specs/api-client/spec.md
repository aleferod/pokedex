# api-client Specification

## Purpose
How the SPA talks to pokedex-api: one client, same-origin by default, and one error type built from the API's RFC 7807 problem documents, plus the query retry policy.

## Requirements

### Requirement: One client for the API
Every call to pokedex-api SHALL go through one client that prefixes the configured base URL (`VITE_API_BASE_URL`, default `/api/v1`), sends and accepts JSON, supports cancellation, and resolves an empty or 204 response to no value.

#### Scenario: Default base URL
- **WHEN** a feature requests `/pokemon?page=0`
- **THEN** the browser calls `/api/v1/pokemon?page=0` on the same origin with `Accept: application/json`

#### Scenario: No content
- **WHEN** the API answers 204
- **THEN** the call resolves without a value

### Requirement: Problem details become one error type
Every unsuccessful response SHALL reject with an `ApiError` that carries the HTTP status, the problem type taken from the `type` URI fragment, `title`, `detail`, the field errors grouped by field, and the `X-Correlation-Id` header. A response that is not a problem document SHALL still become an `ApiError` with its status and a generic message.

#### Scenario: Validation problem
- **WHEN** the API answers 400 with a problem whose `errors` contain `title` twice and `region` once
- **THEN** the `ApiError` has status 400, type `bad-request`, and field errors `{title: [two messages], region: [one message]}`

#### Scenario: Conflict
- **WHEN** the API answers 409 with type `…/problems.md#conflict`
- **THEN** the `ApiError` has status 409 and type `conflict`, and its message is the problem's `detail`

#### Scenario: Not a problem document
- **WHEN** a proxy answers 502 with an HTML page
- **THEN** the `ApiError` has status 502, no field errors, and a generic message

### Requirement: Network failures
A request that cannot reach the API SHALL reject with an `ApiError` marked as a network error (status 0) and a message telling the user to check the connection; a cancelled request SHALL reject with the original abort error.

#### Scenario: Offline
- **WHEN** the network request fails
- **THEN** the `ApiError` has status 0 and `isNetworkError` true

### Requirement: Retry policy
Queries SHALL NOT be retried after a 4xx `ApiError`; other failures SHALL be retried once.

#### Scenario: Not found is final
- **WHEN** a query fails with a 404 `ApiError`
- **THEN** it is not retried
