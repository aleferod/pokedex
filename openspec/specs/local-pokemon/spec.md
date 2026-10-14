# local-pokemon Specification

## Purpose
Keeps a locally owned copy of selected Pokémon in the service's own database. It's synchronized from PokeAPI and extended with proprietary fields that PokeAPI doesn't have: localized names, region, habitat notes, internal tags and notes.

## Requirements

### Requirement: First import
`POST /api/v1/local-pokemon/sync/{idOrName}` SHALL, for a Pokémon not yet stored locally, fetch it from PokeAPI and store it. It SHALL answer 201 with:
- a `Location` header pointing to `/api/v1/local-pokemon/{id}`
- an `ETag` header holding the record's version
- the stored record as the body

#### Scenario: Import Pikachu
- **WHEN** an authenticated user `ash` calls `POST /api/v1/local-pokemon/sync/pikachu` and Pikachu is not stored yet
- **THEN** the response status is 201, `Location` is `/api/v1/local-pokemon/{id}` for the new record, and `ETag` is present
- **AND** the body has a UUID `id`, `pokeapiId` 25, `name` `pikachu` and the upstream fields, with empty proprietary fields
- **AND** `createdBy` is `ash`, and `createdAt`, `updatedAt` and `syncedAt` are equal

### Requirement: Re-sync keeps proprietary data
Syncing a Pokémon that is already stored SHALL answer 200. It SHALL replace the upstream fields with PokeAPI's current values and advance `syncedAt` and `updatedAt`. It SHALL keep the proprietary fields, `createdAt` and `createdBy` unchanged, and it SHALL increase the version.

#### Scenario: Re-sync after a PokeAPI change
- **WHEN** Pikachu is stored with `region` `Kanto`, `internalTags` `["starter-line"]` and a `pt-BR` localized name, and PokeAPI now reports a different weight
- **AND** user `misty` calls `POST /api/v1/local-pokemon/sync/25`
- **THEN** the response status is 200 and `weight` shows the new value
- **AND** `region`, `internalTags`, `localizedNames`, `createdAt` and `createdBy` (`ash`) are unchanged, `syncedAt` is later, and `ETag` shows a higher version

#### Scenario: Concurrent first imports
- **WHEN** two sync requests for the same new Pokémon arrive at the same time
- **THEN** exactly one local record exists afterwards; one response is 201 and the other 200

### Requirement: Contents of a local record
A local record SHALL contain:
- **identity:** `id` (UUID) and `pokeapiId` (unique across local records)
- **upstream fields:** `name`, `spriteUrl`, `imageUrl`, `category`, `types`, `height` (m), `weight` (kg), `abilities`, `stats`
- **proprietary fields:** `localizedNames` (an object keyed by locale, for example `pt-BR`), `region`, `habitatNotes`, `internalTags` (array), `notes`
- **audit fields:** `createdAt`, `updatedAt`, `syncedAt`, `createdBy`
- `version`

#### Scenario: Freshly imported record
- **WHEN** a Pokémon is imported for the first time
- **THEN** `localizedNames` is `{}`, `internalTags` is `[]`, and `region`, `habitatNotes` and `notes` are `null`

### Requirement: Sync failures
Syncing SHALL answer:
- 404 `not-found` for an unknown Pokémon
- 400 with `errors[idOrName]` for a malformed identifier, without calling PokeAPI
- 503 `service-unavailable` with `Retry-After` when PokeAPI is unavailable

In each case, no local record SHALL be created or changed.

#### Scenario: Unknown Pokémon
- **WHEN** a user calls `POST /api/v1/local-pokemon/sync/missingno`
- **THEN** the response status is 404 and no local record exists for it

#### Scenario: PokeAPI unavailable during re-sync
- **WHEN** Pikachu is stored and PokeAPI answers with 500 during `POST /api/v1/local-pokemon/sync/pikachu`
- **THEN** the response status is 503 and the stored record is unchanged

### Requirement: Bulk sync
`POST /api/v1/local-pokemon/sync?limit=N` SHALL sync the first N catalog entries, in catalog order, with N from 1 to 20 (default 10). It SHALL answer 200 with `results`, where each entry has `pokeapiId`, `name` and `status` (`created` or `updated`), and with `created` and `updated` counts. A `limit` outside 1–20 SHALL answer 400 with `errors[limit]`.

#### Scenario: Sync the first three
- **WHEN** a user calls `POST /api/v1/local-pokemon/sync?limit=3` and none of the first three entries are stored
- **THEN** the response status is 200, `results` lists `bulbasaur`, `ivysaur` and `venusaur` with status `created`, and `created` is 3

#### Scenario: Limit too high
- **WHEN** a user calls `POST /api/v1/local-pokemon/sync?limit=21`
- **THEN** the response status is 400 and `errors` contains an entry for `limit`

### Requirement: Read one local record
`GET /api/v1/local-pokemon/{id}` SHALL answer 200 with the record and an `ETag` header holding its version. An unknown id SHALL answer 404. An id that isn't a UUID SHALL answer 400 with `errors[id]`.

#### Scenario: Follow the Location of an import
- **WHEN** a user follows the `Location` returned by a successful import
- **THEN** the response status is 200 and the body equals the body of the import response

#### Scenario: Unknown or malformed id
- **WHEN** a user calls `GET /api/v1/local-pokemon/{id}` with an unused UUID, or with `not-a-uuid`
- **THEN** the response status is 404 or 400 (with `errors[id]`) respectively

### Requirement: Authenticated access
Every `/api/v1/local-pokemon/**` endpoint in this capability SHALL require a valid bearer token. Anonymous calls SHALL answer 401.

#### Scenario: Anonymous sync
- **WHEN** an anonymous client calls `POST /api/v1/local-pokemon/sync/pikachu`
- **THEN** the response status is 401 and PokeAPI is not called
