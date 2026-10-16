# Spec Delta: pokemon-catalog

## ADDED Requirements

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
