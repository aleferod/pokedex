# local-pokemon Specification

## Purpose
Keeps a locally owned copy of selected Pokémon in the service's own database. It's synchronized from PokeAPI and extended with proprietary fields that PokeAPI doesn't have: localized names, region, habitat notes, internal tags and notes. Authenticated users list, create, edit and delete these records; edits are guarded by optimistic concurrency (`ETag`/`If-Match`) and never touch the PokeAPI-owned fields.

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

### Requirement: List local records
`GET /api/v1/local-pokemon` SHALL return local records in the paginated envelope (`content`, `page`, `size`, `totalElements`, `totalPages`), ordered by `pokeapiId`, with the same `page` and `size` rules as the catalog. It SHALL accept two optional filters, which combine:
- `tag`: records whose internal tags include the value, ignoring case
- `name`: records whose name contains the value, ignoring case

#### Scenario: Filter by tag
- **WHEN** Bulbasaur is tagged `starter` and Pikachu is not, and a user calls `GET /api/v1/local-pokemon?tag=STARTER`
- **THEN** the response status is 200, `content` contains Bulbasaur only, and `totalElements` is 1

#### Scenario: Filter by name
- **WHEN** a user calls `GET /api/v1/local-pokemon?name=SAUR`
- **THEN** `content` contains every stored record whose name contains `saur` (for example `bulbasaur`, `ivysaur`, `venusaur`) in `pokeapiId` order

#### Scenario: Invalid paging
- **WHEN** a user calls `GET /api/v1/local-pokemon?size=101`
- **THEN** the response status is 400 and `errors` contains an entry for `size`

### Requirement: Create a local record
`POST /api/v1/local-pokemon` with a `pokeapiId` and optional proprietary fields SHALL fetch the upstream fields from PokeAPI and store a new record. It SHALL answer 201 with a `Location` header, an `ETag` and the record; `createdBy` is the caller.

#### Scenario: Create Bulbasaur with proprietary data
- **WHEN** a user posts `{"pokeapiId": 1, "region": "Kanto", "internalTags": ["starter"], "localizedNames": {"pt-BR": "Bulbassauro"}}`
- **THEN** the response status is 201 and the body has `name` `bulbasaur`, `region` `Kanto`, `internalTags` `["starter"]` and `localizedNames` `{"pt-BR": "Bulbassauro"}`

#### Scenario: Duplicate PokeAPI number
- **WHEN** a record with `pokeapiId` 1 exists and a user posts `{"pokeapiId": 1}`
- **THEN** the response status is 409, the problem type is `conflict`, and `errors` contains an entry for `pokeapiId`

#### Scenario: Unknown PokeAPI number
- **WHEN** a user posts `{"pokeapiId": 99999}` and PokeAPI has no such Pokémon
- **THEN** the response status is 400 and `errors` contains an entry for `pokeapiId`

#### Scenario: Missing PokeAPI number
- **WHEN** a user posts `{}`
- **THEN** the response status is 400 and `errors` contains an entry for `pokeapiId`

### Requirement: Replace proprietary fields
`PUT /api/v1/local-pokemon/{id}` SHALL replace all proprietary fields with the ones in the body. Proprietary fields left out SHALL be cleared. It SHALL answer 200 with the updated record and its new `ETag`, and it SHALL advance `updatedAt` while leaving `syncedAt` and the upstream fields unchanged.

#### Scenario: Replace
- **WHEN** Pikachu has `region` `Kanto` and `notes` `old`, and a user sends `PUT` with `If-Match` set to its current `ETag` and body `{"region": "Johto"}`
- **THEN** the response status is 200, `region` is `Johto`, `notes` is `null`, and `ETag` shows a higher version

### Requirement: Partially update proprietary fields
`PATCH /api/v1/local-pokemon/{id}` SHALL apply JSON Merge Patch (RFC 7396) to the proprietary fields:
- a member that is absent keeps its value
- a member set to `null` is cleared
- a member with a value replaces the current one
- `localizedNames` is merged per locale, and a locale set to `null` is removed

