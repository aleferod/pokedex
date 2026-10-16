# Proposal: add-caching-resilience

## Why

Every catalog page costs PokeAPI 1 + 2 × `size` requests, and every detail costs 3. Data that changes a few times a year is fetched again on every click. That's slow for users and impolite to a free public API whose [fair-use policy](https://pokeapi.co/docs/v2#fairuse) asks clients to cache resources locally. Upstream hiccups also turn straight into 503s, even when a second attempt would have worked. And when PokeAPI is really down, every request still waits for it.

The brief asks for "Caching: Use Spring Cache with Caffeine for PokeAPI responses" and resilience with timeouts, retries and a circuit breaker.

## What Changes

- **Caching (Spring Cache + Caffeine):**
  - Catalog pages and Pokémon details are kept in memory for a configurable time (default 1 hour), up to a configurable number of entries per cache (default 2000).
  - A detail is reachable by number and by name; an unknown Pokémon is remembered as unknown.
  - Failures are never cached.
  - Concurrent first requests for the same entry trigger **one** upstream load.
- **Fresh reads for the local replica:** sync and create (US03/US04) always read PokeAPI directly, and they refresh the cached detail with what they read. A sync therefore really is a sync, and the catalog shows the newer data too.
- **Resilience (Resilience4j) on every PokeAPI call:**
  - **Retry:** connection failures, 5xx and 429 answers are retried, up to 3 attempts in total, with exponential backoff and jitter. A 404, other 4xx answers and timeouts are not retried.
  - **Circuit breaker:** after half of the recent calls fail, PokeAPI isn't called for 30 seconds. Requests answer 503 at once, then trial calls decide whether to close again.
  - **Timeouts:** the existing connect and read timeouts bound every attempt (unchanged).
- **Configuration:** `app.catalog-cache.*`, `app.pokeapi.retry.*` and `app.pokeapi.circuit-breaker.*`, each validated at startup like the existing settings.
- **ADR-0003:** in-process Caffeine cache and Resilience4j core in front of PokeAPI.

## Capabilities

### New Capabilities
- `upstream-caching`: what is cached, for how long, under which keys, what is never cached, and the fresh reads used by the local replica.
- `upstream-resilience`: which upstream failures are retried and how, and the circuit breaker.

### Modified Capabilities
- None. The existing 503 behaviour (`pokemon-catalog`, `pokemon-detail`) is unchanged; it may now come sooner, from an open circuit.

## Out of Scope

- A shared cache (Redis) for several instances, and cache warm-up.
- Serving stale entries while PokeAPI is down (stale-if-error), and an end-to-end time budget per request.
- Exposing cache or circuit metrics through an actuator endpoint (the `metrics` endpoint stays hidden).

## Impact

- **Serves:** the brief's caching and resilience requirements. It also makes the catalog (US01) and detail view (US02) fast on repeat visits.
- **Code:**
  - `adapter.out.cache.CachingPokemonCatalog`: a Decorator around `PokemonCatalogPort`
  - `PokemonCatalogPort.findFreshDetail` and `GetPokemonDetail.getFresh`, used by sync and create
  - `adapter.out.pokeapi.PokeApiResilience` and failure classification in `PokeApiClient`
  - wiring and properties in `infrastructure`
- **Dependencies:** `spring-boot-starter-cache` (Caffeine's Spring integration), `com.github.ben-manes.caffeine:caffeine` (version managed by Boot), and `io.github.resilience4j:resilience4j-retry` and `resilience4j-circuitbreaker` 2.4.0 (core modules, no Boot starter or AOP).
- **Database:** none.
