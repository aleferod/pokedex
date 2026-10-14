# Proposal: add-local-crud

## Why

User Story 04 (local data modification): "enable update operations for any Pokémon currently stored within the local database", with robust validation: 404 for missing records, 400 for malformed payloads, "and further defensive logic as required". The brief also asks for "comprehensive CRUD operations" with standard HTTP verbs. US03 created the local copy and its proprietary fields. This change lets people list, create, change and delete local records safely.

## What Changes

- **List:** `GET /api/v1/local-pokemon?page&size&tag&name` returns the paginated envelope ordered by PokeAPI number. `tag` filters by an exact internal tag (case-insensitive); `name` filters by a case-insensitive substring of the Pokémon name.
- **Create:** `POST /api/v1/local-pokemon` with `pokeapiId` plus optional proprietary fields.
  - The upstream fields are fetched from PokeAPI.
  - Success returns 201 with `Location` and `ETag`.
  - A duplicate `pokeapiId` returns 409; an unknown `pokeapiId` returns 400 naming the field.
- **Replace:** `PUT /api/v1/local-pokemon/{id}` replaces all proprietary fields; omitted ones are cleared. It returns 200 with the new `ETag`.
- **Partial update:** `PATCH /api/v1/local-pokemon/{id}` follows JSON Merge Patch (RFC 7396). Absent members are kept, `null` clears, and `localizedNames` merges per locale.
- **Delete:** `DELETE /api/v1/local-pokemon/{id}` returns 204 and needs the `ADMIN` role, which was already enforced by `access-control`.
- **Optimistic concurrency:**
  - `PUT` and `PATCH` require `If-Match` with the record's `ETag`; a missing header returns **428**.
  - A version mismatch returns **409** (the brief: "stale update returns 409"). `DELETE` checks `If-Match` when it's sent.
- **Defensive rules:**
  - Upstream fields (`name`, `weight`, `stats`, `spriteUrl`, …) and identity or audit fields are **read-only**. Sending them returns 400 naming each such field.
  - Proprietary text is trimmed and length-limited, tags are pattern- and count-limited, and locales are validated. **Every** violation is reported in one 400.
  - Malformed JSON, wrong types and unknown members return 400 with field-level errors, as `api-errors` already guarantees.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `local-pokemon`: adds requirements for listing and filtering, creating, replacing, patching and deleting local records, optimistic concurrency with `If-Match`, read-only upstream fields, and the proprietary-field rules.

## Out of Scope

- Bulk edits, soft delete with restore, history or audit trail of changes, full-text search across localized names.
- Changing upstream fields locally. They come from PokeAPI only, through sync.

## Impact

- **Serves:** US04, and the "comprehensive CRUD" API requirement.
- **Code:**
  - `domain.localpokemon`: `withProprietary`, `ProprietaryPatch`, a multi-field validation error
  - `application`: list, create, replace, patch and delete use cases, plus repository port additions
  - persistence: a filtered paged query using the GIN tag index, delete with a version check, batch fetching of localized names
  - web: CRUD endpoints, `If-Match` handling, read-only field errors
- **Database:** no new migration, because V3 already has the version column and the tag index.
- **Dependencies:** none new.
