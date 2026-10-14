# ADR-0006: Spring Boot 4.1 instead of 3.5

- **Status:** Accepted
- **Date:** 2026-10-14

## Context

The original brief said "Spring Boot 3.x (latest stable)". The last 3.x line, 3.5 (latest patch 3.5.16), reached the end of open-source support on 2026-06-30. Spring Boot 4.1 (4.1.1) is the current release, with support until 2027-07. We need these libraries:
- Resilience4j
- springdoc-openapi
- Testcontainers
- ArchUnit
- WireMock
- MapStruct

All of them ship versions that work with Boot 4: `resilience4j-spring-boot4` 2.4.0, springdoc 3.1.x (built against Boot 4.1), and Testcontainers 2.0.x, which Boot manages.

## Decision

Use Spring Boot 4.1.x on Java 21. This brings in:
- Spring Framework 7
- Spring Security 7
- Hibernate 7
- Jackson 3
- JUnit 6
- Flyway 12
- the modular starters (`spring-boot-starter-webmvc`, `spring-boot-starter-flyway`, …), with a matching `*-test` starter for each module

## Consequences

- Positive: new code doesn't start on an end-of-life framework, and we get the Framework 7 additions: `RestTestClient`, built-in API versioning support and JSpecify null-safety.
- Negative: there are fewer blog posts and Q&A answers for Boot 4, and some packages moved, for example:
  - `@WebMvcTest` → `org.springframework.boot.webmvc.test.autoconfigure`
  - `ErrorController` → `org.springframework.boot.webmvc.error`
  - `TestRestTemplate` → the `spring-boot-resttestclient` module
- Negative: springdoc still brings Jackson 2 in for swagger-core, so both Jackson lines are on the classpath. Our code uses Jackson 3 (`tools.jackson`) only.
- Mitigation: versions are pinned and were checked against Maven Central. If a library blocks progress, we record it here and ask before downgrading.
