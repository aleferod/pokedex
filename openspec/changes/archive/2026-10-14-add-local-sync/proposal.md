# Proposal: add-local-sync

## Why

User Story 03 (data synchronization): "develop a mechanism to persist Pokémon data into a local relational store", so that proprietary fields can be added: localized names, geographic metadata and internal classification tags. PokeAPI is read-only, so anything the business wants to add or edit needs a local copy that it owns. Change US04 (`add-local-crud`) then builds the full editing API on top of this copy.

## What Changes

- **New table `local_pokemon`** (Flyway `V3`). Each local Pokémon holds:
  - its own UUID and a **unique PokeAPI id**
  - a snapshot of the upstream fields: name, sprite, artwork, category, types, height, weight, abilities, base stats
  - **proprietary fields**: localized names per locale (in table `local_pokemon_localized_name`), region, habitat notes, internal tags, notes
  - **audit columns**: `created_at`, `updated_at`, `synced_at`, `created_by`
  - a `version` for optimistic locking, also exposed as an `ETag`
- **`POST /api/v1/local-pokemon/sync/{idOrName}`** (authenticated) imports from PokeAPI.
  - The **first import returns 201** with `Location` and `ETag`.
  - A **re-sync returns 200**: it refreshes the upstream fields, advances `syncedAt`, and **never touches the proprietary fields** or the creation audit.
  - Two concurrent first syncs of the same Pokémon still produce one record.
- **`POST /api/v1/local-pokemon/sync?limit=N`** (optional bulk, authenticated) syncs the first N catalog entries, with N capped at 20. It answers with a per-entry summary.
- **`GET /api/v1/local-pokemon/{id}`** (authenticated) reads one local record, so the `Location` of a sync resolves. The rest of the CRUD comes in US04.
- **Domain:**
  - `LocalPokemon` aggregate, with factory `importFrom` and behaviour `resyncWith`
  - `UpstreamData`
  - `ProprietaryData` with value objects `LocalizedNames` and `InternalTags`: trimming, length limits, tag pattern and count. US04 exposes them for editing.
  - `PokemonDetail` gains the default sprite URL, internally only; the US02 response shape is unchanged.
- **Use cases and ports:** `SyncPokemon`, `SyncCatalogPage`, `GetLocalPokemon`, and `LocalPokemonRepositoryPort`, plus a persistence adapter with optimistic locking and unique-key translation.

## Capabilities

### New Capabilities
- `local-pokemon`: the locally owned copy of Pokémon. It covers syncing from PokeAPI (import, re-sync, bulk), what a local record contains (upstream snapshot, proprietary fields, audit, version) and reading a record. US04 extends this same capability with listing and editing.

### Modified Capabilities
- None. The routes are already protected by `access-control`, and errors follow `api-errors`.

## Out of Scope

- **Changing proprietary fields over the API**, plus list/filter, create, update, patch, delete, `If-Match` preconditions and the 409/412 behaviour on edits. All of that is `add-local-crud`.
- **Seeded local Pokémon:** `add-seed-data`.
- **Scheduled or background re-sync.**

## Impact

- **Serves:** US03. It's also the foundation for US04 and the seeds.
- **Database:** `V3__create_local_pokemon.sql`, with two tables, a unique PokeAPI id and a GIN index on tags (ready for US04 filtering).
- **Code:**
  - `domain.localpokemon`
  - `application.port.in/out.localpokemon`, `application.usecase.localpokemon`
  - `adapter.out.persistence.localpokemon`
  - `adapter.in.web.localpokemon`
  - small additions to `domain.pokemon.PokemonDetail` and `PokeApiDetailMapper`
- **Dependencies:** none new. PostgreSQL arrays are mapped natively by Hibernate 7.
