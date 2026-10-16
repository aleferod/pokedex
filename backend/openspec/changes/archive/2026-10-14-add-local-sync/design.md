# Design: add-local-sync

## Context

- **From earlier changes:** `GetPokemonDetail` / `PokemonCatalogPort.findDetail` (US02) already fetch and translate everything a local copy needs. `ListPokemon` (US01) gives the catalog order for bulk sync. `users`, JWT auth and the protected `/api/v1/local-pokemon/**` area exist (`add-user-auth`), and the authenticated principal's name is the username.
- **Persistence setup:** Flyway owns the schema (`ddl-auto=validate`). The microsecond clock and Testcontainers persistence tests are in place.
- **This change** adds the stored copy, the sync semantics, and the minimal read endpoint needed for `Location`. Requirements are in `specs/local-pokemon/spec.md`.

## Goals / Non-Goals

**Goals:**
- One aggregate with clear invariants: upstream data is replaceable by sync only, and proprietary data is never touched by sync.
- Correct under concurrency: a unique PokeAPI id plus optimistic locking, with no duplicate records and no lost updates.
- A schema ready for US04: version column, tag index, length limits enforced in the domain.

**Non-Goals:**
- The editing API (US04), scheduled syncs, and soft deletes.

## Decisions

### D1. Domain (`domain.localpokemon`)
- **`LocalPokemonId`:** a value object holding a UUID. `parse(String)` throws `InvalidFieldException("id")` on malformed input.
- **`UpstreamData`:** a record of name, spriteUrl, imageUrl, category, types, `Height`, `Weight`, abilities and `BaseStats`, built from a `PokemonDetail` with `UpstreamData.from(detail)`.
  - `PokemonDetail` gains `spriteUrl`, mapped by `PokeApiDetailMapper`. The US02 *response* doesn't expose it, so its contract is unchanged.
- **`ProprietaryData`:** a record of `LocalizedNames`, region, habitatNotes, `InternalTags` and notes, with `ProprietaryData.empty()`.
  - Text fields are trimmed, and blank becomes `null`.
  - Limits: region ≤ 50, habitatNotes ≤ 500, notes ≤ 2000, otherwise `InvalidFieldException` naming the field.
- **`LocalizedNames`:** an immutable `Map<String, String>` kept in key order.
  - At most 10 entries.
  - Each locale is BCP-47-like (`[a-z]{2,3}(-[A-Za-z0-9]{2,8}){0,2}`, for example `pt-BR`, `zh-Hant-TW`).
  - Each name is trimmed to 1–50 characters.
- **`InternalTags`:** an ordered set of lower-cased, de-duplicated tags.
  - At most 20 tags, each matching `[a-z0-9][a-z0-9-]{0,29}`.
- **`AuditInfo`:** createdAt, updatedAt, syncedAt and createdBy.
- **`LocalPokemon`** (aggregate):
  - **Fields:** `id`, `pokeapiId` (`PokemonId`), `upstream`, `proprietary`, `audit`, `version` (`Long`, `null` until first stored).
  - **Factory `importFrom(id, upstream, actor, now)`:** empty proprietary data, all three timestamps equal to `now`, createdBy = actor.
  - **`resyncWith(upstream, now)`:** returns a copy with the new upstream data, syncedAt = updatedAt = now, and **the same proprietary data and creation audit**. The invariant lives in one method, not spread across adapters.
- **Exceptions:** `LocalPokemonNotFoundException(LocalPokemonId)` → 404. `StaleLocalPokemonException` (version conflict) is used by US04 and by the sync retry.

### D2. Use cases and ports
- **Ports:** `SyncPokemon` (in): `SyncResult sync(PokemonIdentifier, String actor)`, where `SyncResult(LocalPokemon, boolean created)`.
- **`SyncPokemonUseCase` flow:**
  1. `GetPokemonDetail.get(identifier)`. This reuses the US02 use case, including its 404 and 503 behaviour.
  2. `repository.findByPokeapiId(detail.id())`.
  3. If stored: `save(existing.resyncWith(...))` → `created = false`. Otherwise: `save(LocalPokemon.importFrom(...))` → `created = true`.
