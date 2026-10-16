# Design: add-local-crud

## Context

US03 delivered the `LocalPokemon` aggregate, with proprietary-data value objects and their rules, and the V3 schema with a `version` column, `text[]` tags and a GIN index. It also delivered `GET /{id}` with an `ETag`, `LocalPokemonRepositoryPort`, a persistence adapter that maps optimistic-lock failures to `StaleLocalPokemonException`, `DuplicateLocalPokemonException`, and the 404/409 mappings. Security already allows `/api/v1/local-pokemon/**` for authenticated users and requires `ADMIN` for `DELETE`. Requirements are in `specs/local-pokemon/spec.md`.

## Goals / Non-Goals

**Goals:**
- Safe edits: no lost updates (`If-Match` is required), no edits to PokeAPI-owned data, and every validation problem reported at once.
- One place for the rules: the domain value objects. The web layer adds no duplicate limits.

**Non-Goals:**
- Server-driven field selection, sorting options and full-text search. The list is always ordered by `pokeapiId`.

## Decisions

### D1. Status codes for concurrency (409, as the brief asks)
- **Missing `If-Match` on `PUT`/`PATCH` → 428 `precondition-required`** (RFC 6585), so a client can't overwrite blindly. `DELETE` accepts a missing `If-Match`, because an admin deleting a record has nothing to lose.
- **`If-Match` for an older version → 409 `conflict`**, as the brief requires ("stale update returns 409, using optimistic locking with `@Version` and `If-Match`/ETag").
  - **Trade-off:** RFC 9110 suggests 412 Precondition Failed for a failed `If-Match`. The product owner confirmed 409 for this API. The 412 problem type stays documented for completeness but isn't emitted.
- **The version is checked twice:**
  - first by the use case against the `If-Match` value, which avoids pointless work
  - then by JPA `@Version` at save, which catches a concurrent writer between read and save. Both end as `StaleLocalPokemonException` → 409.
- **`If-Match` parsing:** `"3"` and `W/"3"` → 3. `*` → any existing version. Anything else → 400 `errors[If-Match]`.

### D2. Domain additions
- **`LocalPokemon.withProprietary(ProprietaryData, Instant now)`:** new proprietary data, `updatedAt = now`, with upstream data and `syncedAt` unchanged.
- **`LocalPokemon.requireVersion(long expected)`:** throws `StaleLocalPokemonException` on a mismatch.
- **`ProprietaryData.of(...)` collects errors:** it builds each part, gathers every `InvalidFieldException`, and throws **`InvalidFieldsException(List<InvalidFieldException>)`**, a new domain exception mapped to one 400 with all entries. The existing constructor keeps failing fast, which suits trusted callers.
- **`ProprietaryPatch` (domain):**
  - a tri-state `FieldPatch<T>` (sealed: `Keep`, `Clear`, `Set(value)`) for `region`, `habitatNotes`, `notes` and `internalTags`
  - a `LocalizedNamesPatch` (`boolean clearAll` for `"localizedNames": null`, `Map<String, String> put`, `Set<String> remove`)
  - `applyTo(current)` returns the patched *raw* values, which are then validated through `ProprietaryData.of`, so PATCH and PUT share the same rules and error reporting.

### D3. Use cases (one class each)
| Use case | Behaviour |
|---|---|
| `ListLocalPokemon` | `list(LocalPokemonFilter(tag, name), PageRequest)` → `Page<LocalPokemon>`. The tag is normalized like `InternalTags` (trim, lower case). |
| `CreateLocalPokemon` | `create(CreateLocalPokemonCommand(pokeapiId, ProprietaryValues), actor)` |
| `ReplaceLocalPokemon` | `replace(id, ExpectedVersion, ProprietaryValues)` |
| `PatchLocalPokemon` | `patch(id, ExpectedVersion, ProprietaryPatch)` |
| `DeleteLocalPokemon` | `delete(id, ExpectedVersion)` |

`ExpectedVersion` is `exactly(n)` (from `If-Match`) or `any()` (`If-Match: *`, or a `DELETE` without the header), so no use case takes an `Optional` parameter. `ProprietaryValues` holds the raw client values and converts through `ProprietaryData.of`. `LocalPokemonFilter` is a domain value object, because both the inbound and the outbound port use it; it normalizes the tag like `InternalTags` and lower-cases the name, and blank criteria are ignored.

**Create flow:**
1. Validate `pokeapiId` (positive) and the proprietary values first (cheap; every error reported together).
2. `existsByPokeapiId` → `DuplicateLocalPokemonException`, mapped to **409 with `errors[pokeapiId]`**.
3. `GetPokemonDetail.get(ById)`. A `PokemonNotFoundException` becomes `InvalidFieldException("pokeapiId", "no Pokémon has this number")` → 400; unavailability → 503.
4. Build with `LocalPokemon.importFrom(...).withProprietary(...)` and save. The unique key still guards races (409).

**Replace, patch and delete flow:** load (404) → `requireVersion` (409) → apply → save (JPA version check: 409). Delete removes the row with a version-checked delete in the adapter.

