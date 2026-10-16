# ADR-0003: In-process Caffeine cache and Resilience4j in front of PokeAPI

- **Status:** Accepted
- **Date:** 2026-10-14

## Context

The catalog (US01) and detail view (US02) are built from PokeAPI.
- **Cost of a page:** a page of N entries costs 1 + 2N upstream requests, and a detail costs 3. The data changes a few times a year, and PokeAPI's fair-use policy asks clients to cache.
- **Failure modes:** PokeAPI is a free public service. It occasionally drops connections or answers 5xx, and it can be down for minutes. The API already has connect and read timeouts and answers 503 with `Retry-After`. But every request still waited for PokeAPI to fail, and a single dropped connection failed the whole page.
- **The brief:** it asks for Spring Cache with Caffeine, and for timeouts, retries and a circuit breaker.
- **Constraints:**
  - The application layer must stay framework-free (ADR-0001).
  - The local replica (US03/US04) stores what it reads from PokeAPI, so it must never store a stale copy.

## Decision

- **Where the cache sits:** behind the port, as a Decorator. `CachingPokemonCatalog` implements `PokemonCatalogPort` and wraps the PokeAPI adapter. The use cases and the PokeAPI adapter don't know a cache exists, and the bare adapter isn't a bean.
- **Which cache:** Spring's cache abstraction on **Caffeine**, in process.
  - There are two caches, `pokemon-pages` and `pokemon-details`, each with a TTL (default 1 hour), a maximum size (default 2000) and recorded statistics.
  - The decorator calls `Cache.get(key, loader)` itself rather than using `@Cacheable`. That gives one load per key under concurrency and needs no proxies.
  - Details are keyed by number and by name, and unknown Pokémon are cached too. Caffeine's TinyLFU admission keeps junk keys from evicting popular ones.
  - Failures are never cached.
- **Fresh reads for persistence:** `findFreshDetail` / `getFresh` bypass the cache and write the result back. Sync and create use them.
- **Resilience4j core modules (`retry`, `circuitbreaker`), decorated explicitly in `PokeApiResilience`:** every PokeAPI request runs as `Retry(CircuitBreaker(request))`.
  - **Retry:** 3 attempts, exponential backoff from 200 ms with jitter. It covers connection failures, 5xx and 429. Timeouts, 404, other 4xx and unreadable answers aren't retried.
  - **Circuit breaker:** it opens when 50 % of the last 20 calls failed (once at least 10 were made), stays open for 30 s, then allows 3 trial calls. A 404 counts as a success.
  - **Timeouts:** the JDK `HttpClient` connect and read timeouts bound each attempt. There's no `TimeLimiter`, because it couldn't stop the HTTP call any sooner.
- **Not the Resilience4j Spring Boot starter:** the starter's annotations rely on AOP ordering and property binding we would not validate. Explicit decoration is a few readable lines, configured by our validated `app.pokeapi.*` properties.

## Consequences

- Positive: a repeated page or detail is served in milliseconds with zero upstream requests, which the integration tests prove with WireMock request counts. PokeAPI sees far less traffic.
- Positive: short blips are absorbed. During an outage, requests answer 503 at once instead of waiting for timeouts, and PokeAPI isn't hammered.
- Positive: caching and resilience are each confined to one class and unit-tested without Spring.
- Negative: each instance has its own cache, so several instances would warm up separately and could briefly disagree.
  - **Mitigation:** one instance today. A shared cache (Redis) behind the same decorator is the next step; only the `CacheManager` would change.
- Negative: catalog data can be up to one TTL old.
  - **Mitigation:** PokeAPI data rarely changes, the TTL is configurable (`CATALOG_CACHE_TTL`), and the local replica always reads fresh.
- Negative: retries add up to about 0.6 s on a failing request, and the JDK client itself may replay a GET once on a dropped connection.
  - **Mitigation:** timeouts aren't retried, and the circuit breaker stops retry storms during an outage.
- Not done: stale-if-error (serving expired entries during an outage) and an end-to-end request time budget. Both are listed as future work.
