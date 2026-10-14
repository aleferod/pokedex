# api-errors Specification

## Purpose
Defines the single error response contract that every API endpoint follows, so clients can handle failures uniformly and no internal details leak to callers.

## Requirements

### Requirement: Problem detail error format
Every error response (HTTP status 4xx or 5xx) SHALL have media type `application/problem+json`. Its body SHALL contain exactly these members:
- `type`: a URI identifying the problem type
- `title`: a short summary that is the same for every occurrence of that type
- `status`
- `detail`
- `instance`: the request path
- `timestamp`: ISO-8601 UTC
- `errors`: an array, empty when there are no field-level errors

#### Scenario: Unknown route
- **WHEN** a client sends `GET /api/v1/does-not-exist`
- **THEN** the response status is 404
- **AND** the content type is `application/problem+json`
- **AND** the body has `status` 404, `instance` `/api/v1/does-not-exist`, a non-empty `type`, `title` and `detail`, an ISO-8601 `timestamp`, and `errors` equal to `[]`

#### Scenario: Method not allowed
- **WHEN** a client sends `DELETE /actuator/health`
- **THEN** the response status is 405 with a problem-detail body whose `status` is 405
- **AND** the `Allow` header lists the supported methods

#### Scenario: Error raised outside request handling
- **WHEN** a request fails before it reaches a request handler, for example because the servlet container rejects it
- **THEN** the response still uses the problem-detail format, with the status that matches the failure

### Requirement: No internal details in error responses
Error responses SHALL NOT expose any of these:
- stack traces
- exception class names
- framework or library messages
- SQL
- file paths
- configuration values

#### Scenario: Unexpected server failure
- **WHEN** handling a request fails with an unexpected internal error
- **THEN** the response status is 500
- **AND** `detail` is a generic message that tells the caller to quote the correlation id
- **AND** the body contains no stack trace, exception class name or exception message

#### Scenario: Server-side logging of unexpected failures
- **WHEN** an unexpected internal error produces a 500 response
- **THEN** the service logs the error at ERROR level with its full stack trace and the request's correlation id

### Requirement: Documented problem types
Each problem `type` URI the API returns SHALL resolve to human-readable documentation for that problem type.

#### Scenario: Problem type is documented
- **WHEN** a client receives a problem detail with a given `type`
- **THEN** that URI points to a section of the published problem-type documentation that describes the problem and how to fix it
