# Spec Delta: upstream-caching

## ADDED Requirements

### Requirement: Cached catalog pages
The service SHALL keep each catalog page it has built in an in-memory cache, keyed by `page` and `size`. A repeated request for the same page within the time to live SHALL be answered without calling PokeAPI. The time to live (default 1 hour) and the maximum number of entries per cache (default 2000) SHALL be configurable.

#### Scenario: Second visit is served from the cache
- **WHEN** a client requests `GET /api/v1/pokemon?page=0&size=3` twice
- **THEN** both responses are equal, and PokeAPI received the list request and each entry's requests exactly once

### Requirement: Cached details under number and name
The service SHALL cache each Pokémon detail it has loaded, and SHALL make it reachable by both the Pokémon's number and its name. A Pokémon that PokeAPI doesn't know SHALL be remembered as unknown for the same time to live.

#### Scenario: Name then number
- **WHEN** a client requests `GET /api/v1/pokemon/pikachu` and then `GET /api/v1/pokemon/25`
- **THEN** both answer 200 with the same body, and PokeAPI received one request for the Pokémon, one for its species and one for its evolution chain

#### Scenario: Unknown Pokémon asked twice
- **WHEN** a client requests `GET /api/v1/pokemon/missingno` twice
- **THEN** both answer 404, and PokeAPI received one request

### Requirement: Failures are never cached
A failed load (PokeAPI unreachable, too slow or failing) SHALL NOT be cached, so the next request tries again.

#### Scenario: Recovery after an outage
- **WHEN** a page request gets 503 because PokeAPI fails, and PokeAPI then recovers
- **THEN** the next request for that page answers 200 with data from PokeAPI

### Requirement: One load per entry under concurrency
Simultaneous requests for an entry that isn't cached yet SHALL trigger a single upstream load, whose result all of them receive.

#### Scenario: Two first visits at once
- **WHEN** two clients request the same uncached page at the same moment while PokeAPI is slow
- **THEN** both answer 200 with the same body, and PokeAPI received the list request once

### Requirement: Fresh reads for the local replica
Importing, re-syncing or creating a local record SHALL read PokeAPI directly instead of the cache, and SHALL then store what it read in the detail cache. The catalog detail then shows the same data as the local record.

#### Scenario: Re-sync after PokeAPI changed
- **WHEN** Pikachu's detail is cached with weight 6.0 kg, PokeAPI now reports 6.1 kg, and a user re-syncs Pikachu
- **THEN** the local record has weight 6.1, and a following `GET /api/v1/pokemon/25` also shows 6.1
