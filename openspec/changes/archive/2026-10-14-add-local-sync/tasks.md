# Tasks: add-local-sync

TDD convention as in earlier changes: each `test:` commit is red on assertions, and the `feat:` commit makes it green.

## 1. Domain

- [x] 1.1 Write `LocalPokemonIdTest`, `LocalizedNamesTest`, `InternalTagsTest`, `ProprietaryDataTest` and `LocalPokemonTest` (D1):
  - UUID parsing, with `id` named on error
  - locale pattern; name trim and 1–50; at most 10 entries
  - tag lower-casing, trim, dedupe, pattern, at most 20
  - region/habitat/notes limits; blank → `null`
  - `importFrom` defaults
  - `resyncWith` keeps proprietary data and creation audit
  - Verify: red against stubs.
- [x] 1.2 Implement the value objects, `UpstreamData` (with `from(PokemonDetail)`), `AuditInfo`, the `LocalPokemon` aggregate and its exceptions. Add `spriteUrl` to `PokemonDetail`, filled by `PokeApiDetailMapper` (a test asserts it). Verify: 1.1 green; ArchUnit and existing tests green.

## 2. Application

- [x] 2.1 Write `SyncPokemonUseCaseTest`, `SyncCatalogPageUseCaseTest` and `GetLocalPokemonUseCaseTest` (D2). Verify: red.
- [x] 2.2 Implement the ports (`SyncPokemon`, `SyncCatalogPage`, `GetLocalPokemon`, `LocalPokemonRepositoryPort`, result records) and the three use cases. Verify: 2.1 green; domain + application coverage ≥ 85 %.

## 3. Persistence

- [x] 3.1 Write `LocalPokemonPersistenceAdapterIT` (`@DataJpaTest` + Testcontainers):
  - round trip including arrays and localized names
  - update increments version
  - duplicate PokeAPI id → `DuplicateLocalPokemonException`
  - stale version → `StaleLocalPokemonException`
  - Verify: red.
- [x] 3.2 Implement `V3__create_local_pokemon.sql`, `LocalPokemonJpaEntity`, `SpringDataLocalPokemonRepository`, `LocalPokemonPersistenceMapper` and `LocalPokemonPersistenceAdapter` (D3, D4). Verify: 3.1 green; `SchemaMigrationIT` green (V1–V3; Hibernate validate).

## 4. Web

- [x] 4.1 Write `LocalPokemonControllerTest` (web slice, mocked use cases, real tokens):
  - 201 with `Location` and `ETag`; 200 re-sync
  - 404, 503
  - 400 for `idOrName`, `id` and `limit`
  - 401 anonymous
  - `GET /{id}` 200 with `ETag`, and 404
  - bulk response shape
  - Verify: red.
- [x] 4.2 Implement `LocalPokemonController`, `LocalPokemonResponse`, `BulkSyncResponse`, `LocalPokemonWebMapper`, the 404 mapping and the use-case wiring. Verify: 4.1 green.

## 5. End-to-end and docs

- [x] 5.1 Write `LocalPokemonSyncIT` (Testcontainers + WireMock):
  - import → GET via `Location` equals the import body
  - proprietary fields set through the repository, upstream weight changed in WireMock → re-sync keeps proprietary and refreshes upstream, with a higher version
  - concurrent first imports → one record (one 201, one 200)
  - PokeAPI 500 during re-sync → 503 and the record unchanged
  - bulk `limit=3` → created 3
  - Verify: green with full `verify`.
- [x] 5.2 Update the README (US03 row ✅; a local-replica section on sync semantics, proprietary fields and the audit/version/ETag; the sync rows in the endpoint table; ER diagram in Mermaid) and `docs/problems.md` (404 also for local records). Verify: lychee offline shows no broken links.

## 6. Integration verification

- [x] 6.1 `./scripts/verify-in-docker.sh` from a clean `target/`. Verify: exit 0; record counts and coverage.
- [x] 6.2 From a fresh clone with `docker compose up --build` against the real PokeAPI:
  - register/login → sync pikachu (201) → GET `Location` → re-sync (200, higher `ETag`)
  - bulk `limit=5`
  - anonymous sync → 401
  - inspect the rows with `psql`
  - Verify: as in the spec; `docker compose down -v`.
- [x] 6.3 Run `openspec validate add-local-sync --strict` and gitleaks. Verify: valid, no leaks.

## Workflow follow-up

- Push, open the PR and wait for CI to go green.
- Archive as the PR's last commit, wait for CI to be green again, then squash-merge.
- Send the status report.
