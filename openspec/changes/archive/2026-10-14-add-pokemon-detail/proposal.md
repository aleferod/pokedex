# Proposal: add-pokemon-detail

## Why

User Story 02 (detailed view): "access comprehensive data for a chosen Pokémon, specifically viewing its image, core statistics, narrative description, and evolutionary lineage." The catalog (US01) shows a summary; this is the page a user opens from it. Its data spans three PokeAPI resources: `/pokemon`, `/pokemon-species` and `/evolution-chain`. Two of those need real translation work: flavor texts contain raw control characters, and evolution chains are trees with machine-oriented trigger data.

## What Changes

- **New public endpoint** `GET /api/v1/pokemon/{idOrName}`. The identifier is a Pokédex number or a name; names are case-insensitive. It returns:
  - `id`, `name`, `imageUrl` (official artwork), `category`, `description` (English, cleaned)
  - `types`, `height` (m), `weight` (kg), `abilities`
  - `stats`: HP, attack, defense, special attack, special defense, speed
  - `evolutionChain`: a tree from the base form, with branching and a readable evolution condition on each stage
- **Errors:**
  - 404 `not-found` for an unknown Pokémon
  - 400 for a malformed identifier
  - 503 when PokeAPI is unavailable, same behaviour as the catalog
- **Domain:**
  - `PokemonIdentifier`: an id or a normalized name
  - `Height` (decimetres → metres)
  - `BaseStats`, `EvolutionStage` (recursive tree), `PokemonDetail`
  - `PokemonNotFoundException`
- **Port and use case:** `GetPokemonDetail`, plus `PokemonCatalogPort.findDetail`.
- **Adapter:**
  - The PokeAPI client tells "not found" apart from "unavailable".
  - Detail DTOs and mapping (anti-corruption layer).
  - Flavor-text cleaning.
  - **Strategy:** one `EvolutionConditionStrategy` per PokeAPI evolution trigger (level-up, use-item, trade, others) turns raw evolution details into conditions such as "Level 16" or "Use Water Stone".

## Capabilities

### New Capabilities
- `pokemon-detail`: the detailed view of one Pokémon, covering identifier rules, content (artwork, stats, types, size, description), the evolution chain with conditions, and error answers.

### Modified Capabilities
- None. The 503 behaviour reuses the problem type and the `Retry-After` header already specified by `pokemon-catalog`.

## Out of Scope

- **Caching and resilience** (`add-caching-resilience`). A detail costs 3 sequential upstream calls until then.
- **Alternate forms and varieties** (for example Mega Evolutions or regional forms) beyond what `/pokemon/{idOrName}` returns directly. Moves, locations, cries and localized descriptions are also out.

## Impact

- **Serves:** US02.
- **Code:** `domain.pokemon`, `application.port.in/out.pokemon`, `application.usecase.pokemon`, `adapter.out.pokeapi` (client, DTOs, mapper, `evolution` strategies), `adapter.in.web.pokemon`, and `GlobalExceptionHandler` (404 mapping).
- **Fixtures:** new real, trimmed captures: Eevee (8-branch chain), the Pikachu chain (Pichu → Pikachu → Raichu), Bulbasaur's chain, and Ditto (no evolutions).
- **Dependencies or configuration:** none new.
