# Design: add-pokemon-catalog

## Context

PokeAPI v2 (checked against the live API on 2026-10-14):

| Call | Returns | Used for |
|---|---|---|
| `GET /pokemon?offset&limit` | `{count, next, previous, results:[{name, url}]}`; `count` is 1351 | Paging and totals. The id is parsed from `url`. |
| `GET /pokemon/{id}` | `weight` in hectograms, `sprites.front_default`, `abilities[]{ability.name, slot, is_hidden}`, `species.url` | Sprite, weight, abilities |
| `GET /pokemon-species/{id}` | `genera[]{genus, language.name}` | Category: the genus whose language is `en` |

- The species id can differ from the Pokémon id (forms 10001+ point to their base species), so it's read from `species.url`.
- Detail responses are 250–310 KB each because of `moves`. Our DTOs bind only what we use.
- The platform pieces from earlier changes: the error contract, the correlation filter, ArchUnit layer rules, and the public routing rule (`GET /api/v1/pokemon/**` is already `permitAll`).

## Goals / Non-Goals

**Goals:**
- PokeAPI's shape stays inside `adapter.out.pokeapi`. Everything else sees `PokemonSummary`.
- A page of 20 costs about one round trip of latency, not 40.
- Failures are fast and honest (503), never hanging and never leaking internals.

**Non-Goals:**
- Caching, retry and circuit breaking: `add-caching-resilience` adds them around the same port (Decorator/Proxy), with no change to the use case.
- Spreading load across PokeAPI's CDN, or preloading the catalog.

## Decisions

### D1. Domain model
- **`domain.pokemon.PokemonId`**: a value object holding a positive `int`.
- **`domain.pokemon.Weight`**: a value object, `Weight.ofHectograms(int)` (must be ≥ 0), exposing `kilograms()` as a `BigDecimal` with one decimal (69 → 6.9). Hectograms are kept as the canonical unit, so the conversion is exact.
- **`domain.pokemon.PokemonSummary`**: a record of `PokemonId id`, `String name`, `String spriteUrl` (nullable), `String category` (nullable), `Weight weight`, and `List<String> abilities` (an immutable copy).
- **`domain.shared.PageRequest(page, size)`**: a value object that validates `page ≥ 0` and `1 ≤ size ≤ 100` (throwing `InvalidFieldException`), and exposes `offset()`.
- **`domain.shared.Page<T>(content, page, size, totalElements)`** with derived `totalPages()` (ceiling). Both are reused later by the local CRUD.

### D2. Port and use case
- **`application.port.in.pokemon.ListPokemon`:** `Page<PokemonSummary> list(PageRequest)`. **`ListPokemonUseCase`** delegates to the port.
  - It stays deliberately thin. Caching and resilience will wrap the port (Decorator) without touching it, and the use case is where future rules such as filtering land.
- **`application.port.out.pokemon.PokemonCatalogPort`:** `Page<PokemonSummary> findPage(PageRequest)`.
  - It throws **`CatalogUnavailableException`**, defined next to the port because it's part of the port's contract: an upstream failure, not a business rule.
- **Why the port returns finished summaries** rather than "list ids, then fetch each one": that PokeAPI needs 1 + 2N calls is an implementation detail of this one source. Another catalog (a mirror, or our own database) might answer in a single query. Keeping the fan-out in the adapter keeps the port honest. That's the anti-corruption layer's job.

### D3. PokeAPI adapter (`adapter.out.pokeapi`): Adapter + Anti-Corruption Layer
- **`PokeApiClient`:** thin calls on Spring `RestClient` (`listPokemon(offset, limit)`, `pokemon(id)`, `species(id)`).
  - It throws `PokeApiUnavailableException` on I/O errors, timeouts and 5xx. A 404 on a detail is treated the same way, because an entry that the list returned but can't be fetched means the catalog is inconsistent.
- **`dto/*`:** records bound with `@JsonIgnoreProperties(ignoreUnknown = true)`, overriding the API's strict unknown-member setting, which only applies to *our* clients' JSON. Snake-case names use `@JsonProperty`.
- **`PokeApiMapper`** (the anti-corruption layer):
  - takes the id from the resource URL
  - converts weight from hectograms via `Weight`
  - uses `sprites.front_default`
  - orders abilities by `slot`
  - picks the genus whose `language.name == "en"`
  - turns null or blank into `null`
- **`PokeApiCatalogAdapter implements PokemonCatalogPort`:**
  1. List `offset`/`limit`.
  2. Fan out one task per entry: fetch the Pokémon, then its species, then map.
  3. Collect in list order.
  4. Wrap any `PokeApiUnavailableException` in `CatalogUnavailableException`.
- **Pages past the end:** PokeAPI returns `results: []` with the real `count`, so no special case is needed.

### D4. Bounded fan-out (`adapter.out.pokeapi.BoundedFanOut`)
- **Mechanism:**
  - Java 21 virtual threads, through a `newVirtualThreadPerTaskExecutor()` per call.
  - An **instance-wide `Semaphore`** (permits = `app.pokeapi.max-concurrency`, default 8), acquired around each task. Concurrent requests share the cap, which protects PokeAPI and our socket pool.
  - Results come back in input order. The first failure cancels the remaining tasks and is rethrown.
- **Correlation id:** the caller's MDC map is copied into each task and cleared afterwards, so parallel log lines carry it (bootstrap-project D6 promised this).
- **Rejected alternatives:**
  - **A Spring `ThreadPoolTaskExecutor` with a `TaskDecorator`.** Platform threads mostly sit blocked on I/O here; virtual threads plus a semaphore give the same bound with less tuning.
  - **WebClient/Reactor.** Adding a reactive stack for one fan-out isn't worth it in an MVC app.

