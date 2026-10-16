# Tasks: add-pokemon-catalog

TDD convention as in the earlier changes: each `test:` commit is red on assertions, the `feat:` commit makes it green, and refactors are separate commits.

## 1. Test support

- [x] 1.1 Add `org.wiremock:wiremock-standalone` 3.13.2 (test scope). Save trimmed, dated PokeAPI fixtures under `src/test/resources/pokeapi/`: the list page (3 entries, count 1351), Pokémon #1/#2/#3/#25 and species #1/#2/#3/#25. Verify: fixtures are valid JSON under 10 KB each; `verify` stays green.

## 2. Domain: catalog values and paging

- [x] 2.1 Write `PokemonIdTest`, `WeightTest`, `PageRequestTest` and `PageTest`:
  - positive id
  - 69 hg → 6.9 kg, 0 hg → 0.0 kg; negative weight rejected
  - `page`/`size` bounds give `InvalidFieldException` naming the field; `offset`
  - `totalPages` uses ceiling division; empty page
  - Verify: red against stubs.
- [x] 2.2 Implement `PokemonId`, `Weight`, `PokemonSummary`, `PageRequest` and `Page` (D1). Verify: 2.1 green; ArchUnit green.

## 3. Application: port and use case

- [x] 3.1 Write `ListPokemonUseCaseTest` (Mockito): returns the port's page for the request; propagates `CatalogUnavailableException`. Verify: red.
- [x] 3.2 Implement `ListPokemon`, `PokemonCatalogPort`, `CatalogUnavailableException` and `ListPokemonUseCase` (D2). Verify: 3.1 green; domain + application coverage ≥ 85 %.

## 4. PokeAPI adapter

- [x] 4.1 Write `BoundedFanOutTest`:
  - results in input order
  - the high-water mark of concurrent tasks never exceeds the cap
  - first failure propagates
  - MDC correlation id visible inside tasks and absent afterwards
  - Verify: red.
- [x] 4.2 Implement `BoundedFanOut` (D4). Verify: 4.1 green.
- [x] 4.3 Write `PokeApiMapperTest` using the fixtures:
  - bulbasaur → id 1, sprite, `Seed Pokémon`, 6.9 kg, `[overgrow, chlorophyll]`
  - no English genus → `null` category; no sprite → `null`
  - abilities ordered by slot
  - Verify: red.
- [x] 4.4 Implement the PokeAPI DTOs and `PokeApiMapper` (D3). Verify: 4.3 green.
- [x] 4.5 Write `PokeApiCatalogAdapterTest` (WireMock):
  - first page of 3 mapped in order with totals
  - past-the-end page empty with totals
  - list 500, connection refused and read timeout → `CatalogUnavailableException`
  - detail 404 → `CatalogUnavailableException`
  - `User-Agent` header present
  - 10 entries with 300 ms delays finish in < 3 s
  - Verify: red.
- [x] 4.6 Implement `PokeApiClient` and `PokeApiCatalogAdapter`. Verify: 4.5 green; ArchUnit green (the adapter depends only on application ports and domain).

## 5. Web API

- [x] 5.1 Write `PokemonControllerTest` (`@WebSliceTest`, mocked `ListPokemon`):
  - envelope and item JSON (`weight` 6.9)
  - defaults page 0 / size 20 passed to the use case
  - `size=101` → errors[size]; `page=-1&size=-5` → errors[page, size]; `page=abc` → errors[page]
  - `CatalogUnavailableException` → 503 `service-unavailable` with `Retry-After` and no internals
  - anonymous access allowed
  - Verify: red.
- [x] 5.2 Implement `PokemonController`, `PokemonSummaryResponse`, `PageResponse` and `PokemonWebMapper`, plus the `GlobalExceptionHandler` additions (method-validation and type-mismatch errors, 503 mapping) (D5). Verify: 5.1 green; all earlier slice tests green.

## 6. Wiring, configuration and end-to-end

- [x] 6.1 Write `PokeApiPropertiesTest` (defaults; `max-concurrency` 0 or 65 rejected; non-positive timeouts rejected) and `PokemonCatalogIT` (`@IntegrationTest` + WireMock as PokeAPI via `@DynamicPropertySource`: first page mapped end to end; PokeAPI stopped → 503). Verify: red.
- [x] 6.2 Implement `PokeApiProperties`, `PokeApiConfiguration` (RestClient with timeouts and User-Agent, adapter, fan-out, use-case beans) and the `app.pokeapi.*` defaults in `application.yml`. Add `POKEAPI_BASE_URL` to `.env.example` and compose. Verify: 6.1 green; full `verify` green.
- [x] 6.3 Update the README (US01 row ✅, curl example, catalog section: paging, item fields, 503 behaviour, parallel fetching; configuration table; patterns: Anti-Corruption Layer and Bulkhead) and `docs/problems.md` (`service-unavailable` mentions PokeAPI and `Retry-After`). Verify: lychee offline shows no broken links.

## 7. Integration verification

- [x] 7.1 `./scripts/verify-in-docker.sh` from a clean `target/`. Verify: exit 0; record test counts, coverage, ArchUnit, 0 warnings, Spotless clean.
- [x] 7.2 From a fresh clone with `docker compose up --build`, against the **real PokeAPI**:
  - `GET /api/v1/pokemon?page=0&size=5` → 5 entries starting with bulbasaur (category `Seed Pokémon`, weight 6.9)
  - `size=101` → 400
  - `page=100` → empty content
  - record the response time
  - Verify: as described; `docker compose down -v`.
- [x] 7.3 Run `openspec validate add-pokemon-catalog --strict` and gitleaks. Verify: valid, no leaks.

## Workflow follow-up

- Push, open the PR and wait for CI to go green.
- Archive as the PR's last commit, wait for CI to be green again, then squash-merge.
- Send the status report.
