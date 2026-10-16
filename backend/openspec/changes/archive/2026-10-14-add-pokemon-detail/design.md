# Design: add-pokemon-detail

## Context

`add-pokemon-catalog` already provides `PokemonCatalogPort`, the PokeAPI client with timeouts and `User-Agent`, `PokeApiMapper` (anti-corruption layer), lenient DTOs, `Weight`, and the 503 mapping. PokeAPI data for a detail, checked against the live API on 2026-10-14:

| Resource | Used for | Notes |
|---|---|---|
| `/pokemon/{idOrName}` | `sprites.other.official-artwork.front_default`, `types[]{slot,type.name}`, `height` (dm), `weight` (hg), `abilities`, `stats[]{base_stat, stat.name}`, `species.url` | Accepts a lower-case name or a number; 404 when unknown |
| `/pokemon-species/{id}` | `genera`, `flavor_text_entries[]{flavor_text, language, version}` in game order, `evolution_chain.url` | 28 English texts for Bulbasaur; older ones contain `\n` and `\f` |
| `/evolution-chain/{id}` | `chain{species{name,url}, evolution_details[], evolves_to[]}` | A recursive tree. `evolution_details` lists one entry per game method, for example Leafeon's 6. Each has `trigger.name` plus about 30 optional condition fields (`min_level`, `item`, `held_item`, `min_happiness`, `time_of_day`, `known_move_type`, `location`, …) |

## Goals / Non-Goals

**Goals:**
- One request gives the frontend everything for the detail page, including a renderable, clickable evolution tree.
- PokeAPI's evolution vocabulary is translated once, behind an extensible strategy, into text a user understands.

**Non-Goals:**
- Caching (`add-caching-resilience`), varieties and forms, and localization.

## Decisions

### D1. Domain additions (`domain.pokemon`)
- **`PokemonIdentifier`:** a value object, a sealed interface with records `ById(PokemonId)` and `ByName(String)`. `PokemonIdentifier.parse(String)` trims and lower-cases:
  - digits only → `ById`, only when the value is 1–99999
  - `[a-z][a-z0-9-]{0,49}` → `ByName`
  - anything else → `InvalidFieldException("idOrName", …)`
- **`Height`:** a value object in decimetres, with `metres()` as a one-decimal `BigDecimal` (7 dm → 0.7 m), mirroring `Weight`.
- **`BaseStats`:** a record of `hp`, `attack`, `defense`, `specialAttack`, `specialDefense`, `speed`, all ≥ 0.
- **`EvolutionStage`:** a recursive record of `PokemonId id`, `String name`, `String condition` (nullable) and `List<EvolutionStage> evolvesTo` (immutable).
- **`PokemonDetail`:** a record holding everything in the spec.
- **`PokemonNotFoundException(PokemonIdentifier)`:** a domain exception mapped to 404 by the web adapter.

### D2. Port and use case
- **Port:** `PokemonCatalogPort.findDetail(PokemonIdentifier) → Optional<PokemonDetail>`. An empty result means "no such Pokémon"; `CatalogUnavailableException` means "cannot tell right now". These two outcomes stay distinct all the way to HTTP: 404 versus 503.
- **Use case:** `GetPokemonDetail` / `GetPokemonDetailUseCase` returns the detail, or throws `PokemonNotFoundException`.

### D3. Client: not found versus unavailable
- **Client:** a 404 from PokeAPI raises `PokeApiNotFoundException`; everything else keeps raising `PokeApiUnavailableException`.
- **Adapter:**
  - A 404 on `/pokemon/{idOrName}` → `Optional.empty()`.
  - A 404 on the species or chain of an existing Pokémon means inconsistent upstream data → `CatalogUnavailableException`, as in the catalog.
  - In catalog pages, a 404 for a listed entry stays "unavailable", which is unchanged behaviour.
- **Calls are sequential:** Pokémon → species (id from `species.url`) → chain (id from `evolution_chain.url`). Each depends on the previous one, so there's nothing to parallelize. Caching will absorb the cost later.

### D4. Anti-corruption mapping (`PokeApiMapper` + `PokeApiDetailMapper`)
- **Image:** `imageUrl` is the official artwork, else `null`.
- **Types and abilities:** ordered by `slot`.
- **Size:** `Height`/`Weight` from dm/hg.
- **Stats:** mapped by name (`hp`, `attack`, `defense`, `special-attack`, `special-defense`, `speed`); a missing stat is 0.
- **Description:** the **last** English `flavor_text_entries` item (PokeAPI lists them in game order, so the last is the newest), cleaned by `FlavorText.clean`:
  - every `Character.isISOControl` character (`\n`, `\f`, `\r`, `\t`, …) and the soft hyphen U+00AD becomes a space
  - whitespace runs collapse to one space, then the text is trimmed
  - empty → `null`
- **Evolution tree:** mapped recursively. Each stage's `id` comes from its `species.url`, and `condition` comes from the strategy registry (D5). The root's condition is `null`.

