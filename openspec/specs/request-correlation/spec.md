# request-correlation Specification

## Purpose
Gives every request a correlation id that callers can see and that appears in every log line written for that request, so one failure can be traced from the browser to the server logs.

## Requirements

### Requirement: Correlation id on every response
Every HTTP response SHALL carry an `X-Correlation-Id` header. This includes error responses and responses from operational endpoints.

#### Scenario: Caller supplies a valid id
- **WHEN** a request carries `X-Correlation-Id: demo-123`
- **THEN** the response carries `X-Correlation-Id: demo-123`

#### Scenario: Caller supplies no id
- **WHEN** a request has no `X-Correlation-Id` header
- **THEN** the response carries an `X-Correlation-Id` header holding a newly generated UUID

#### Scenario: Error responses carry the id
- **WHEN** a request with `X-Correlation-Id: demo-404` targets an unknown route
- **THEN** the 404 problem-detail response carries `X-Correlation-Id: demo-404`

### Requirement: Untrusted correlation ids are replaced
The service SHALL accept a caller-supplied correlation id only if it is 1 to 64 characters long and contains only ASCII letters, digits and hyphens. Any other value SHALL be replaced with a newly generated UUID. This stops callers from injecting content into logs.

#### Scenario: Id too long
- **WHEN** a request carries an `X-Correlation-Id` of 65 characters
- **THEN** the response carries a newly generated UUID instead

#### Scenario: Id with illegal characters
- **WHEN** a request carries `X-Correlation-Id: abc\r\nINJECTED` or `X-Correlation-Id: <script>`
- **THEN** the response carries a newly generated UUID
- **AND** the rejected value never appears in the logs

### Requirement: Correlation id in logs
Every log entry written while a request is handled SHALL include that request's correlation id. The id SHALL NOT appear in log entries for later requests handled on the same thread.

#### Scenario: Log entry during a request
- **WHEN** the service writes a log entry while handling a request whose correlation id is `demo-log`
- **THEN** that log entry contains `demo-log`

#### Scenario: No leakage between requests
- **WHEN** a request with correlation id `first` completes and another request without a correlation id is handled on the same worker thread
- **THEN** log entries for the second request do not contain `first`
