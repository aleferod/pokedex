# Architecture Decision Records

Short records of the decisions that shape this code base. Each one has a context, a decision and its consequences. A decision is changed by writing a new ADR that supersedes the old one.

| ADR | Title | Status |
|---|---|---|
| [0001](0001-clean-hexagonal-architecture.md) | Clean / Hexagonal Architecture in a single module, enforced by ArchUnit | Accepted |
| [0002](0002-stateless-jwt-authentication.md) | Stateless JWT authentication issued by the API | Accepted |
| [0003](0003-caffeine-cache-and-resilience4j-for-pokeapi.md) | In-process Caffeine cache and Resilience4j in front of PokeAPI | Accepted |
| [0004](0004-testcontainers-over-in-memory-databases.md) | Testcontainers PostgreSQL instead of an in-memory database | Accepted |
| [0005](0005-spec-driven-development-with-openspec.md) | Spec-driven development with OpenSpec | Accepted |
| [0006](0006-spring-boot-4.md) | Spring Boot 4.1 instead of 3.5 | Accepted |

Template: `# ADR-NNNN: Title`, then **Status** and **Date**, then the sections *Context*, *Decision* and *Consequences*.
