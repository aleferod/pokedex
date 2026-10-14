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
  "type": "https://github.com/aleferod/pokedex-api/blob/main/docs/problems.md#not-found",
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

**400 Bad request.** The request is malformed: unreadable JSON, a parameter of the wrong type, a missing required parameter, or values that fail validation. When the problem is with specific fields, `errors` lists each field and why it was rejected.

**Fix:** correct the request as described in `detail` and `errors`, then retry.

## unauthorized

**401 Authentication required.** The endpoint is protected and the request has no valid bearer token, or the token has expired.

**Fix:** log in with `POST /api/v1/auth/login` and send `Authorization: Bearer <token>`.

## forbidden

**403 Access denied.** You're authenticated, but your role isn't allowed to do this. For example, deleting a local Pokémon requires the `ADMIN` role.

**Fix:** use an account with the required role.

## not-found

**404 Resource not found.** Nothing exists at this path, or the resource it names doesn't exist (for example, an unknown Pokémon).

**Fix:** check the path and the identifier.

## method-not-allowed

**405 Method not allowed.** The resource exists but doesn't support this HTTP method. The `Allow` response header lists the methods it does support.

**Fix:** use one of the methods listed in `Allow`.

## not-acceptable

**406 Not acceptable.** The API can't produce any of the media types listed in the request's `Accept` header.

**Fix:** accept `application/json`. Errors are always sent as `application/problem+json`.

## conflict

**409 Conflict.** The request conflicts with the resource's current state. For example, the resource already exists (a duplicate username, email or Pokémon), or it was changed by someone else since you read it.

**Fix:** read the current state, then retry with up-to-date data.

## precondition-failed

**412 Precondition failed.** The `If-Match` ETag you sent no longer matches the resource, because someone else updated it in the meantime.

**Fix:** fetch the resource again to get the current `ETag`, reapply your change, and retry.

## payload-too-large

**413 Payload too large.** The request body is larger than the API accepts.

**Fix:** send a smaller payload.

## unsupported-media-type

**415 Unsupported media type.** The request body's `Content-Type` isn't accepted by this endpoint.

**Fix:** send `Content-Type: application/json`.

## precondition-required

**428 Precondition required.** This update has to be conditional, so that it can't silently overwrite somebody else's change.

**Fix:** send the resource's current `ETag` in an `If-Match` header.

## internal-error

**500 Internal server error.** Something unexpected went wrong on the server. The details are logged, never returned.

**Fix:** retry later. If it keeps happening, report it and quote the correlation id given in `detail`.

## service-unavailable

**503 Service unavailable.** A dependency is temporarily unavailable, for example PokeAPI. The API fails fast instead of hanging.

**Fix:** retry after a short wait.

## client-error

**Other 4xx.** A client error with no more specific type. `status` holds the exact code.

**Fix:** see `detail`.

## server-error

**Other 5xx.** A server error with no more specific type. `status` holds the exact code.

**Fix:** retry later, and quote the correlation id if you report it.