### D4. Persistence additions
- **Port:** `Page<LocalPokemon> findAll(LocalPokemonFilter, PageRequest)`, `boolean existsByPokeapiId(PokemonId)`, `void delete(LocalPokemon)`.
- **List query:** a **native query** with a count query, so the GIN index is used:
  - `WHERE (CAST(:tag AS text) IS NULL OR internal_tags @> ARRAY[CAST(:tag AS text)])`
  - `AND (CAST(:name AS text) IS NULL OR name ILIKE '%' || :name || '%')`
  - `ORDER BY pokeapi_id`
  - `LIMIT`/`OFFSET` from `PageRequest`
  - `ILIKE` wildcards in `name` (`%`, `_`) are escaped before binding.
- **No N+1 on lists:** localized names get `@BatchSize(size = 100)`, so a page loads them in one extra query.
- **Delete:** `deleteById` after a version check, inside one transaction (`@Transactional` on the adapter method). A concurrent change makes the JPA `@Version` delete fail → `StaleLocalPokemonException`.

### D5. Web
- **`POST /api/v1/local-pokemon`:** `CreateLocalPokemonRequest(@NotNull @Positive Integer pokeapiId, Map<String,String> localizedNames, String region, String habitatNotes, List<String> internalTags, String notes)` → 201 with `Location` and `ETag`.
- **`PUT /{id}`:** `ReplaceLocalPokemonRequest` (the same proprietary members, without `pokeapiId`).
- **`PATCH /{id}`:** consumes `application/merge-patch+json` and `application/json`. The body is read as a Jackson 3 `ObjectNode` and turned into `ProprietaryPatch` by `MergePatchReader`:
  - members are checked against the writable set
  - an upstream, identity or audit member → read-only error
  - any other member → "is not an accepted field"
  - a JSON type mismatch → "has the wrong type"
  - every problem is reported together
- **Read-only fields on `POST`/`PUT`:** the request records carry **`@ReadOnlyFields(LocalPokemonResponse.class)`**, a small web-layer annotation: a member of the response representation that the request doesn't declare is read-only, so the list never has to be maintained by hand. When Jackson reports an `UnrecognizedPropertyException`, `GlobalExceptionHandler` checks the referring class and answers `"is read-only: it comes from PokeAPI or is managed by the service"` instead of the generic message. `MergePatchReader` applies the same rule to `PATCH`, using `ReplaceLocalPokemonRequest` as its shape.
  - **Caveat:** Jackson stops at the first unknown member, so a POST/PUT reports the first read-only field. PATCH, which reads the tree, reports all of them.
- **`DELETE /{id}`:** 204 with an optional `If-Match`.
- **`GET /`:** list with `page` (`@Min(0)`), `size` (`@Min(1) @Max(100)`), `tag` and `name` (each `@Size(max = 50)`).
- **`IfMatch` helper:** parses the header; `PreconditionRequiredException` (web) → 428. A malformed header, like a malformed merge-patch member, raises the web-layer `InvalidRequestException` (400 with every entry), so request-format problems never masquerade as domain rule violations.
- **Two controllers:** `LocalPokemonSyncController` keeps the PokeAPI import endpoints from US03, and `LocalPokemonController` becomes the plain REST resource (list, read, create, replace, patch, delete). Same base path; each has one reason to change.
- **`GlobalExceptionHandler` additions:**
  - `InvalidFieldsException` → 400 with all entries
  - `DuplicateLocalPokemonException` → 409 with `errors[pokeapiId]`
  - read-only messages
  - 428

### D6. Testing
| Test | What it covers |
|---|---|
| Domain | `ProprietaryPatchTest` (keep, clear, set; localized-name merge and remove; tags replace), `ProprietaryDataTest` additions (`of` collects all violations), `LocalPokemonTest` additions (`withProprietary`, `requireVersion`) |
| Use cases (Mockito) | list, create (duplicate before PokeAPI, unknown number → 400, unavailable → 503, invalid fields before anything), replace/patch (404, stale 409, rules), delete (404, stale) |
| `LocalPokemonPersistenceAdapterIT` additions | tag filter case-insensitivity and GIN containment, name `ILIKE` with escaping, combined filters, paging totals, `existsByPokeapiId`, delete, stale delete |
| `LocalPokemonControllerTest` additions | every status code in the spec (201/200/204/400/404/409/428), read-only messages (POST, PUT, PATCH), merge-patch content type, `If-Match` parsing (`"3"`, `W/"3"`, `*`, garbage) |
| `LocalPokemonCrudIT` | full flow: create → list by tag → patch → stale patch (409) → replace → delete as ADMIN (204) → GET 404; USER delete → 403 |

### Patterns introduced in this change
| Pattern | Where |
|---|---|
| Notification (collect, then report) | `ProprietaryData.of` → `InvalidFieldsException` |
| Tri-state field patch (Merge Patch) | `FieldPatch` (sealed), `ProprietaryPatch`, `MergePatchReader` |
| Optimistic Offline Lock | `If-Match` ↔ `version` ↔ JPA `@Version` |
| Specification-like filter | `LocalPokemonFilter` → native query |

## Risks / Trade-offs

- [409 instead of 412 for a failed `If-Match`] → agreed with the product owner and documented. The frontend treats both as "reload and retry".
- [Jackson stops at the first unknown member on POST/PUT] → one read-only field is reported per request on those verbs; PATCH reports all. Acceptable, and documented.
- [Native SQL for the list] → it's tied to PostgreSQL, which is fine because it's the only database and Testcontainers runs the real one. The query is covered by integration tests.
- [`name` filter on the PokeAPI name only] → localized-name search is listed as future work.
