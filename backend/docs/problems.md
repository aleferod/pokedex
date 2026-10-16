# Problem types

Every error response from the Pokédex API is an [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) problem detail with media type `application/problem+json` and exactly these members:

| Member | Meaning |
|---|---|
| `type` | URI of one of the sections below |
| `title` | Short summary of the problem type. The same for every occurrence. |
| `status` | HTTP status code |
| `detail` | What went wrong in this specific request, in plain language. Safe to show to users. |
| `instance` | Path of the request that failed |
| `timestamp` | When the error happened, as ISO-8601 UTC |
| `errors` | Field-level errors as `[{ "field", "message" }]`. Empty when the problem isn't about specific fields. |

Every response also carries an `X-Correlation-Id` header. Quote it when reporting a problem: the server logs every line written for that request under the same id.

```json
{
  "type": "https://github.com/aleferod/pokedex/blob/main/backend/docs/problems.md#not-found",
  "title": "Resource not found",
  "status": 404,
  "detail": "No endpoint GET /api/v1/does-not-exist.",
  "instance": "/api/v1/does-not-exist",
  "timestamp": "2026-10-14T12:00:00Z",
  "errors": []
}
```

Responses never contain stack traces, exception names, SQL or configuration values.

## bad-request

**400 Bad request.** The request is malformed or has invalid values. `errors` lists each offending field and why:
- missing or invalid fields
- values of the wrong JSON type (`"has the wrong type"`)
- members the endpoint doesn't accept (`"is not an accepted field"`, for example sending `role` when registering)
- password-policy violations: one `password` entry per broken rule
- read-only members (`"is read-only: it comes from PokeAPI or is managed by the service"`), for example `weight` in a local Pokémon update
- several business rules at once: one entry per offending field, all in the same response (for example `region` and `internalTags`)
- a `pokeapiId` that PokeAPI doesn't know when creating a local record (`"no Pokémon has this number"`)
- a malformed `If-Match` header: an entry for `If-Match`. Send one ETag such as `"3"`, `W/"3"`, or `*`.

A body that isn't JSON at all gets the detail `Malformed JSON request body.` and an empty `errors`.

**Fix:** correct the request as described in `detail` and `errors`, then retry.

## unauthorized

**401 Authentication required.** One of these:
- A protected endpoint was called without a bearer token. The detail says a token is required.
- The token is invalid, tampered with, from another issuer, or expired. The detail says the token is invalid or has expired, and `WWW-Authenticate` contains `error="invalid_token"`.
- `POST /api/v1/auth/login` got a wrong username or password. The detail is always `Invalid username or password.`, so it never reveals whether the account exists.

**Fix:** log in with `POST /api/v1/auth/login` and send `Authorization: Bearer <accessToken>`. After the token's `expiresAt`, log in again.

## forbidden

**403 Access denied.** You're authenticated, but your role isn't allowed to do this. For example, `DELETE` on `/api/v1/local-pokemon/**` requires the `ADMIN` role.

**Fix:** use an account with the required role.

## not-found

**404 Resource not found.** Nothing exists at this path, or the resource it names doesn't exist. For example, `GET /api/v1/pokemon/missingno` answers `No Pokémon matches 'missingno'.` and `GET /api/v1/local-pokemon/{id}` with an unused id answers `No local Pokémon with id … .`

**Fix:** check the path and the identifier.

## method-not-allowed

**405 Method not allowed.** The resource exists but doesn't support this HTTP method. The `Allow` response header lists the methods it does support.

**Fix:** use one of the methods listed in `Allow`.

## not-acceptable

**406 Not acceptable.** The API can't produce any of the media types listed in the request's `Accept` header.

**Fix:** accept `application/json`. Errors are always sent as `application/problem+json`.

## conflict

**409 Conflict.** The request conflicts with the resource's current state:
- **It already exists.** When registering, `errors` names the taken field (`username` or `email`, ignoring case). When creating a local Pokémon, `errors` names `pokeapiId`.
- **It changed since you read it.** The `If-Match` ETag of a `PUT`, `PATCH` or `DELETE` is outdated, or a local Pokémon keeps changing while a sync retries. Nothing was changed. This API answers 409 here rather than 412, as the exercise asks.

**Fix:** read the current state, then retry with up-to-date data.

## precondition-failed

**412 Precondition failed.** Reserved. RFC 9110 suggests 412 when an `If-Match` ETag no longer matches, but this API answers that case with [409 conflict](#conflict), as the exercise asks. The type is documented for completeness and isn't currently returned.

**Fix:** fetch the resource again to get the current `ETag`, reapply your change, and retry.

## payload-too-large

**413 Payload too large.** The request body is larger than the API accepts.

**Fix:** send a smaller payload.

## unsupported-media-type

**415 Unsupported media type.** The request body's `Content-Type` isn't accepted by this endpoint.

**Fix:** send `Content-Type: application/json`. `PATCH` on a local Pokémon also accepts `application/merge-patch+json`.

## precondition-required

**428 Precondition required.** This update has to be conditional, so that it can't silently overwrite somebody else's change. `PUT` and `PATCH` on `/api/v1/local-pokemon/{id}` need an `If-Match` header; `errors` names `If-Match`.

**Fix:** send the resource's current `ETag` in an `If-Match` header.

## internal-error

**500 Internal server error.** Something unexpected went wrong on the server. The details are logged, never returned.

**Fix:** retry later. If it keeps happening, report it and quote the correlation id given in `detail`.

## service-unavailable

**503 Service unavailable.** A dependency is temporarily unavailable. For example, PokeAPI is unreachable, kept answering with server errors after the API's retries, or didn't respond within the read timeout. After repeated failures the API's circuit breaker opens: for about 30 seconds it answers 503 at once, without calling PokeAPI. The API fails fast instead of hanging. The upstream error is logged with the correlation id, and never returned.

**Fix:** retry after the number of seconds in the `Retry-After` response header.

## client-error

**Other 4xx.** A client error with no more specific type. `status` holds the exact code.

**Fix:** see `detail`.

## server-error

**Other 5xx.** A server error with no more specific type. `status` holds the exact code.

**Fix:** retry later, and quote the correlation id if you report it.
