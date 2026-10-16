# Tasks: add-caching-resilience

TDD convention as in earlier changes: each `test:` commit is red on assertions, and the `feat:` commit makes it green.

## 1. Fresh reads (application)

- [x] 1.1 Write tests: `GetPokemonDetailUseCaseTest` for `getFresh` (calls `findFreshDetail`; unknown → not found); switch `SyncPokemonUseCaseTest` and `CreateLocalPokemonUseCaseTest` to expect `getFresh` (D2). Verify: red.
- [x] 1.2 Add `PokemonCatalogPort.findFreshDetail` (default) and `GetPokemonDetail.getFresh`, and use it in sync and create. Verify: 1.1 green; coverage gate green.

## 2. Cache decorator

- [x] 2.1 Add `spring-boot-starter-cache` and Caffeine. Write `CachingPokemonCatalogTest` (D1, D2), covering:
  - page hit
  - expiry with a fake ticker
  - unknown detail cached
  - failure not cached and unwrapped
  - number and name keys
  - fresh read bypasses the cache and writes through
  - Verify: red.
- [x] 2.2 Implement `CachingPokemonCatalog`. Verify: 2.1 green; ArchUnit green.

## 3. Resilience

- [x] 3.1 Add the Resilience4j core modules. Write `PokeApiResilienceTest` (D3: retryable only, max attempts, breaker opens, fails fast, 404 ignored, half-open recovery), plus `PokeApiCatalogAdapterTest` additions for classification (503 then 200, 429, reset retried; timeout and 404 once). Verify: red.
- [x] 3.2 Implement `PokeApiResilience`, the `retryable` classification in `PokeApiClient`, and event logging. Verify: 3.1 green; existing adapter tests green.

## 4. Wiring and configuration

- [x] 4.1 Write `CatalogCacheConfigurationTest` (TTL and size from properties) and property-validation tests for the cache, retry and circuit-breaker settings (D4). Verify: red.
- [x] 4.2 Add `CatalogCacheProperties`, the nested PokeAPI settings, the cache manager and decorated-port wiring, `application.yml` defaults, test-profile values, and `CATALOG_CACHE_TTL` in `.env.example`. Verify: 4.1 green; context loads.

## 5. Integration

- [x] 5.1 Add the cache-clearing listener to `@IntegrationTest`. Write the IT additions (D5):
  - catalog: page counts, recovery after a 503, concurrency
  - detail: name then number, unknown, blip retried
  - `LocalPokemonSyncIT`: fresh re-sync
  - new `PokeApiResilienceIT`
  - Verify: green with full `verify`.
- [x] 5.2 Docs:
  - ADR-0003 and the ADR index
  - README: a caching and resilience section, the configuration table, patterns (Decorator ✅, Retry, Circuit Breaker) and trade-offs
  - `docs/problems.md`: 503 from an open circuit
  - Verify: lychee offline shows no broken links.

## 6. Integration verification

- [x] 6.1 `./scripts/verify-in-docker.sh` from a clean `target/`. Verify: exit 0; record counts and coverage.
- [x] 6.2 From a fresh clone with `docker compose up --build` against the real PokeAPI:
  - time a first and a second request for the same page and detail (the second takes milliseconds, because it's served from the cache)
  - re-sync a Pokémon and check that the detail is still served
  - Verify: as described; `docker compose down -v`.
- [x] 6.3 Run `openspec validate add-caching-resilience --strict` and gitleaks. Verify: valid, no leaks.

## Workflow follow-up

- Push, open the PR and wait for CI to go green.
- Archive as the PR's last commit, wait for CI to be green again, then squash-merge.
- Send the status report.