### D5. Web (`adapter.in.web.pokemon`)
- **`PokemonController`:** `GET /api/v1/pokemon` with `@RequestParam` `page` (default 0) and `size` (default 20), annotated `@Min(0)` and `@Min(1) @Max(100)`. The controller is deliberately **not** `@Validated`. *Refined during implementation:* with `@Validated`, Boot's AOP `MethodValidationPostProcessor` would validate first and throw `ConstraintViolationException`, which surfaces as a 500. Without it, Spring MVC's built-in method validation handles the constraints.
  - Spring's built-in method validation raises `HandlerMethodValidationException`, which reports **all** invalid parameters at once.
  - The domain's `PageRequest` validates again (defense in depth).
- **Responses:** `PokemonSummaryResponse` (`weight` as a `BigDecimal` in kg, documented in OpenAPI) and a generic `PageResponse<T>`. `PokemonWebMapper` (DTO + Mapper) builds them.
- **`GlobalExceptionHandler` additions:**
  - `handleHandlerMethodValidationException`: one `errors[]` entry per parameter (field = parameter name).
  - `handleTypeMismatch`: an entry for the parameter with "has the wrong type".
  - `CatalogUnavailableException`: 503 `service-unavailable` with `Retry-After: 30`, logged at WARN with the cause. The client gets a fixed detail only.

### D6. Configuration (`infrastructure.pokeapi`)
`PokeApiProperties` is a validated record (`app.pokeapi.*`):

| Property | Default | Notes |
|---|---|---|
| `base-url` | `https://pokeapi.co/api/v2` | from `POKEAPI_BASE_URL` |
| `connect-timeout` | `PT2S` | |
| `read-timeout` | `PT5S` | |
| `max-concurrency` | `8` | 1–64 |
| `user-agent` | `pokedex-api/<version> (+https://github.com/aleferod/pokedex-api)` | |

- **`PokeApiConfiguration`** builds a `RestClient` on `JdkClientHttpRequestFactory` (JDK `HttpClient` with connect timeout, HTTP/1.1, plus a read timeout). It sets a default `User-Agent` and `Accept: application/json`, and wires the client, adapter and use case.
- **No Boot `RestClient.Builder` dependency:** PokeAPI is the only outbound HTTP client, and building it explicitly keeps the timeouts visible in one place.
- *Found during implementation:* with `JdkClientHttpRequestFactory`, Spring applies the read timeout to reading the **whole response body**, not just to inactivity. Detail responses are about 300 KB, so the 5 s default leaves headroom. The adapter test uses 2 s, because 800 ms was too tight for the first request in a cold JVM.

### D7. Testing
| Test | What it covers |
|---|---|
| Domain unit | `PokemonIdTest`, `WeightTest` (hectogram conversion, rounding, negatives), `PageRequestTest`, `PageTest` (ceiling division, empty) |
| Use case unit | `ListPokemonUseCaseTest` (delegation) |
| `BoundedFanOutTest` | order kept; the configured maximum in flight is never exceeded (measured with an `AtomicInteger` high-water mark and latches); first failure propagates; MDC visible inside tasks and cleared after |
| `PokeApiMapperTest` | fixtures → summary; missing genus or sprite → `null`; abilities sorted by slot |
| `PokeApiCatalogAdapterTest` (WireMock, no Spring) | happy page from real-but-trimmed fixtures; past the end; 500 / connection refused / read timeout → `CatalogUnavailableException`; `User-Agent` sent; 10 entries with 300 ms delays finish in < 3 s against a sequential 6 s |
| `PokemonControllerTest` (web slice) | envelope, defaults, 400s with `errors[]` (101, −1/−5, `abc`), 503 problem with `Retry-After`, public without a token |
| `PokemonCatalogIT` (full stack + WireMock as PokeAPI) | first page mapped end to end; PokeAPI down → 503 |

- **Fixtures:** real PokeAPI responses for #1, #2, #3 and #25 (Pokémon and species) and a 3-entry list page, saved 2026-10-14 under `src/test/resources/pokeapi/` and **trimmed** to the fields the API uses (about 5 KB each instead of about 300 KB).

### Patterns introduced in this change
| Pattern | Where |
|---|---|
| Adapter / Anti-Corruption Layer | `PokeApiClient` + `dto/*` + `PokeApiMapper` behind `PokemonCatalogPort` |
| Value Object | `PokemonId`, `Weight`, `PageRequest` |
| DTO + Mapper | `PokemonWebMapper`, `PokeApiMapper` |
| Bulkhead (bounded concurrency) | `BoundedFanOut` with a shared semaphore |

## Risks / Trade-offs

- [Each page costs 1 + 2N PokeAPI calls] → bounded parallelism now; caching in `add-caching-resilience` makes repeat pages free.
- [A single failing detail fails the whole page with 503] → an honest answer rather than a silently partial page. Caching and retry will make it rare.
- [Virtual threads pin on `synchronized` blocks inside the JDK HTTP client] → negligible at concurrency 8, and Java 21's HTTP client is virtual-thread friendly.
- [The cap is per instance, not global] → fine for one instance. A global rate limit would need shared state; listed as future work.
- [The upstream `count` includes alternate forms (10001+), so totals are higher than "Pokédex number 1025"] → we report what PokeAPI reports and document it.
- [Fixtures go stale if PokeAPI changes] → the DTOs ignore unknown members, and fixtures are dated real captures. `PokemonCatalogIT` doesn't hit the real PokeAPI, so CI stays deterministic.
