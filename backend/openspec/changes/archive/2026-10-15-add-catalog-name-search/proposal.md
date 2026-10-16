# Proposal: add-catalog-name-search

## Why

People look for a Pokémon by the part of its name they remember: "pika" for Pikachu, "saur" for the Bulbasaur line. The catalog can only be paged, and the detail endpoint needs the exact name or number. Neither PokeAPI nor this API offers a search, so the web app can't offer one either.

## What Changes

- **A `name` filter on the catalog:** `GET /api/v1/pokemon?name=pika` returns only the Pokémon whose name contains `pika`, ignoring case, in the catalog's order, with the usual envelope and paging (`page`, `size`, `totalElements`, `totalPages`). Each entry has the same contents as in the catalog.
- **The input:** `name` is trimmed and lower-cased, and spaces become hyphens (`mr mime` → `mr-mime`). It must then be **3 to 50** letters, digits or hyphens; anything else answers 400 with `errors[name]`. A blank `name` means no filter, so the endpoint behaves as today.
- **Where the names come from:** PokeAPI's full name list (`/pokemon?limit=…`, one request), cached like the catalog pages. Only the entries on the requested page are then fetched in detail, through the same bounded parallel fan-out, cache and resilience as the catalog.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `pokemon-catalog`: adds "Search by part of a name".

## Out of Scope

- **Localized names** (the local replica's `localizedNames`) and fuzzy matching or typo tolerance: matching is containment of the PokeAPI name.
- **Ranking:** results stay in Pokédex order. For the names people type, the main forms come first anyway, for example `pikachu` before `pikachu-rock-star`.

## Impact

- **Domain:** a new `PokemonNameQuery` value object and a `CatalogName` record.
- **Application:** `PokemonCatalogPort` gains `findAllNames()` and `findSummaries(ids)`, and `ListPokemon` gains `search(query, page)`.
- **Adapters:** `PokeApiCatalogAdapter` implements the two port methods, and `CachingPokemonCatalog` caches them, the name list in a new `pokemon-names` cache.
- **Web:** `PokemonController` accepts `name`.
- **API documentation:** the README and the OpenAPI annotations.
- **The web app:** a later pokedex-web change will use this.