### D5. Strategy: evolution conditions (`adapter.out.pokeapi.evolution`)
- **`EvolutionConditionStrategy`** (interface): `String describe(EvolutionDetailDto detail)`.
- **Implementations, keyed by `trigger.name`:**

| Strategy | Trigger | Output |
|---|---|---|
| `LevelUpCondition` | `level-up` | "Level {min_level}". Without a level: "Level up", followed by qualifiers joined with " ": "with high friendship" (`min_happiness`), "with high affection" (`min_affection`), "during the day" / "at night" (`time_of_day`), "knowing a {Type} move" (`known_move_type`), "knowing {Move}" (`known_move`), "holding {Item}" (`held_item`), "at {Location}" (`location`) |
| `UseItemCondition` | `use-item` | "Use {Item}" |
| `TradeCondition` | `trade` | "Trade", optionally "holding {Item}" / "for {Species}" |
| `FallbackCondition` | any other trigger | the humanized trigger name, for example "Shed", "Spin", "Tower of darkness" |

- **Humanizing:** `water-stone` → `Water Stone`, `fairy` → `Fairy`.
- **`EvolutionConditions`** (registry): `Map<String, EvolutionConditionStrategy>` with the fallback.
  - **Choosing the detail:** the first entry without `location`, else the first entry. This yields "Use Leaf Stone" for Leafeon rather than one of its five location-specific methods.
  - **New PokeAPI triggers** need one new class and one map entry; no existing code changes (open/closed).
- **Why a Strategy here, not chain flattening:** the brief cites "e.g. evolution-chain flattening". The response keeps the **tree**, because it represents branching losslessly and the frontend renders it directly, so flattening would only throw information away. The real variability is in *how* each trigger is described, and that's where the Strategy pays for itself. Recorded as a deliberate deviation from the example.

### D6. Web
- **Endpoint:** `PokemonController` gains `GET /api/v1/pokemon/{idOrName}`. It parses with `PokemonIdentifier.parse`, so an `InvalidFieldException("idOrName")` yields the existing 400 with `errors[]`.
- **Responses:** `PokemonDetailResponse` with nested `BaseStatsResponse` and `EvolutionStageResponse` (recursive), mapped by `PokemonWebMapper`.
- **`GlobalExceptionHandler`:** maps `PokemonNotFoundException` to a 404 `not-found` problem, with detail `No Pokémon matches '{idOrName}'.`

### D7. Testing
| Test | What it covers |
|---|---|
| Domain unit | `PokemonIdentifierTest` (numbers, names, case, boundaries, malformed), `HeightTest`, `BaseStatsTest`, `EvolutionStageTest` (immutable tree) |
| `GetPokemonDetailUseCaseTest` | found, not found, unavailable |
| `FlavorTextTest` | `\n`, `\f`, soft hyphen, collapsing, blank → `null` |
| `EvolutionConditionsTest` | each strategy, the qualifier combinations, fallback humanizing, location-free detail preference |
| `PokeApiDetailMapperTest` (real captures) | Bulbasaur (stats, types, height, description, `Level 16`/`Level 32` chain); Eevee (8 branches with the conditions in the spec); Pikachu chain from Pichu; Ditto with no evolutions |
| `PokeApiCatalogAdapterTest` additions (WireMock) | by id; by name lower-cased in the upstream path; unknown → empty; species 404 → unavailable; 500 → unavailable |
| `PokemonControllerTest` additions | 200 body shape; 404 problem naming the identifier; malformed identifiers → 400 `errors[idOrName]` without calling the use case; 503 |
| `PokemonDetailIT` | Eevee end to end through WireMock |

- **Fixtures:** new trimmed real captures: `pokemon-133`/`species-133`/`evolution-chain-67` (Eevee), `pokemon-132`/`species-132`/`evolution-chain-66` (Ditto), `evolution-chain-1`, `evolution-chain-10`. Species fixtures also keep the newest English flavor text.

### Patterns introduced in this change
| Pattern | Where |
|---|---|
| Strategy | `EvolutionConditionStrategy` and its implementations, chosen per trigger by `EvolutionConditions` |
| Value Object | `PokemonIdentifier` (sealed), `Height`, `BaseStats` |
| Composite-style recursive structure | `EvolutionStage` tree, mirrored by `EvolutionStageResponse` |

## Risks / Trade-offs

- [Three sequential upstream calls per detail, about 300–600 ms cold] → acceptable for one page view. Caching comes in change 7.
- [Condition texts are English-only and best-effort for exotic triggers] → the fallback always yields *something* readable. Exotic qualifiers (beauty, natures, weather) are ignored; PokeAPI's raw data isn't exposed.
- [The "most recent English text" relies on PokeAPI's ordering] → documented. A different ordering would only change which valid description is shown.
- [Names are restricted to a letter first, then `[a-z0-9-]`] → matches every current PokeAPI Pokémon name. Anything else is rejected before any upstream call, which also protects PokeAPI from junk requests.
