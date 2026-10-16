# Design: add-caching-resilience

## Context

`PokeApiCatalogAdapter` implements `PokemonCatalogPort` on PokeAPI through `PokeApiClient`, which has connect and read timeouts and turns errors into `PokeApiNotFoundException` or `PokeApiUnavailableException`. Catalog pages fan out through `BoundedFanOut`. The use cases `ListPokemon` and `GetPokemonDetail` sit on the port. Sync and create (US03/US04) reuse `GetPokemonDetail`. A `CatalogUnavailableException` becomes 503 with `Retry-After: 30`. Requirements are in `specs/upstream-caching/spec.md` and `specs/upstream-resilience/spec.md`.

## Goals / Non-Goals

**Goals:**
- Repeat visits never touch PokeAPI, failures are never remembered, and a sync is always fresh.
- A short upstream blip is absorbed; a real outage is detected and answered at once.
- No new framework magic: caching and resilience are visible in a few classes and testable without Spring.

**Non-Goals:**
- Distributed cache, stale-if-error, per-request time budgets, metrics endpoints (see the proposal's Out of Scope).

## Decisions

### D1. Cache at the port, as a Decorator
- **`adapter.out.cache.CachingPokemonCatalog implements PokemonCatalogPort`** wraps the PokeAPI adapter. The infrastructure wiring exposes only the decorated port, so the use cases and the PokeAPI adapter don't know a cache exists.
- **Why the port and not each HTTP call:** a cached page is a finished domain object, so one hit saves up to 41 upstream requests and the mapping work. HTTP-level caching would still rebuild the page.
- **Spring Cache with Caffeine,** as the brief asks:
  - a `CaffeineCacheManager` with two caches, `pokemon-pages` (key: `PageRequest`) and `pokemon-details` (key: the identifier's value, `"25"` or `"pikachu"`)
  - each with `expireAfterWrite` = TTL, `maximumSize` = max entries, and `recordStats`
  - the decorator calls `Cache.get(key, loader)` itself. There's no `@Cacheable`, so nothing depends on proxies or self-invocation rules.
- **The load runs on the calling thread,** inside Caffeine's per-key compute. Concurrent callers for the same key wait for it, which gives one load per key, and the request's correlation id stays in the MDC for the PokeAPI calls and their logs.
  - Caffeine's async mode wouldn't hold the map's bin lock during a load, but it runs the loader on the cache's executor, which loses the MDC.
  - The lock is acceptable: a load is bounded by the HTTP timeouts and the circuit breaker. Another key only waits if it hashes to the same bin of a 2000-entry table, and a loader never touches its own cache, so there's no recursive update.
- **What's stored:**
  - Details are stored as `Optional<PokemonDetail>`, so an unknown Pokémon is cached too. Caffeine's TinyLFU admission keeps a flood of junk names from evicting popular entries.
  - A failed load throws, so nothing is stored. The decorator unwraps Spring's `ValueRetrievalException`, so callers still see `CatalogUnavailableException`.
  - After a detail loads, it's also stored under the other key (number or name). This happens outside the loader, because Caffeine forbids writing other keys from inside a load.

### D2. Fresh reads for the local replica (write-through)
- **`PokemonCatalogPort.findFreshDetail(identifier)`:** a default method that delegates to `findDetail`, so the PokeAPI adapter is unchanged. The decorator overrides it to bypass the cache, call the delegate, and store the result under both keys.
- **`GetPokemonDetail.getFresh(identifier)`:** the same not-found handling as `get`, used by `SyncPokemonUseCase` and `CreateLocalPokemonUseCase`. Persisting a snapshot of PokeAPI must not persist a copy up to an hour old.
- **Why not just evict before syncing:** an eviction followed by a load could race with a concurrent reader. Calling the source and then `put` is simpler and leaves the cache holding the newest data.

### D3. Resilience per PokeAPI request (Resilience4j core)
- **Where:** every request goes through `PokeApiClient.call`, so a page's 41 requests are retried individually instead of rebuilding the whole page.
- **`adapter.out.pokeapi.PokeApiResilience`** wraps one `Retry` and one `CircuitBreaker`, named `pokeapi`, built from plain settings. The decoration order is **Retry(CircuitBreaker(request))**, so every attempt is counted by the breaker. An open circuit (`CallNotPermittedException`) isn't retried; it becomes a non-retryable `PokeApiUnavailableException` → 503.
- **Core modules, not the Boot starter:** `resilience4j-retry` and `resilience4j-circuitbreaker` are decorated explicitly. That needs no AOP and no annotations whose order depends on aspect precedence, and settings are validated like our other properties. The trade-off is that we build the registry and event logging ourselves (a few lines).
- **What is transient:** `PokeApiClient` classifies failures (its `PokeApiUnavailableException` carries `retryable`):

| Failure | Retry? | Counts against the circuit? |
|---|---|---|
| Connection refused or reset, other I/O errors | yes | yes |
| 5xx, 429 | yes | yes |
| Timeout (connect or read) | **no**: it has already cost the full timeout | yes |
| 404 | no | no (a success) |
| Other 4xx, unreadable or empty body | no | yes |

- **The JDK client replays too:** the JDK `HttpClient` itself replays an idempotent GET once when the server drops the connection before answering. A reset can therefore reach PokeAPI up to twice per attempt. Tests count our attempts with a status-based failure, which the JDK never replays.
- **Retry:** at most 3 attempts. The wait grows exponentially from 200 ms with ±50 % jitter, so concurrent callers don't retry in lockstep.
- **Circuit breaker:** count-based window of 20 calls, at least 10 calls before it can trip, 50 % failure threshold, 30 s open, then 3 half-open trial calls.
  - The 30 s open period matches the existing `Retry-After: 30`.
- **Timeouts:** the existing JDK `HttpClient` connect timeout (2 s) and read timeout (5 s) bound each attempt. Timeouts aren't retried, so the worst case per request stays about one read timeout. Resilience4j's `TimeLimiter` would only add a thread hop: it can't stop the underlying HTTP call any sooner than the read timeout does.
- **Logging:** a retry is logged at INFO and a circuit state change at WARN, on the calling thread, so the correlation id is in the MDC (`BoundedFanOut` copies it to its virtual threads).

### D4. Configuration
| Property | Default | Meaning |
|---|---|---|
| `app.catalog-cache.ttl` (`CATALOG_CACHE_TTL`) | `PT1H` | Time to live of every cached page and detail |
| `app.catalog-cache.max-entries` | `2000` | Maximum entries per cache |
| `app.pokeapi.retry.max-attempts` | `3` | Attempts per request, including the first (1 disables retries) |
| `app.pokeapi.retry.initial-backoff` | `200ms` | First wait; doubles each time, ±50 % jitter |
| `app.pokeapi.circuit-breaker.failure-rate-threshold` | `50` | Percentage of failed calls that opens the circuit |
| `app.pokeapi.circuit-breaker.minimum-calls` | `10` | Calls needed before the rate counts |
| `app.pokeapi.circuit-breaker.sliding-window` | `20` | Calls considered |
| `app.pokeapi.circuit-breaker.open-duration` | `PT30S` | Time before trial calls |

Each group is a validated record (`CatalogCacheProperties`, and nested records in `PokeApiProperties`). A nonsensical value stops startup with a message naming the property. The test profile shortens the backoff (10 ms) and raises `minimum-calls`, so unrelated integration tests never trip the shared breaker; a dedicated IT uses tight settings.

### D5. Testing
| Test | What it covers |
|---|---|
| `CachingPokemonCatalogTest` | Real Caffeine-backed Spring caches with a fake ticker: hit, expiry, negative entry, failure not cached, dual keys, fresh read with write-through |
| `PokeApiResilienceTest` | Retry only retryable failures, attempt count, breaker opens, fails fast without calling, ignores 404, half-open then closed |
| `PokeApiCatalogAdapterTest` additions (WireMock) | Classification: 503-then-200 retried, 429 retried, reset retried, timeout and 404 not retried |
| `CatalogCacheConfigurationTest`, properties tests | The manager's caches carry the configured TTL and size; invalid values fail at startup |
| `PokemonCatalogIT` / `PokemonDetailIT` additions | WireMock request counts for page and detail hits, name-then-number, unknown cached, recovery after a 503, one load under concurrency |
| `LocalPokemonSyncIT` addition | Re-sync after PokeAPI changed: fresh local record, and the catalog detail updated |
| `PokeApiResilienceIT` | Tight breaker settings: an outage opens the circuit, the next request is 503 without reaching WireMock, then recovery |

`@IntegrationTest` gains a test-execution listener that clears every cache before each test, so test order never matters.

## Risks / Trade-offs

- [In-process cache per instance] → each instance warms up separately and could briefly serve different data. That's acceptable for a single instance; Redis is the next step (ADR-0003).
- [Catalog data up to one TTL old] → PokeAPI changes rarely, and sync and create are always fresh.
- [Retries add latency on failure] → at most two extra waits (about 200 ms and 400 ms), and only for fast-failing errors.
- [The breaker is shared by all callers] → that's the point: one failing dependency shouldn't be hammered by every request. A 404 doesn't count against it.
