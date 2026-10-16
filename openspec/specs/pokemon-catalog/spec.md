# pokemon-catalog Specification

## Purpose
Lets anyone browse the Pokémon catalog page by page, with each Pokémon's sprite, category, weight and abilities. The data comes from PokeAPI, and the service stays fast and well-behaved even when PokeAPI is slow or down.

## Requirements

### Requirement: Paginated catalog
`GET /api/v1/pokemon` SHALL be reachable without credentials. It SHALL return the catalog in PokeAPI's order, with the envelope `content`, `page`, `size`, `totalElements` and `totalPages`. `page` is zero-based and defaults to 0; `size` defaults to 20. `totalElements` is the number of entries PokeAPI reports, and `totalPages` is that number divided by `size`, rounded up.

#### Scenario: First page
- **WHEN** an anonymous client calls `GET /api/v1/pokemon?page=0&size=3` and PokeAPI reports 1351 entries
- **THEN** the response status is 200
- **AND** `content` holds the first 3 entries in order (`bulbasaur`, `ivysaur`, `venusaur`)
- **AND** `page` is 0, `size` is 3, `totalElements` is 1351 and `totalPages` is 451

#### Scenario: Default paging
- **WHEN** a client calls `GET /api/v1/pokemon` without parameters
- **THEN** the response has `page` 0 and `size` 20, with up to 20 entries

### Requirement: Catalog entry contents
Each catalog entry SHALL contain these members:
- `id`: the Pokédex number
- `name`
- `spriteUrl`: the default front sprite, or `null` when PokeAPI has none
- `category`: the English genus, or `null` when PokeAPI has none
- `weight`: in kilograms with one decimal, converted from PokeAPI's hectograms
- `abilities`: ability names in PokeAPI's slot order, hidden abilities included

#### Scenario: Bulbasaur
- **WHEN** the catalog includes Pokémon #1
- **THEN** its entry has `id` 1, `name` `bulbasaur`, `spriteUrl` ending in `/sprites/pokemon/1.png`, `category` `Seed Pokémon`, `weight` 6.9 and `abilities` `["overgrow", "chlorophyll"]`

#### Scenario: Missing English genus or sprite
- **WHEN** PokeAPI provides no English genus or no default sprite for an entry
- **THEN** that entry's `category` or `spriteUrl` is `null`, and the rest of the page is unaffected

### Requirement: Paging parameter validation
`page` SHALL be a whole number of at least 0, and `size` a whole number from 1 to 100. Any other value SHALL be rejected with 400, and `errors[]` SHALL contain one entry naming each offending parameter.

#### Scenario: Size above the maximum
- **WHEN** a client calls `GET /api/v1/pokemon?size=101`
- **THEN** the response status is 400 and `errors` contains an entry for `size`

#### Scenario: Negative values
- **WHEN** a client calls `GET /api/v1/pokemon?page=-1&size=-5`
- **THEN** the response status is 400 and `errors` contains entries for `page` and `size`

#### Scenario: Not a number
- **WHEN** a client calls `GET /api/v1/pokemon?page=abc`
- **THEN** the response status is 400 and `errors` contains an entry for `page`

### Requirement: Pages beyond the end
A page that starts after the last entry SHALL return 200 with an empty `content`, while still reporting `totalElements` and `totalPages`.

#### Scenario: Past the last page
- **WHEN** PokeAPI reports 1351 entries and a client calls `GET /api/v1/pokemon?page=500&size=20`
- **THEN** the response status is 200, `content` is empty, `totalElements` is 1351 and `totalPages` is 68

### Requirement: Upstream unavailability
When PokeAPI cannot be reached, doesn't answer within the configured read timeout, or answers with a server error, the service SHALL return 503 with a problem of type `service-unavailable` and a `Retry-After` header. The response SHALL NOT contain the upstream error, its URL or a stack trace. The failure SHALL be logged with the correlation id.

#### Scenario: PokeAPI down
- **WHEN** PokeAPI refuses connections and a client calls `GET /api/v1/pokemon`
- **THEN** the response status is 503, the problem type is `service-unavailable`, and a `Retry-After` header is present

#### Scenario: PokeAPI too slow
- **WHEN** PokeAPI takes longer than the configured read timeout to answer
- **THEN** the response status is 503 within roughly that timeout, rather than hanging

#### Scenario: PokeAPI server error
- **WHEN** PokeAPI answers a catalog request with 500
- **THEN** the response status is 503 and the body contains no part of the upstream response

### Requirement: Parallel, bounded detail fetching
The service SHALL fetch the per-entry details of a page in parallel. Each instance SHALL never have more PokeAPI detail fetches in flight than the configured maximum (default 8). Log lines written by the parallel work SHALL carry the request's correlation id.

#### Scenario: Faster than sequential
- **WHEN** every PokeAPI detail response takes 300 ms and a client requests a page of 10 entries
- **THEN** the response arrives in clearly less time than fetching the 20 detail resources one after another would take (6 s)

#### Scenario: Concurrency is capped
- **WHEN** more entries are requested than the configured maximum
- **THEN** at no point are more detail fetches in flight than that maximum

### Requirement: Polite upstream client
Every request to PokeAPI SHALL carry a `User-Agent` that identifies this service, and SHALL use the configured base URL, connect timeout and read timeout.

#### Scenario: Identifying header
- **WHEN** the service calls PokeAPI
- **THEN** the request carries a `User-Agent` header starting with `pokedex-api`

### Requirement: Search by part of a name
`GET /api/v1/pokemon` SHALL accept an optional `name` and return only the Pokémon whose name contains it, in catalog order, with the same envelope, paging and entry contents. `name` is trimmed and lower-cased, and spaces become hyphens; it SHALL then be 3 to 50 letters, digits or hyphens, and anything else SHALL answer 400 with `errors[name]`. A blank `name` SHALL mean no filter. PokeAPI's name list SHALL be cached like catalog pages.

#### Scenario: Part of a name
- **WHEN** a client calls `GET /api/v1/pokemon?name=PIKA` and PokeAPI lists 17 names containing `pika`
- **THEN** the response status is 200, `totalElements` is 17, and `content` starts with `pikachu` (id 25) with its category, weight and abilities

#### Scenario: Spaces in the name
- **WHEN** a client calls `GET /api/v1/pokemon?name=mr%20mime`
- **THEN** `content` holds `mr-mime` and `mr-mime-galar`

#### Scenario: Paging the matches
- **WHEN** 23 names contain `chu` and a client calls `GET /api/v1/pokemon?name=chu&size=5&page=4`
- **THEN** `totalElements` is 23, `totalPages` is 5, and `content` holds the last 3 matches

#### Scenario: Nothing matches
- **WHEN** a client calls `GET /api/v1/pokemon?name=zzz`
- **THEN** the response status is 200, `content` is empty and `totalElements` is 0

#### Scenario: Too short
- **WHEN** a client calls `GET /api/v1/pokemon?name=pi`
- **THEN** the response status is 400 and `errors` contains an entry for `name`

#### Scenario: Name list cached
- **WHEN** two searches run within the cache TTL
- **THEN** PokeAPI's name list is requested once
