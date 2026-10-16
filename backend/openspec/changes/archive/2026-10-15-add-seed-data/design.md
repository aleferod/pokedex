# Design: add-seed-data

## Context

Accounts are created only by `RegisterUser`, which always assigns `USER` (`User.register`). Local records are created by `CreateLocalPokemon`, which validates the proprietary values, refuses duplicates before calling PokeAPI, reads PokeAPI fresh and saves. There's no startup hook and no seed file. Requirements are in `specs/demo-data/spec.md`.

## Goals / Non-Goals

**Goals:**
- One command gives a demo with known accounts and a populated replica.
- Nothing about it is dangerous by default or surprising on restart.
- The seeder goes through the same use cases as the API, so the domain rules and audit behaviour apply.

**Non-Goals:**
- Admin endpoints, offline seeding and periodic resets (see the proposal).

## Decisions

### D1. A driving adapter that calls the use cases, not SQL migrations
- **Why not Flyway migrations:** demo accounts would exist in every database the migrations ever touch, including production. Hashes would also have to be committed, and upstream data copied by hand.
- **Instead:** `adapter.in.seed.DemoDataSeeder` is an `ApplicationRunner`, a driving adapter like a controller. It reads the seed file and calls the inbound ports `ProvisionUser` and `CreateLocalPokemon`.
  - Passwords are hashed with the production `PasswordHasherPort` and checked against the production `PasswordPolicy`.
  - Proprietary values go through `ProprietaryData.of`.
  - Upstream data comes fresh from PokeAPI, with the existing retry and circuit breaker.
- **It runs before readiness:** Spring Boot reports readiness only after the runners finish. Compose's health check (`/actuator/health/readiness`, 60 s start period) therefore turns healthy only when the demo data is in place. Seeding 10 Pokémon takes about 30 requests, so a few seconds.

### D2. `ProvisionUser`, an operator-side account creation
- **New pieces:**
  - `User.provision(id, username, email, hash, role, now)`, a new factory for an account whose role an operator chose
  - `ProvisionUserUseCase`, whose `provision(ProvisionUserCommand)` returns `true` when it created the account and `false` when the username exists
- **Rules:** it applies the same value objects and password policy as registration. An email that belongs to another account raises `DuplicateUserException`; the seeder logs it and goes on.
- **Not exposed over HTTP:** registration stays `USER`-only, and an admin endpoint is out of scope.

### D3. Seed file and configuration
- **File:** `classpath:seed/demo-data.json` (`app.seed.location` can point elsewhere) has three parts:
  - `createdBy`, the username recorded on seeded records (`admin`)
  - `accounts[]`: `username`, `email`, `password`, `role`
  - `localPokemon[]`: `pokeapiId` plus the proprietary fields, in the API's own shape
- **Reader:** Jackson 3, with unknown members rejected, so a typo fails at startup rather than being ignored.
- **The passwords are public on purpose.** They're the credentials documented in the README. They're stored in the file, never in the database, and never logged.
- **Properties:** `SeedProperties`: `app.seed.enabled` (`SEED_DEMO_DATA`, default `false`) and `app.seed.location`.
  - `DemoDataConfiguration` creates the seeder only when `enabled` is true (`@ConditionalOnBooleanProperty`).
  - Compose passes `SEED_DEMO_DATA` with a default of `true`, and `.env.example` sets `true` with a warning comment.
- **Proprietary content:** Japanese, French and German names copied from PokeAPI's species resources (captured 2026-10-15), with tags such as `starter`, `kanto`, `grass`/`fire`/`water`, `final-evolution` and `mascot`.

### D4. Behaviour on restart and failure
| Situation | Behaviour |
|---|---|
| Username exists | skipped (counted), never updated |
| Email used by another account | warning, skipped |
| Pokémon already stored | skipped: `CreateLocalPokemon` raises the duplicate error before calling PokeAPI |
| PokeAPI unavailable for one Pokémon | warning naming the number, continue. The next start creates it. |
| Malformed seed file or invalid seed values | startup fails with the reason: the data is a bug, not a runtime condition |

The run ends with one INFO summary, for example `Demo data: accounts 2 created, 0 kept; local Pokémon 10 created, 0 kept, 0 failed`. It starts with the WARN about public credentials.

### D5. Testing
| Test | What it covers |
|---|---|
| `UserTest`, `ProvisionUserUseCaseTest` | the chosen role kept; existing username → `false` without hashing; weak password, duplicate email |
| `DemoDataSeederTest` (mocks) | every account provisioned and every Pokémon created with `createdBy`; skips counted; continues after `CatalogUnavailableException`; the warning without passwords; the summary |
| `DemoDataFileTest` | the shipped file: two accounts matching the README, Pokémon #1–#9 and #25, every proprietary set accepted by the domain, every password accepted by the policy |
| `DemoDataConfigurationTest` | no seeder unless enabled |
| `DemoDataSeedIT` (Testcontainers, WireMock, a test seed file with #1–#3 and #25) | admin deletes (204); trainer is `USER`; tag filter finds the seeded starters; re-running keeps edits and creates no duplicates; with PokeAPI down, the accounts still exist |

## Risks / Trade-offs

- [Known passwords] → seeding is off by default, a warning is logged whenever it's on, the README says never to enable it publicly, and nothing is created unless it's switched on.
- [A demo that needs PokeAPI] → the whole application is a PokeAPI client. A failed start is logged and healed on the next restart.
- [Slower first startup] → a few seconds, inside the health check's start period.
