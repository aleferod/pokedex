# Spec Delta: authentication

## Purpose

Exchanges valid credentials for a short-lived bearer token, and identifies the caller on later requests from that token alone, with no server-side session.

## ADDED Requirements

### Requirement: Login issues a bearer token
`POST /api/v1/auth/login` with a correct `username` (case-insensitive) and `password` SHALL answer 200. The body SHALL contain `accessToken`, `tokenType` `Bearer`, `expiresAt` (ISO-8601) and the account's `id`, `username`, `email` and `role`.

#### Scenario: Successful login
- **WHEN** account `ash` with password `Pikachu@25` exists and a client logs in with `username` `Ash` and that password
- **THEN** the response status is 200
- **AND** the body contains a non-empty `accessToken`, `tokenType` `Bearer`, an `expiresAt` in the future, and `user.username` `ash`

### Requirement: Bad credentials are indistinguishable
A login with an unknown username or a wrong password SHALL answer 401 with a problem of type `unauthorized` and the same `detail`. The response SHALL NOT reveal whether the username exists.

#### Scenario: Wrong password
- **WHEN** a client logs in as `ash` with password `Wrong@1234`
- **THEN** the response status is 401 and `detail` is `Invalid username or password.`

#### Scenario: Unknown username
- **WHEN** a client logs in as `nobody` with any password
- **THEN** the response status is 401 and `detail` is `Invalid username or password.`

#### Scenario: Missing credentials
- **WHEN** a client logs in without a `password` member
- **THEN** the response status is 400 and `errors` contains an entry for field `password`

### Requirement: Token properties
The access token SHALL be signed by the service. It SHALL identify the account by its id, carry the username and role, name the service as issuer, and expire after a configured lifetime (default one hour). A token that is expired, has an invalid signature, was issued by someone else or is malformed SHALL be rejected with 401 and a `WWW-Authenticate: Bearer` header reporting `invalid_token`.

#### Scenario: Expired token
- **WHEN** a request to a protected route carries a token whose lifetime has passed
- **THEN** the response status is 401 and `WWW-Authenticate` contains `error="invalid_token"`

#### Scenario: Tampered token
- **WHEN** a request carries a token whose payload was changed after signing
- **THEN** the response status is 401 and `WWW-Authenticate` contains `error="invalid_token"`

### Requirement: Current user
`GET /api/v1/auth/me` with a valid token SHALL answer 200 with the caller's `id`, `username`, `email`, `role` and `createdAt`. If the account no longer exists, it SHALL answer 401.

#### Scenario: Who am I
- **WHEN** `ash` calls `GET /api/v1/auth/me` with the token from their login
- **THEN** the response status is 200 and the body's `username` is `ash`, with role `USER`

#### Scenario: No token
- **WHEN** a client calls `GET /api/v1/auth/me` without an `Authorization` header
- **THEN** the response status is 401, with a problem of type `unauthorized` and a `WWW-Authenticate: Bearer` header
