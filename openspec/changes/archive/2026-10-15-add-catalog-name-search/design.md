# Design: add-catalog-name-search

## Context

**How the catalog works now:** PokeAPI's `/pokemon?offset&limit` returns names and URLs, and the adapter fans out to `/pokemon/{id}` and `/pokemon-species/{id}` for each entry of the page, with a bound of 8 in flight. `CachingPokemonCatalog` (Caffeine) caches whole pages and details, and every PokeAPI call goes through retry and a circuit breaker.

**What a search can use:** PokeAPI has no search, but `/pokemon?offset=0&limit=100000` returns all 1351 names and URLs in one response (about 90 KB, captured 2026-10-15). In that capture:
- every name matches `[a-z0-9-]+`, and the longest has 27 characters
- `pika` matches 17 names, `saur` 5, `mime` 3 and `chu` 23

## Goals / Non-Goals

**Goals:**
- Containment matching that's simple to explain and predictable.
- A search page costs what a catalog page costs: the name list is cached, and only the entries of the requested page are fetched in detail.
- The matching rule lives in the domain and application layers, not in the PokeAPI adapter.

**Non-Goals:**
- Fuzzy matching, ranking, and searching localized names.

## Decisions

### D1. The query as a value object
`PokemonNameQuery.parse(raw)` returns an `Optional` of a query.
- **Blank or missing:** empty, which means no filter.
- **Normalisation:** trim, lower-case, and turn each run of spaces into one hyphen.
- **Validation:** the result must match `[a-z0-9-]{3,50}`. Otherwise it throws `InvalidFieldException("name", "must be 3 to 50 letters, digits, hyphens or spaces")`, which the existing handler turns into a 400 with `errors[name]`.
- **Matching:** `matches(name)` is `name.contains(value)`.

Three characters keep the matches meaningful: a single letter matches most of the catalog.

### D2. Who does what
- **The port** gains two methods:
  - `List<CatalogName> findAllNames()`: every Pokémon's id and name, in catalog order
  - `List<PokemonSummary> findSummaries(List<PokemonId> ids)`: summaries in the given order, through the bounded fan-out
- **The use case:** `ListPokemonUseCase.search(query, request)` filters the names, counts the matches, slices the requested page, and asks the port for those summaries only. An empty slice makes no call. `Page.of(summaries, request, matches)` gives the same envelope as the catalog.
- **Not a `findPage(request, filter)` on the adapter:** that would put the rule in the PokeAPI adapter.

### D3. Caching
`CachingPokemonCatalog` decorates both methods:
- `findAllNames()` is cached under one key in a new `pokemon-names` cache, with the catalog's TTL and size settings.
- `findSummaries(ids)` is cached in `pokemon-pages`, keyed by the list of ids. The same query and page give the same ids, so a repeated search is a cache hit.

Like the existing cache entries, these are per instance and up to one TTL old (ADR-0003).

### D4. Fetching the name list
`PokeApiClient.listPokemon(0, 100_000)` makes one request. The limit is far above PokeAPI's count (1351), and PokeAPI caps nothing below it. IDs come from the entry URLs, as for pages. Failures map to `CatalogUnavailableException` (503), as for pages.

### D5. The endpoint
`GET /api/v1/pokemon` gains an optional `@RequestParam name`. With a query, the controller calls `search`; without one, `list`, as today. The page and size rules are unchanged. OpenAPI documents the new parameter, the 400 for it, and the matching rule.

### D6. Tests
| Test | What it covers |
|---|---|
| `PokemonNameQueryTest` | blank → no filter; trim, case, spaces → hyphen; too short, too long and invalid characters with the message; `matches` |
| `ListPokemonUseCaseTest` | catalog order, counting, page slicing, only the page's ids sent to the port, no call for an empty page or no match |
| `PokeApiCatalogAdapterTest` | the full name list (the real capture) mapped to ids and names; `findSummaries` in order, through the fan-out; 503 on failure |
| `CachingPokemonCatalogTest` | names loaded once; summaries cached by ids |
| `PokemonControllerTest` | `name` routes to `search`; `pi` and `pika!` → 400 `errors[name]`; blank → `list` |
| `PokemonCatalogIT` | `?name=PIKA` → 17 matches starting with `pikachu`, with details; `?name=mr%20mime` → 2; `?name=zzz` → empty; `?name=chu&size=5` → 5 pages (WireMock serving the real list) |

## Risks / Trade-offs

- [A cold search makes one larger request (about 90 KB)] → it's cached for the TTL, and pages then cost the same as catalog pages.
- [The capture goes stale as PokeAPI grows] → tests use the captured list; the code doesn't depend on its size.
- [Hyphenated forms such as `pikachu-rock-star` appear among the results] → they're real Pokémon in the catalog too, and Pokédex order puts the main form first.
