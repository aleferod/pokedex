# Spec Delta: local-pokemon

## ADDED Requirements

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
