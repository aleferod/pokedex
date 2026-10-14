# ADR-0001: Clean / Hexagonal Architecture in a single module, enforced by ArchUnit

- **Status:** Accepted
- **Date:** 2026-10-14

## Context

The exercise is judged on Clean Architecture: separation of concerns, a business layer independent of both the API and data access, and testability. The service has two very different outside dependencies, PokeAPI over HTTP and PostgreSQL, plus a REST inbound side. We expect to replace pieces later, for example Caffeine with Redis, or PokeAPI with a mirror.

## Decision

Use Ports & Adapters (Hexagonal) with four layers under `com.ballastlane.pokedex`:

| Layer | May depend on | Contains |
|---|---|---|
| `domain` | JDK only | entities, value objects, domain services, domain exceptions |
| `application` | `domain`, JDK | use cases (one class each), inbound ports `port.in`, outbound ports `port.out` |
| `adapter` | `application`, `domain`, frameworks | `in.web` (REST), `out.pokeapi` (HTTP client + anti-corruption layer), `out.persistence` (JPA) |
| `infrastructure` | everything | Spring configuration, security, wiring of use cases to adapters |

Additional rules:
- `application` is framework-free as well. Transactions, caching and resilience are added around ports by adapters or infrastructure, using Decorator/Proxy.
- Adapters never depend on each other or on `infrastructure`.

Everything stays in **one Maven module**. `ArchitectureTest` enforces the rules on every build, and `ArchitectureRulesSelfTest` proves each rule catches a deliberately broken fixture.

## Alternatives considered

- **Multi-module Maven** (`domain`, `application`, `adapters`, `boot`). This gives compile-time enforcement and is the stronger guarantee. We rejected it for this scope: it triples the build files, and it makes a small code base harder to walk through in a 30-minute review. If the service grew or got more contributors, this is where we would go next.
- **Classic layered architecture** (controller → service → repository). It's simpler, but the business logic ends up depending on JPA and HTTP details, and that's exactly what the exercise asks us to avoid.

## Consequences

- Positive: use cases are unit-tested with plain JUnit and Mockito, with no Spring context, so they run fast.
- Positive: PokeAPI JSON never leaks past `adapter.out.pokeapi`, because the anti-corruption mapper turns it into domain objects.
- Positive: a dependency that breaks the rules fails the build, and the report names the class.
- Negative: more types (port interfaces, DTO ↔ domain mappers) and a wiring class in `infrastructure`.
- Negative: the layering is enforced when tests run, not when code compiles. An IDE will happily autocomplete a forbidden import, and you only find out when the tests run.