- **Races:**
  - `DuplicateLocalPokemonException` from the insert (a concurrent first import won): reload and re-sync once → 200.
  - `StaleLocalPokemonException` from a re-sync (a concurrent re-sync won): reload and re-sync once.
  - Nothing is written before PokeAPI has answered, so a 404 or 503 leaves the store untouched.
- **`SyncCatalogPage`:** `BulkSyncResult syncFirst(int limit, String actor)`. It uses `ListPokemon` with page 0 and size = limit, then `SyncPokemon` for each entry sequentially, in catalog order. It stops at the first failure, which propagates as 503 or 404; entries already synced stay synced (documented). `limit` must be 1–20, otherwise `InvalidFieldException("limit")`.
- **`GetLocalPokemon`:** `LocalPokemon get(LocalPokemonId)` → `LocalPokemonNotFoundException`.
- **`LocalPokemonRepositoryPort`:**
  - `Optional<LocalPokemon> findById(LocalPokemonId)`
  - `Optional<LocalPokemon> findByPokeapiId(PokemonId)`
  - `LocalPokemon save(LocalPokemon)`, which inserts when the version is `null` and otherwise updates with a version check; it throws `DuplicateLocalPokemonException` or `StaleLocalPokemonException`.
- **Transactions:** each port call is its own transaction (Spring Data). The use case holds no transaction across the PokeAPI call, so there's no database connection held during slow upstream I/O.

### D3. Schema (`V3__create_local_pokemon.sql`)
**`local_pokemon`:**
- `id uuid` primary key; `pokeapi_id integer NOT NULL`, constraint `uk_local_pokemon_pokeapi_id`
- **Upstream columns:**
  - `name varchar(50)`, `sprite_url varchar(500)`, `image_url varchar(500)`, `category varchar(100)`
  - `types text[]`, `height_dm integer`, `weight_hg integer`, `abilities text[]`
  - six stat columns
- **Proprietary columns:** `region varchar(50)`, `habitat_notes varchar(500)`, `internal_tags text[] NOT NULL DEFAULT '{}'`, `notes varchar(2000)`
- **Audit and locking:** the audit columns `timestamptz`; `created_by varchar(30)`; `version bigint NOT NULL`
- **Checks:** non-negative stats, height and weight; array cardinality limits (`cardinality(internal_tags) <= 20`)
- **Indexes:** `ix_local_pokemon_internal_tags` (GIN, for US04 tag filtering) and `ix_local_pokemon_name`

**`local_pokemon_localized_name`:**
- `local_pokemon_id uuid` references `local_pokemon` `ON DELETE CASCADE`
- `locale varchar(35)`, `name varchar(50)`
- primary key `(local_pokemon_id, locale)`

**Rejected alternatives:**
- **`jsonb` for localized names.** Hibernate would need a Jackson 2 `FormatMapper`; only Jackson 3 is a direct dependency, and Jackson 2 arrives only through springdoc. A child table is plain relational, validated by Hibernate, and queryable.
- **Child tables for tags, types and abilities.** That means three more joins for value lists that are always read whole. PostgreSQL arrays, mapped natively by Hibernate 7 (`@JdbcTypeCode(SqlTypes.ARRAY)`), keep one row per Pokémon, and the GIN index serves tag filters.

### D4. Persistence adapter (`adapter.out.persistence.localpokemon`)
- **`LocalPokemonJpaEntity`:** flattened columns.
  - `@Version Long version`. `null` means new, so Spring Data persists rather than merges. Hibernate 6.6+ rejects merging a detached versioned entity whose row doesn't exist.
  - `@ElementCollection(fetch = EAGER) @CollectionTable(local_pokemon_localized_name) @MapKeyColumn(locale)` for the localized names.
  - `@JdbcTypeCode(SqlTypes.ARRAY)` on the `List<String>` columns.
- **`SpringDataLocalPokemonRepository`:** `findByPokeapiId`.
- **`LocalPokemonPersistenceMapper`** is **hand-written**. The aggregate is nested records (upstream, proprietary, audit) while the entity is flat. MapStruct's strength, matching field names, doesn't apply, and the explicit mapping is shorter and unit-tested.
  - Rows are rebuilt through the value objects, so invalid stored data fails loudly.
