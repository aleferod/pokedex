# Tasks: add-pokemon-detail

TDD convention as in earlier changes: each `test:` commit is red on assertions, and the `feat:` commit makes it green.

## 1. Fixtures

- [x] 1.1 Add trimmed, dated real PokeAPI captures:
  - Eevee: `pokemon-133`, `species-133`, `evolution-chain-67`
  - Ditto: `pokemon-132`, `species-132`, `evolution-chain-66`
  - `evolution-chain-1` and `evolution-chain-10`
  - species #1/#2/#3/#25 extended with their newest English flavor text and evolution-chain link; chain details reduced to the fields D5 reads
  - Verify: valid JSON; existing catalog tests stay green; `pokeapi/README.md` updated.

## 2. Domain

- [x] 2.1 Write `PokemonIdentifierTest`, `HeightTest`, `BaseStatsTest` and `EvolutionStageTest` (D1):
  - `25` → id; `Pikachu` → name `pikachu`; `mr-mime`, `porygon2` → names
  - `0`, `100000`, `-1`, `pika chu`, `<script>`, empty, 51 characters → `InvalidFieldException("idOrName")`
  - 7 dm → 0.7 m
  - negative stat rejected
  - tree lists immutable
  - Verify: red against stubs.
- [x] 2.2 Implement `PokemonIdentifier`, `Height`, `BaseStats`, `EvolutionStage`, `PokemonDetail` and `PokemonNotFoundException`. Verify: 2.1 green; ArchUnit green.

## 3. Application

- [x] 3.1 Write `GetPokemonDetailUseCaseTest`: found → detail; empty → `PokemonNotFoundException`; unavailable propagates. Verify: red.
- [x] 3.2 Add `PokemonCatalogPort.findDetail`, the `GetPokemonDetail` port and `GetPokemonDetailUseCase`, plus a temporary `findDetail` in the PokeAPI adapter that returns empty until group 4. Verify: 3.1 green; coverage gate holds.

## 4. Adapter

- [x] 4.1 Write `FlavorTextTest` and `EvolutionConditionsTest` (D4, D5). Verify: red.
- [x] 4.2 Implement `FlavorText`, `EvolutionConditionStrategy` and its implementations, and the `EvolutionConditions` registry. Verify: 4.1 green.
- [x] 4.3 Write `PokeApiDetailMapperTest` on the captures:
  - Bulbasaur fields and its `Level 16` → `Level 32` chain
  - Eevee's 8 branches and the conditions in the spec
  - the Pikachu chain from Pichu
  - Ditto with no evolutions
  - Verify: red.
- [x] 4.4 Implement the detail DTOs (types, stats, artwork, flavor texts, chain) and `PokeApiDetailMapper`. Verify: 4.3 green.
- [x] 4.5 Extend `PokeApiCatalogAdapterTest` (WireMock):
  - detail by id and by name, with the name lower-cased in the upstream path
  - unknown → empty
  - species 404 → unavailable; Pokémon 500 → unavailable
  - Verify: red.
- [x] 4.6 Implement `PokeApiNotFoundException`, the client methods (`pokemon(String)`, `evolutionChain(id)`) and `PokeApiCatalogAdapter.findDetail` (D3). Verify: 4.5 green; catalog tests still green.

## 5. Web

- [x] 5.1 Extend `PokemonControllerTest`:
  - `GET /api/v1/pokemon/1` body shape (stats object, `height` 0.7, nested `evolutionChain`)
  - unknown → 404 `not-found` naming the identifier
  - `pika chu`, `<script>`, `0`, `-1`, 51 characters → 400 `errors[idOrName]` without calling the use case
  - unavailable → 503
  - Verify: red.
- [x] 5.2 Implement the detail endpoint, `PokemonDetailResponse`/`BaseStatsResponse`/`EvolutionStageResponse`, the mapper additions, the 404 mapping, and the `GetPokemonDetail` bean wiring. Verify: 5.1 green.

## 6. End-to-end and docs

- [x] 6.1 Write `PokemonDetailIT` (WireMock as PokeAPI): Eevee end to end with 8 branches and conditions; `PIKACHU` resolves to id 25; unknown → 404. Verify: green, with full `verify`.
- [x] 6.2 Update the README (US02 row ✅, a detail section with a sample response and the evolution-condition strategy, Strategy pattern row) and `docs/problems.md` (`not-found` mentions unknown Pokémon). Verify: lychee offline shows no broken links.

## 7. Integration verification

- [x] 7.1 `./scripts/verify-in-docker.sh` from a clean `target/`. Verify: exit 0; record counts and coverage.
- [x] 7.2 From a fresh clone with `docker compose up --build`, against the real PokeAPI:
  - `/api/v1/pokemon/1`, `/eevee`, `/PIKACHU`, `/ditto`
  - `/missingno` → 404; `/pika%20chu` → 400
  - record response times
  - Verify: as in the spec; `docker compose down -v`.
- [x] 7.3 Run `openspec validate add-pokemon-detail --strict` and gitleaks. Verify: valid, no leaks.

## Workflow follow-up

- Push, open the PR and wait for CI to go green.
- Archive as the PR's last commit, wait for CI to be green again, then squash-merge.
- Send the status report.