It SHALL accept `application/merge-patch+json` and `application/json`.

#### Scenario: Patch one field
- **WHEN** Pikachu has `region` `Kanto` and `notes` `old`, and a user sends `PATCH` with a current `If-Match` and body `{"notes": "Loves ketchup"}`
- **THEN** the response status is 200, `notes` is `Loves ketchup` and `region` is still `Kanto`

#### Scenario: Merge localized names
- **WHEN** Pikachu has `localizedNames` `{"pt-BR": "Pikachu", "ja": "ピカチュウ"}` and the patch is `{"localizedNames": {"fr": "Pikachu", "ja": null}}`
- **THEN** `localizedNames` becomes `{"fr": "Pikachu", "pt-BR": "Pikachu"}`

### Requirement: Optimistic concurrency with If-Match
`PUT` and `PATCH` SHALL require an `If-Match` header holding the record's current `ETag`. A missing header SHALL answer 428 `precondition-required`. A header for an older version SHALL answer 409 `conflict` and change nothing. `DELETE` SHALL apply the same check when `If-Match` is sent.

#### Scenario: Missing If-Match
- **WHEN** a user sends `PUT /api/v1/local-pokemon/{id}` without `If-Match`
- **THEN** the response status is 428 and the record is unchanged

#### Scenario: Stale ETag
- **WHEN** two users read Pikachu at `ETag` `"3"`, the first saves a `PATCH` successfully, and the second then sends a `PATCH` with `If-Match: "3"`
- **THEN** the second response status is 409, the problem type is `conflict`, and the first user's change is kept

### Requirement: Upstream fields are read-only
The bodies of `POST`, `PUT` and `PATCH` SHALL NOT change upstream, identity or audit fields:
- upstream: `name`, `spriteUrl`, `imageUrl`, `category`, `types`, `height`, `weight`, `abilities`, `stats`
- identity and audit: `id`, `createdAt`, `updatedAt`, `syncedAt`, `createdBy`, `version`, and `pokeapiId` outside `POST`

A body containing any of them SHALL answer 400, with an `errors[]` entry per such field stating that it is read-only.

#### Scenario: Try to change the weight
- **WHEN** a user sends `PATCH` with a current `If-Match` and body `{"weight": 999, "notes": "heavy"}`
- **THEN** the response status is 400, `errors` contains an entry for `weight` whose message says it is read-only, and nothing changes

### Requirement: Proprietary field rules
Proprietary fields SHALL follow these rules:
- `region` at most 50 characters, `habitatNotes` at most 500, `notes` at most 2000; text is trimmed, and blank text becomes `null`
- `internalTags`: at most 20 tags, each 1–30 lower-case letters, digits or hyphens, starting with a letter or digit; tags are lower-cased and de-duplicated
- `localizedNames`: at most 10 locales such as `pt-BR`, names 1–50 characters

A request breaking several rules SHALL get one 400 listing every offending field.

#### Scenario: Several invalid fields at once
- **WHEN** a user sends `PATCH` with a current `If-Match` and body `{"region": "<51 characters>", "internalTags": ["Bad Tag!"]}`
- **THEN** the response status is 400 and `errors` contains entries for `region` and `internalTags`

#### Scenario: Normalised input
- **WHEN** a user sends `PATCH` with `{"region": "  Kanto  ", "internalTags": ["Starter", "starter", " KANTO "]}`
- **THEN** `region` is `Kanto` and `internalTags` is `["starter", "kanto"]`

### Requirement: Delete a local record
`DELETE /api/v1/local-pokemon/{id}` SHALL remove the record and answer 204 with no body. It requires the `ADMIN` role. An unknown id SHALL answer 404.

#### Scenario: Admin deletes
- **WHEN** an `ADMIN` sends `DELETE /api/v1/local-pokemon/{id}` for an existing record
- **THEN** the response status is 204, and a following `GET` of that id answers 404

#### Scenario: Unknown id
- **WHEN** an `ADMIN` sends `DELETE` for an unused UUID
- **THEN** the response status is 404