- **`LocalPokemonPersistenceAdapter`:**
  - `saveAndFlush`, so constraint and version errors surface inside the adapter.
  - `uk_local_pokemon_pokeapi_id` → `DuplicateLocalPokemonException`
  - `ObjectOptimisticLockingFailureException` → `StaleLocalPokemonException`

### D5. Web (`adapter.in.web.localpokemon`)
- **`LocalPokemonController`:**
  - `POST /sync/{idOrName}` → 201, with `Location` built from the request URI's base and `ETag: "<version>"`, or 200 with `ETag`.
  - `POST /sync?limit=` → 200 with `BulkSyncResponse`. `limit` is checked with `@Min(1) @Max(20)` (method validation, as for paging).
  - `GET /{id}` → 200 with `ETag`.
- **Actor:** `Authentication.getName()`, the `username` claim.
- **`LocalPokemonResponse`:** id, pokeapiId, the upstream fields (height m, weight kg, stats object), `localizedNames` (object), region, habitatNotes, internalTags, notes, createdAt, updatedAt, syncedAt, createdBy, version. `LocalPokemonWebMapper` builds it.
- **`GlobalExceptionHandler`:** `LocalPokemonNotFoundException` → 404 `not-found`.

### D6. Testing
| Test | What it covers |
|---|---|
| Domain unit | `LocalPokemonTest` (import defaults; re-sync replaces upstream only and keeps proprietary + creation audit), `ProprietaryDataTest`, `LocalizedNamesTest`, `InternalTagsTest` (trim, case, dedupe, pattern, limits), `LocalPokemonIdTest` |
| Use case unit (Mockito) | `SyncPokemonUseCaseTest` (import, re-sync, not found / unavailable leave the store untouched, duplicate → retry as re-sync, stale → retry once), `SyncCatalogPageUseCaseTest` (order, counts, limit bounds, stop at first failure), `GetLocalPokemonUseCaseTest` |
| `LocalPokemonPersistenceAdapterIT` (`@DataJpaTest` + Testcontainers) | round trip including arrays and localized names; update increments version; duplicate PokeAPI id → `DuplicateLocalPokemonException`; stale version → `StaleLocalPokemonException`; Hibernate `validate` against V3 |
| `LocalPokemonControllerTest` (web slice, real tokens) | 201 with `Location`/`ETag`, 200 re-sync, 404, 400 `idOrName`/`id`/`limit`, 503, 401 anonymous, `GET /{id}` 200 with `ETag`, 404 |
| `LocalPokemonSyncIT` (full stack, Testcontainers + WireMock) | import → GET via Location → proprietary fields set directly through the repository → upstream changes in WireMock → re-sync keeps proprietary and refreshes upstream; concurrent first imports → one record (201 + 200); PokeAPI 500 during re-sync → 503 with the record unchanged; bulk `limit=3` |

### Patterns introduced in this change
| Pattern | Where |
|---|---|
| Aggregate + Factory | `LocalPokemon.importFrom`, `resyncWith` |
| Value Object | `LocalPokemonId`, `LocalizedNames`, `InternalTags`, `ProprietaryData`, `UpstreamData`, `AuditInfo` |
| Repository | `LocalPokemonRepositoryPort` ← `LocalPokemonPersistenceAdapter` |
| Optimistic locking | `version` / `@Version`, exposed as `ETag` |
| Use-case composition | `SyncPokemonUseCase` reuses `GetPokemonDetail`; `SyncCatalogPageUseCase` reuses `ListPokemon` + `SyncPokemon` |

## Risks / Trade-offs

- [Re-sync costs 3 PokeAPI calls, bulk sync of 20 costs about 100] → acceptable at these caps; caching in `add-caching-resilience` reduces it.
- [Bulk sync isn't atomic: entries synced before a failure stay] → each entry is independently valid, so this is documented rather than rolled back. Clients can simply retry.
- [EAGER element collection for localized names] → a single extra query per record read. US04's list endpoint uses batch fetching (`@BatchSize`) to avoid N+1.
- [The retry-once on races can still lose under extreme contention] → the second failure surfaces as 503/409 rather than corrupting data. Acceptable for a manual sync endpoint.
