# Tasks: add-local-crud

TDD convention as in earlier changes: each `test:` commit is red on assertions, and the `feat:` commit makes it green.

## 1. Domain

- [x] 1.1 Write `ProprietaryPatchTest`, `ProprietaryDataTest` additions (`of` collects every violation into `InvalidFieldsException`) and `LocalPokemonTest` additions (`withProprietary` advances `updatedAt` only; `requireVersion`) (D2). Verify: red against stubs.
- [x] 1.2 Implement `InvalidFieldsException`, `ProprietaryData.of`, `FieldPatch`, `ProprietaryPatch`, `LocalizedNamesPatch`, `LocalPokemon.withProprietary` and `requireVersion`. Verify: 1.1 green; ArchUnit green.

## 2. Application

- [x] 2.1 Write tests for the five use cases (D3):
  - list (normalized tag)
  - create (validation first; duplicate before PokeAPI; unknown number → `InvalidFieldException(pokeapiId)`; unavailable)
  - replace/patch (404; stale; rules)
  - delete (404; stale; no version)
  - Verify: red.
- [x] 2.2 Implement the ports (`ListLocalPokemon`, `CreateLocalPokemon`, `ReplaceLocalPokemon`, `PatchLocalPokemon`, `DeleteLocalPokemon`, `LocalPokemonFilter`, the repository port additions) and the use cases. Verify: 2.1 green; coverage ≥ 85 %.

## 3. Persistence

- [x] 3.1 Extend `LocalPokemonPersistenceAdapterIT`:
  - tag filter (case-insensitive) and name filter, including `%`/`_` escaping
  - combined filters
  - paging totals and order
  - `existsByPokeapiId`
  - delete; stale delete
  - Verify: red.
- [x] 3.2 Implement the native filtered page query with a count query, `@BatchSize` on localized names, `existsByPokeapiId` and the version-checked delete (D4). Verify: 3.1 green.

## 4. Web

- [x] 4.1 Extend `LocalPokemonControllerTest`:
  - list (filters, envelope, 400 paging)
  - create (201, `Location`, `ETag`; 409 `errors[pokeapiId]`; 400 unknown number; 400 missing `pokeapiId`; 400 read-only `weight`)
  - replace (200 new `ETag`; 428 without `If-Match`; 409 stale; read-only)
  - patch (merge-patch content type; keep/clear/set; localized merge; read-only lists every field; several invalid fields at once; 428; 409)
  - delete (204; 404; stale 409 with `If-Match`)
  - `If-Match` parsing (`"3"`, `W/"3"`, `*`, garbage → 400)
  - Verify: red.
- [x] 4.2 Implement the endpoints, request DTOs with `@ReadOnlyFields`, `MergePatchReader`, the `IfMatch` helper and `PreconditionRequiredException`, the `GlobalExceptionHandler` additions, and the wiring (D5). Verify: 4.1 green; earlier tests green.

## 5. End-to-end and docs

- [x] 5.1 Write `LocalPokemonCrudIT` (Testcontainers + WireMock):
  - create Bulbasaur → list `?tag=starter` → patch notes → stale patch with the old `ETag` (409) → replace → USER delete (403) → ADMIN delete (204) → GET (404)
  - Verify: green with full `verify`.
- [x] 5.2 Update the README:
  - US04 rows ✅, the CRUD section (verbs, `If-Match`/`ETag` flow, 409/428, merge patch, read-only fields, rules) and the 409-vs-412 note
  - patterns: Notification, Merge Patch, Optimistic Offline Lock
  - update `docs/problems.md` for 409, 428 and 412
  - Verify: lychee offline shows no broken links.

## 6. Integration verification

- [x] 6.1 `./scripts/verify-in-docker.sh` from a clean `target/`. Verify: exit 0; record counts and coverage.
- [x] 6.2 From a fresh clone with `docker compose up --build` against the real PokeAPI, run the curl flow above with a registered USER. The ADMIN delete waits for the seeded admin in `add-seed-data`, so for now USER delete → 403. Verify: as in the spec; `docker compose down -v`.
- [x] 6.3 Run `openspec validate add-local-crud --strict` and gitleaks. Verify: valid, no leaks.

## Workflow follow-up

- Push, open the PR and wait for CI to go green.
- Archive as the PR's last commit, wait for CI to be green again, then squash-merge.
- Send the status report.
