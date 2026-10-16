# Proposal: add-pokemon-catalog

## Why

User Story 01 (Pokémon enumeration): "browse Pokémon via paginated results, displaying each entry's sprite, category, mass and a collection of their skills." It's the API's first business feature, and the public entry point the frontend's catalog grid is built on.

PokeAPI's list endpoint only returns names and URLs. Each entry's sprite, weight and abilities come from `/pokemon/{id}`, and its category (the genus) comes from `/pokemon-species/{id}`. So one page of 20 costs 1 + 2 × 20 upstream calls. Making those calls one after another would be slow, which is why the brief asks for bounded parallel fetching.

## What Changes

- **New public endpoint** `GET /api/v1/pokemon?page=0&size=20` returns `{ content[], page, size, totalElements, totalPages }`. Each item has `id`, `name`, `spriteUrl`, `category` (English genus), `weight` (kg, converted from hectograms) and `abilities[]`.
- **Validation:** `page ≥ 0`, `1 ≤ size ≤ 100`, and numeric parameters. Violations return 400 with one `errors[]` entry per parameter. A page past the end returns 200 with empty `content`.
- **PokeAPI outages:** when PokeAPI is unreachable, slow (past the read timeout) or failing (5xx), the API returns 503 `service-unavailable` with a `Retry-After` header and no internals.
- **Parallel fetching:** each page's details are fetched in parallel, with an instance-wide cap on in-flight PokeAPI requests (configurable, default 8). The correlation id is carried into the parallel work.
- **Well-behaved PokeAPI client:** configurable base URL, connect and read timeouts, and an identifying `User-Agent`.
- **Domain:**
  - value objects `PokemonId` and `Weight`
  - `PokemonSummary`
  - `PageRequest`/`Page`, reusable by the local CRUD later
- **Port and adapter:** the `PokemonCatalogPort`, and a PokeAPI adapter acting as an anti-corruption layer: PokeAPI DTOs never leave `adapter.out.pokeapi`.

## Capabilities

### New Capabilities
- `pokemon-catalog`: browsing the Pokémon catalog page by page. It covers item contents, paging rules, behaviour when the upstream catalog is unavailable, and performance and politeness towards the upstream source.

### Modified Capabilities
- None.

## Out of Scope

- **Caching, retry with backoff, and the circuit breaker.** They're part of `add-caching-resilience` (the next-but-three change in the agreed order). That change adds the "repeated calls are served from cache" scenarios to this capability, proven with WireMock request counts. This change only adds timeouts and the 503 mapping.
- **Detail view** (stats, description, evolution chain): `add-pokemon-detail` (US02).
- **Search and filtering** by name or type.

## Impact

- **Serves:** US01, and the PokeAPI integration requirement.
- **Code:**
  - `domain.pokemon`, `domain.shared` (paging)
  - `application.port.in/out.pokemon`, `application.usecase.pokemon`
  - `adapter.out.pokeapi`: client, DTOs, anti-corruption mapper, bounded fan-out
  - `adapter.in.web.pokemon`
  - `infrastructure.pokeapi`
  - `GlobalExceptionHandler`: 503 mapping and parameter errors
- **Dependencies:** none new at runtime, since `RestClient` is in Spring Web. Test-only: WireMock 3.13 (standalone).
- **Configuration:** `POKEAPI_BASE_URL`, plus optional timeouts and max concurrency.
- **External system:** PokeAPI (`https://pokeapi.co/api/v2`), read-only, public and without authentication.
