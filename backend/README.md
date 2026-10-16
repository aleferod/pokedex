# Pokédex API

[![CI](https://github.com/aleferod/pokedex/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/aleferod/pokedex/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-6db33f)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](../LICENSE)

A REST API built with Java 21 and Spring Boot for the Ballast Lane Java technical exercise. It browses Pokémon from [PokeAPI](https://pokeapi.co/docs/v2), keeps a local PostgreSQL replica with proprietary fields, and lets authenticated users manage that replica. It follows **Clean / Hexagonal Architecture** and is built **test-first** and **spec-first** with [OpenSpec](https://github.com/Fission-AI/OpenSpec). It's the backend of the [Pokédex](../README.md); the frontend, pokedex-web, is in [`frontend/`](../frontend).

> **Status:** complete. The backend changes below are done, and so is the frontend in [`frontend/`](../frontend). The repository root runs **the whole product in one command** (see [Quick start](#quick-start)). [docs/PRESENTATION.md](../docs/PRESENTATION.md) is the guide for the presentation and code review.
> - `bootstrap-project`: the platform foundation (error contract, correlation ids, CORS, health/info/OpenAPI, migrations, quality gates, Docker, CI).
> - `add-user-auth`: registration, JWT login and public vs protected routes.
> - `add-pokemon-catalog`: US01, the paginated catalog from PokeAPI.
> - `add-pokemon-detail`: US02, the detailed view with the evolution tree.
> - `add-local-sync`: US03, the local replica in PostgreSQL with proprietary fields.
> - `add-local-crud`: US04, listing, creating, editing and deleting local records with ETags and optimistic locking.
> - `add-caching-resilience`: a Caffeine cache in front of PokeAPI, plus retries and a circuit breaker on every PokeAPI request.
> - `add-seed-data`: demo accounts and ten local Pokémon, created at startup when demo data is enabled.
> - `add-genai-task-api-doc`: the GenAI exercise ([docs/GENAI.md](docs/GENAI.md)), with a generated Task API sample that CI builds and tests.
> - `add-presentation-doc` and `update-guide-for-catalog-search`: the presentation and code-review guide.
> - `add-catalog-name-search`: searching the catalog by part of a name, which pokedex-web uses to search as you type.
>
> See [Roadmap](#roadmap).

## Contents

- [Quick start](#quick-start)
- [Demo data](#demo-data)
- [User stories and endpoints](#user-stories-and-endpoints)
- [Catalog (US01)](#catalog-us01)
- [Detail view (US02)](#detail-view-us02)
- [Local replica (US03)](#local-replica-us03)
- [Managing local records (US04)](#managing-local-records-us04)
- [Caching and resilience](#caching-and-resilience)
- [Authentication](#authentication)
- [Error format](#error-format)
- [Architecture](#architecture)
- [Design patterns](#design-patterns)
- [Tech stack](#tech-stack)
- [Configuration](#configuration)
- [Testing and quality gates](#testing-and-quality-gates)
- [Spec-driven workflow (OpenSpec)](#spec-driven-workflow-openspec)
- [Architecture decisions](#architecture-decisions)
- [GenAI exercise and AI use](#genai-exercise-and-ai-use)
- [Roadmap](#roadmap)
- [Trade-offs and future improvements](#trade-offs-and-future-improvements)
- [Presentation and code review](../docs/PRESENTATION.md)

## Quick start

You only need **Docker** (with Compose v2).

```bash
cp .env.example .env
docker compose up --build
```

| What | URL |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI document | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |

- **Ready to try:** the stack seeds two demo accounts and ten local Pokémon, as described in [Demo data](#demo-data). Log in as `trainer` / `Trainer@123` to browse and edit, or as `admin` / `Admin@123` to delete.
- **Secrets:** `.env.example` ships working placeholders for `DB_PASSWORD` and `JWT_SECRET`, so the demo runs as is. Anywhere else, set real values; generate a signing secret with `openssl rand -base64 48`.
- **Ports:** PostgreSQL is published on host port **5433** (`DB_PORT`), so it won't clash with a PostgreSQL you may already run on 5432. If port 8080 is taken, change `SERVER_PORT` in `.env`.
- **Stop:** `docker compose down` keeps the data (named volume `pokedex-db-data`). `docker compose down -v` wipes it.
- **Run the API on the host instead (optional):** start only the database with `docker compose up -d postgres`, then run `./mvnw spring-boot:run`. This needs JDK 21. The default `local` profile reads the same `.env` and connects to `localhost:5433`.

**The whole product (database, this API and the web app) in one command**, from the repository root:

```bash
cp .env.example .env && docker compose up --build
```

The app opens at <http://localhost:8000>, and Swagger UI stays at <http://localhost:8080/swagger-ui.html>. That Compose file builds this API and the app from the same checkout; the details are in the [frontend README](../frontend/README.md#run-the-whole-product).

## Demo data

With `SEED_DEMO_DATA=true` (the default in `.env.example` and Compose), the service makes sure this data exists every time it starts:

| Account | Password | Role | Can |
|---|---|---|---|
| `admin` | `Admin@123` | `ADMIN` | everything, including `DELETE /api/v1/local-pokemon/{id}` |
| `trainer` | `Trainer@123` | `USER` | browse, sync, create and edit local records |

**Local Pokémon:** the Kanto starter lines (#1–#9) and Pikachu (#25).
- Their upstream fields come fresh from PokeAPI.
- Their proprietary fields come from [`seed/demo-data.json`](src/main/resources/seed/demo-data.json): Japanese, French and German names taken from PokeAPI, the region `Kanto`, a habitat note, and tags such as `starter`, `grass`, `fire`, `water`, `final-evolution` and `mascot`.
- They're recorded as created by `admin`. Try `GET /api/v1/local-pokemon?tag=starter`.

**How seeding behaves:**
- **Same rules as the API:** it goes through the regular use cases, so the password policy, BCrypt hashing, domain validation and PokeAPI reads all apply. Nothing is written to the database by hand.
- **Idempotent:** existing accounts and records are kept as they are, so a restart neither duplicates nor overwrites your edits. Delete a record, and the next start brings it back.
- **Tolerant:** if PokeAPI is down, the accounts are still created, and each missing Pokémon is logged and created on a later start. The service starts normally either way. Readiness waits for seeding, so a healthy container already has its data.

> **Never enable demo data on a public deployment.** The passwords above are public. Seeding is off unless `SEED_DEMO_DATA=true`, and the service logs a warning whenever it's on.

## User stories and endpoints

| Story | Endpoint | Auth | Status |
|---|---|---|---|
| Operations | `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` | public | ✅ |
| Operations | `GET /actuator/info` (name, version) | public | ✅ |
| API docs | `GET /v3/api-docs`, `GET /swagger-ui.html` | public | ✅ |
| Auth | `POST /api/v1/auth/register` → 201 | public | ✅ |
| Auth | `POST /api/v1/auth/login` → 200 with bearer token | public | ✅ |
| Auth | `GET /api/v1/auth/me` | bearer | ✅ |
| US01 Enumeration | `GET /api/v1/pokemon?page=0&size=20` | public | ✅ |
| US01 Search by part of a name | `GET /api/v1/pokemon?name=pika` (3–50 characters, same paging) | public | ✅ |
| US02 Detail | `GET /api/v1/pokemon/{idOrName}` | public | ✅ |
| US03 Sync | `POST /api/v1/local-pokemon/sync/{idOrName}` → 201 import / 200 re-sync | bearer | ✅ |
| US03 Bulk sync | `POST /api/v1/local-pokemon/sync?limit=N` (1–20, default 10) | bearer | ✅ |
| US03/US04 Read one | `GET /api/v1/local-pokemon/{id}` (with `ETag`) | bearer | ✅ |
| US04 List | `GET /api/v1/local-pokemon?page&size&tag&name` | bearer | ✅ |
| US04 Create | `POST /api/v1/local-pokemon` → 201 with `Location` and `ETag` | bearer | ✅ |
| US04 Replace | `PUT /api/v1/local-pokemon/{id}` with `If-Match` | bearer | ✅ |
| US04 Partial update | `PATCH /api/v1/local-pokemon/{id}` (JSON Merge Patch) with `If-Match` | bearer | ✅ |
| US04 Delete | `DELETE /api/v1/local-pokemon/{id}` → 204 | bearer, `ADMIN` | ✅ |

All other management endpoints (`/actuator/env`, `/beans`, `/heapdump`, `/shutdown`, …) are not exposed and return 404.

## Catalog (US01)

```bash
curl -s 'localhost:8080/api/v1/pokemon?page=0&size=2'
```

```json
{
  "content": [
    { "id": 1, "name": "bulbasaur",
      "spriteUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
      "category": "Seed Pokémon", "weight": 6.9, "abilities": ["overgrow", "chlorophyll"] },
    { "id": 2, "name": "ivysaur", "category": "Seed Pokémon", "weight": 13.0, "...": "..." }
  ],
  "page": 0, "size": 2, "totalElements": 1351, "totalPages": 676
}
```

**Item fields:**
- `category` is the English genus from `/pokemon-species`.
- `weight` is in **kilograms**, converted exactly from PokeAPI's hectograms.
- `abilities` keep PokeAPI's slot order, hidden abilities included.
- `spriteUrl` and `category` are `null` when PokeAPI has no value.

**Paging:**
- `page` is zero-based (default 0) and `size` is 1–100 (default 20).
- Bad values return 400, with one `errors` entry per parameter.
- A page past the end returns 200 with empty `content`.
- `totalElements` is PokeAPI's own count, which includes alternate forms (ids 10001+).

**When PokeAPI is unreachable, slow (past the 5 s read timeout) or failing after retries:** the API answers in time with **503** `service-unavailable` and `Retry-After: 30`. It never hangs, and never echoes upstream errors.

**How a page is built:** PokeAPI's list endpoint only returns names, so each entry needs `/pokemon/{id}` and `/pokemon-species/{id}`. That's 1 + 2N calls for a page of N. The adapter fetches entries **in parallel on virtual threads**, behind an instance-wide cap (`max-concurrency`, default 8) that protects PokeAPI. A 10-entry page whose upstream calls take 300 ms each returns in well under a second, against 6 s if fetched sequentially. A page that was already served comes from the cache with no upstream calls at all; see [Caching and resilience](#caching-and-resilience).

**Search by part of a name:** `GET /api/v1/pokemon?name=pika` returns only the Pokémon whose name contains the text, in the same order and with the same paging and entries.

```bash
curl -s 'localhost:8080/api/v1/pokemon?name=pika&size=3'   # pikachu, pikachu-rock-star, pikachu-belle … of 17
```

- **The text:** it's trimmed and case-insensitive, and spaces count as hyphens (`mr mime` → `mr-mime`, `mr-mime-galar`). It must be **3 to 50** letters, digits or hyphens, otherwise the answer is 400 with `errors[name]`. A blank `name` lists the whole catalog.
- **How it works:**
  - PokeAPI has no search, so the API reads its full name list (one request, about 90 KB) and caches it like a page (`pokemon-names`).
  - The use case filters it by containment; the matching rule lives in the domain's `PokemonNameQuery`.
  - Only the requested page of matches is fetched in detail, so a search page costs what a catalog page costs.
- **Not matched:** localized names (that's the local replica) and typos.

## Detail view (US02)

```bash
curl -s localhost:8080/api/v1/pokemon/eevee      # or /133, /EEVEE, /Eevee
```

```json
{
  "id": 133, "name": "eevee",
  "imageUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/133.png",
  "category": "Evolution Pokémon",
  "description": "Harbors the potential to evolve into manifold forms. Within Eevee lies the key to the mysteries of Pokémon evolution—I'm certain of it.",
  "types": ["normal"], "height": 0.3, "weight": 6.5, "abilities": ["run-away", "adaptability", "anticipation"],
  "stats": { "hp": 55, "attack": 55, "defense": 50, "specialAttack": 45, "specialDefense": 65, "speed": 55 },
  "evolutionChain": {
    "id": 133, "name": "eevee", "condition": null,
    "evolvesTo": [
      { "id": 134, "name": "vaporeon", "condition": "Use Water Stone", "evolvesTo": [] },
      { "id": 196, "name": "espeon", "condition": "Level up with high friendship during the day", "evolvesTo": [] },
      { "id": 700, "name": "sylveon", "condition": "Level up with high affection knowing a Fairy move", "evolvesTo": [] },
      "… 8 branches in total"
    ]
  }
}
```

**Identifier:**
- A Pokédex number (1–99999), or a name: letters, digits and hyphens, starting with a letter, up to 50 characters, case-insensitive.
- A malformed identifier returns 400 without any call to PokeAPI. An unknown one returns 404 `not-found`.

**Description:** the newest English flavor text. The line breaks and form feeds of the original game screens are cleaned up.

**Evolution chain:**
- It's a **tree** from the base form, so branching evolutions like Eevee's are represented exactly. Requesting a middle stage (Pikachu) still returns the whole family (Pichu → Pikachu → Raichu).
- Each `condition` comes from the **Strategy** pattern: one `EvolutionConditionStrategy` per PokeAPI trigger (`level-up`, `use-item`, `trade`, plus a fallback). Each turns PokeAPI's raw evolution data into a sentence.
- When PokeAPI lists several game-specific methods for one evolution, the first one that doesn't depend on a specific location wins. For Leafeon that's "Use Leaf Stone".

## Local replica (US03)

PokeAPI is read-only, so anything the business adds lives in a **local copy** in PostgreSQL. Each local Pokémon has:
- a snapshot of its PokeAPI data
- **proprietary fields**
- audit columns
- a version

```bash
curl -s -X POST localhost:8080/api/v1/local-pokemon/sync/pikachu -H "Authorization: Bearer $TOKEN" -i
# HTTP/1.1 201
# Location: /api/v1/local-pokemon/3f6c…
# ETag: "0"
```

| Field group | Fields | Who changes it |
|---|---|---|
| Upstream (PokeAPI) | `name`, `spriteUrl`, `imageUrl`, `category`, `types`, `height`, `weight`, `abilities`, `stats` | only **sync**, which replaces them all |
| Proprietary | `localizedNames` (`{"pt-BR": "Pikachu"}`), `region`, `habitatNotes`, `internalTags`, `notes` | the business, through the [US04 API](#managing-local-records-us04). Sync **never** touches them. |
| Audit | `createdAt`, `createdBy` (the importing user), `updatedAt`, `syncedAt` | the service |
| Concurrency | `version`, also sent as the `ETag` | increases on every change |

**Sync semantics:**
- The **first import returns 201**, with `Location` and `ETag`.
- A **re-sync returns 200**: it refreshes the upstream fields, advances `syncedAt`, keeps every proprietary field and the creation audit, and bumps the version.
- PokeAPI is read before anything is written, so an unknown Pokémon (404) or an outage (503) leaves the store untouched.
- **Races:** two concurrent first imports produce **one** record. The unique PokeAPI number makes the loser re-sync the winner (201 + 200). Optimistic locking guards concurrent re-syncs.

```mermaid
erDiagram
  local_pokemon ||--o{ local_pokemon_localized_name : "has names"
  local_pokemon {
    uuid id PK
    int pokeapi_id UK
    varchar name
    text_array types
    int height_dm
    int weight_hg
    text_array abilities
    int hp_attack_defense_etc "six stat columns"
    varchar region "proprietary"
    varchar habitat_notes "proprietary"
    text_array internal_tags "proprietary, GIN index"
    varchar notes "proprietary"
    timestamptz created_at
    timestamptz updated_at
    timestamptz synced_at
    varchar created_by
    bigint version "optimistic lock / ETag"
  }
  local_pokemon_localized_name {
    uuid local_pokemon_id PK, FK
    varchar locale PK "pt-BR, ja, ..."
    varchar name
  }
  users {
    uuid id PK
    varchar username UK
    varchar email UK
    varchar password_hash "BCrypt"
    varchar role "USER | ADMIN"
  }
```

The schema is owned by Flyway (`V1` baseline, `V2` users, `V3` local replica), and Hibernate only validates it.

## Managing local records (US04)

Every authenticated user can list, create and edit local records. Only an `ADMIN` can delete them. Registration always creates a `USER`; use the [demo](#demo-data) `admin` account.

```bash
# Create: the upstream fields come from PokeAPI, the proprietary ones from you
curl -si -X POST localhost:8080/api/v1/local-pokemon -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"pokeapiId": 1, "region": "Kanto", "internalTags": ["starter"], "localizedNames": {"pt-BR": "Bulbassauro"}}'
# HTTP/1.1 201
# Location: /api/v1/local-pokemon/<id>
# ETag: "0"

# List: tag matches exactly, name matches a substring, both ignoring case
curl -s 'localhost:8080/api/v1/local-pokemon?tag=STARTER&name=saur' -H "Authorization: Bearer $TOKEN"

# Partial update (JSON Merge Patch): send back the ETag you read
curl -si -X PATCH localhost:8080/api/v1/local-pokemon/<id> -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/merge-patch+json' -H 'If-Match: "0"' \
  -d '{"notes": "Loves sunlight", "localizedNames": {"ja": "フシギダネ"}}'
# HTTP/1.1 200
# ETag: "1"

# The same ETag again is a stale copy: 409 conflict, nothing changes
```

**Verbs:**

| Request | Effect |
|---|---|
| `GET /api/v1/local-pokemon` | The paginated envelope ordered by PokeAPI number. `page` and `size` follow the catalog's rules; `tag` and `name` (each at most 50 characters) combine. |
| `POST /api/v1/local-pokemon` | Needs `pokeapiId`; the proprietary fields are optional. A duplicate number is **409** with `errors[pokeapiId]`; a number PokeAPI doesn't know is **400** with `errors[pokeapiId]`. |
| `PUT /{id}` | Replaces **every** proprietary field. A field left out is cleared. |
| `PATCH /{id}` | JSON Merge Patch ([RFC 7396](https://www.rfc-editor.org/rfc/rfc7396)), sent as `application/merge-patch+json` or `application/json`. An absent member is kept, `null` clears it, and a value replaces it. `localizedNames` merges per locale, and a locale set to `null` is removed. |
| `DELETE /{id}` | **204**, `ADMIN` only. `If-Match` is optional here. |

Editing changes `updatedAt` and the version. The upstream fields and `syncedAt` stay as they are.

**Optimistic concurrency (no lost updates):**
- Every single-record response carries `ETag: "<version>"`.
- `PUT` and `PATCH` must send it back in `If-Match`. `"3"`, `W/"3"` and `*` (any version) are accepted; anything else is a 400 naming `If-Match`.
- A missing `If-Match` gets **428** `precondition-required`, so a client can't overwrite blindly.
- An outdated one gets **409** `conflict`, and nothing changes. The version is checked by the use case and again by JPA `@Version` when saving, which catches a writer that slipped in between.
- **409, not 412:** RFC 9110 suggests 412 Precondition Failed for a failed `If-Match`. The exercise asks for 409 on a stale update, so this API answers 409, and a client treats both the same way: reload, reapply, retry.

**Defensive rules:**
- **Read-only fields.** The upstream fields (`name`, `spriteUrl`, `imageUrl`, `category`, `types`, `height`, `weight`, `abilities`, `stats`) and the identity and audit fields (`id`, `version`, `createdAt`, `updatedAt`, `syncedAt`, `createdBy`, and `pokeapiId` outside `POST`) can't be set. Sending one is a 400 whose `errors` entry says it `is read-only: it comes from PokeAPI or is managed by the service`. `PATCH` lists every such member. `POST` and `PUT` name the first one, because Jackson stops there.
- **Proprietary rules,** enforced by the domain value objects:
  - `region` at most 50 characters, `habitatNotes` at most 500 and `notes` at most 2000. Text is trimmed, and blank text becomes `null`.
  - `internalTags`: at most 20 tags, each 1–30 lower-case letters, digits or hyphens, starting with a letter or digit. Tags are lower-cased and de-duplicated.
  - `localizedNames`: at most 10 locales such as `pt-BR`, with names of 1–50 characters.
- **Every problem at once.** A request that breaks several rules gets one 400 listing each field (Notification pattern), for example `region` and `internalTags` together. Wrong JSON types and unknown members are named the same way.

## Caching and resilience

PokeAPI is a free public service: polite clients cache, and it sometimes stumbles. Every catalog read goes through a cache, and every PokeAPI request goes through a retry and a circuit breaker ([ADR-0003](docs/adr/0003-caffeine-cache-and-resilience4j-for-pokeapi.md)).

```mermaid
flowchart LR
  uc["ListPokemon / GetPokemonDetail"] --> cache["CachingPokemonCatalog<br/>(Spring Cache on Caffeine)"]
  cache -->|miss| adapter["PokeApiCatalogAdapter"]
  adapter --> client["PokeApiClient<br/>Retry → CircuitBreaker → HTTP"]
  client -->|HTTPS| pokeapi[("PokeAPI")]
  sync["SyncPokemon / CreateLocalPokemon"] -->|"fresh read, then write-through"| cache
```

**Cache:**
- **What's cached:** catalog pages (by `page` and `size`) and details. A detail is reachable by number **and** by name, so `/pokemon/pikachu` followed by `/pokemon/25` costs PokeAPI 3 requests, not 6.
- **Unknown Pokémon** are remembered as unknown, so typos don't reach PokeAPI again.
- **Limits:** entries live for `CATALOG_CACHE_TTL` (default 1 hour), with at most 2000 per cache.
- **Never cached:** failures, so the request after an outage asks PokeAPI again.
- **One load at a time:** simultaneous first requests for the same entry trigger **one** load.
- **The local replica never stores a cached copy.** Import, re-sync and create read PokeAPI directly and then refresh the cached detail, so after a re-sync the catalog shows the new data too.

**Resilience (Resilience4j, explicit decoration, no AOP):**

| Upstream failure | Retried? | Counts against the circuit? |
|---|---|---|
| Connection refused or reset, other I/O errors | yes | yes |
| 5xx, 429 | yes | yes |
| Timeout (2 s connect, 5 s read) | no: it has already cost the full timeout | yes |
| 404 | no (a normal answer) | no |
| Other 4xx, unreadable answer | no | yes |

- **Retry:** at most 3 attempts per request, waiting about 200 ms and then 400 ms, with jitter.
- **Circuit breaker:** when half of the last 20 calls failed (counting starts after 10), PokeAPI isn't called for 30 s. Requests answer 503 at once, then 3 trial calls decide whether to close it again.
- **Logs:** every retry (INFO) and every state change (WARN, for example `PokeAPI circuit breaker changed from CLOSED to OPEN`) is logged with the request's correlation id.

The integration tests prove each of these behaviours with WireMock request counts:
- a second visit makes zero requests
- a 503 blip makes two requests and still answers 200
- an open circuit makes zero requests

## Authentication

Stateless bearer tokens (JWT, HS256), issued by the API itself ([ADR-0002](docs/adr/0002-stateless-jwt-authentication.md)).

```bash
# 1. Register: every new account gets role USER; the role can't be chosen
curl -s -X POST localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"ash","email":"ash@pallet.town","password":"Pikachu@25"}'

# 2. Log in: returns accessToken, tokenType, expiresAt and the account
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"ash","password":"Pikachu@25"}' | jq -r .accessToken)

# 3. Call protected routes with the token
curl -s localhost:8080/api/v1/auth/me -H "Authorization: Bearer $TOKEN"
```

**Account rules:**
- **Usernames:** 3–30 letters, digits, `.`, `_` or `-`.
- **Email:** a valid address.
- **Case-insensitive:** both usernames and emails are case-insensitive and unique; a duplicate returns 409 naming the field.
- **Passwords:** 8–72 characters, with upper case, lower case, a digit and a special character. Every broken rule is listed in `errors`.
- **Storage:** passwords are stored only as BCrypt hashes.

**Login failures:** they all return the same `401 Invalid username or password.`, and unknown usernames still cost one hash comparison, so neither the response nor its timing reveals which accounts exist.

| Route | Who |
|---|---|
| `GET /api/v1/pokemon/**`, `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, Swagger/OpenAPI, health, info | anyone |
| `/api/v1/local-pokemon/**`, `GET /api/v1/auth/me` | valid bearer token (401 otherwise, with `WWW-Authenticate: Bearer`) |
| `DELETE /api/v1/local-pokemon/**` | role `ADMIN` (403 for `USER`) |

Unknown routes still return 404. The [demo accounts](#demo-data) `admin / Admin@123` and `trainer / Trainer@123` are ready when demo data is enabled.

## Error format

Every error is an RFC 7807 problem detail (`application/problem+json`) with exactly these members: `type`, `title`, `status`, `detail`, `instance`, `timestamp` and `errors`. That holds for errors from controllers, from Spring MVC, from servlet filters (such as CORS) and from the servlet container.

```json
{
  "type": "https://github.com/aleferod/pokedex/blob/main/backend/docs/problems.md#not-found",
  "title": "Resource not found",
  "status": 404,
  "detail": "No endpoint GET /api/v1/nope.",
  "instance": "/api/v1/nope",
  "timestamp": "2026-10-14T14:43:25.712Z",
  "errors": []
}
```

- **Problem types:** each `type` links to its section in [docs/problems.md](docs/problems.md).
- **No internals:** stack traces, exception names and SQL are never returned.
- **Unexpected failures:** these return a generic 500 that quotes the request's **correlation id**.
- **Correlation id:** every response carries an `X-Correlation-Id` header. A caller-supplied id is echoed if it is 1–64 characters of letters, digits and hyphens; otherwise a UUID is generated. Every log line for the request includes it.

## Architecture

Ports & Adapters. Dependencies point inward: **adapter → application → domain**. `infrastructure` wires everything together, and nothing depends on it.

```mermaid
flowchart LR
  client(["pokedex-web / any HTTP client"]) -->|"HTTP /api/v1"| web
  subgraph IN["adapter.in.web"]
    web["Controllers, DTOs, mappers<br/>CorrelationIdFilter, CORS filter<br/>GlobalExceptionHandler (RFC 7807)"]
  end
  subgraph APP["application (framework-free)"]
    portIn[["port.in: one interface per use case"]]
    usecases["usecase: one class per use case"]
    portOut[["port.out: PokemonCatalogPort, LocalPokemonRepositoryPort,<br/>UserRepositoryPort, PasswordHasherPort, TokenPort"]]
  end
  subgraph DOM["domain (JDK only)"]
    model["Entities, value objects,<br/>domain services, domain exceptions"]
  end
  subgraph OUT["outbound adapters"]
    cache["adapter.out.cache<br/>Caffeine-backed Decorator"]
    pokeapi["adapter.out.pokeapi<br/>RestClient + retry/circuit breaker,<br/>anti-corruption mapper"]
    persistence["adapter.out.persistence<br/>JPA entities, Spring Data, mappers"]
  end
  infra["infrastructure<br/>Spring config, security, wiring"]
  web --> portIn
  usecases -. implements .-> portIn
  usecases --> portOut
  usecases --> model
  cache -. implements .-> portOut
  cache --> pokeapi
  pokeapi -. implements .-> portOut
  persistence -. implements .-> portOut
  pokeapi -->|HTTPS| ext[("PokeAPI")]
  persistence --> db[("PostgreSQL 16")]
  infra -. wires .-> web
  infra -. wires .-> usecases
  infra -. wires .-> pokeapi
  infra -. wires .-> persistence
```

| Package (`com.ballastlane.pokedex…`) | May depend on | Responsibility |
|---|---|---|
| `domain` | JDK only | Business model and rules |
| `application.port.in` / `.port.out` / `.usecase` | `domain`, JDK | Use cases and the ports they expose and need |
| `adapter.in.web` | application, domain, Spring | REST, DTOs, error translation, correlation id, CORS |
| `adapter.out.cache` | application, domain, Spring Cache | Caching Decorator for the catalog port |
| `adapter.out.pokeapi` | application, domain, Spring, Resilience4j | PokeAPI client with retry and circuit breaker, and the anti-corruption layer |
| `adapter.out.persistence` | application, domain, Spring/JPA | PostgreSQL persistence |
| `infrastructure` | everything | Configuration and wiring |

**Enforced, not just documented:** [`ArchitectureTest`](src/test/java/com/ballastlane/pokedex/architecture/ArchitectureTest.java) fails the build on any violation, and [`ArchitectureRulesSelfTest`](src/test/java/com/ballastlane/pokedex/architecture/ArchitectureRulesSelfTest.java) proves each rule catches a deliberately broken fixture. The rules also ban field injection, `System.out` and `java.util.logging`. The single-module-plus-ArchUnit trade-off is explained in [ADR-0001](docs/adr/0001-clean-hexagonal-architecture.md).

## Design patterns

| Pattern | Where | Status |
|---|---|---|
| Ports & Adapters | package layout, `application.port.in/out` | ✅ |
| Template Method | `GlobalExceptionHandler` overrides `ResponseEntityExceptionHandler#createResponseEntity` | ✅ |
| Factory | `ProblemDetailFactory`: the single place that builds problem details | ✅ |
| Chain of Responsibility | servlet filter chain: `CorrelationIdFilter` → `ProblemDetailCorsFilter` → DispatcherServlet | ✅ |
| Specification | `ArchitectureRules`: named, reusable architecture rules | ✅ |
| Value Object | `PokemonId`, `PokemonIdentifier` (sealed: by id or by name), `Weight`, `Height`, `BaseStats`, `PageRequest`, `Username`, `EmailAddress`, `UserId`, `PasswordHash`, `CorsProperties`, `JwtProperties`, `PokeApiProperties` | ✅ |
| Factory | `User.register` (always `USER`) / `User.restore`; `LocalPokemon.importFrom` | ✅ |
| Aggregate | `LocalPokemon` (`importFrom`, `resyncWith`: upstream replaced, proprietary data kept) | ✅ |
| Optimistic Offline Lock | `version` / `@Version` surfaced as `ETag`; `If-Match` → `ExpectedVersion`, checked by the use case and again by JPA at save | ✅ |
| Notification | `ProprietaryData.of` collects every rule violation into `InvalidFieldsException`; `MergePatchReader` does the same for read-only, unknown and wrongly typed members | ✅ |
| Merge Patch (tri-state field) | `FieldPatch` (sealed: `Keep`, `Clear`, `Set`), `LocalizedNamesPatch`, `ProprietaryPatch` | ✅ |
| Specification-style filter | `LocalPokemonFilter` → native query that uses the GIN tag index | ✅ |
| Chain of Responsibility | `PasswordPolicy` → `PasswordRule` chain (length, character classes) | ✅ |
| Repository | `UserRepositoryPort` ← `UserPersistenceAdapter`, `LocalPokemonRepositoryPort` ← `LocalPokemonPersistenceAdapter` (Spring Data) | ✅ |
| Adapter | `BCryptPasswordHasher`, `JwtTokenIssuer` behind application ports | ✅ |
| Adapter / Anti-Corruption Layer | `PokeApiClient` + `dto/*` + `PokeApiMapper` behind `PokemonCatalogPort`: PokeAPI's shapes never leave `adapter.out.pokeapi` | ✅ |
| Bulkhead | `BoundedFanOut`: virtual threads behind one shared semaphore | ✅ |
| Strategy | `EvolutionConditionStrategy` (`LevelUpCondition`, `UseItemCondition`, `TradeCondition`, `FallbackCondition`), chosen per PokeAPI trigger by `EvolutionConditions`. The chain itself stays a tree rather than being flattened, so branching isn't lost. | ✅ |
| Composite-style tree | `EvolutionStage` / `EvolutionStageResponse` (recursive) | ✅ |
| Decorator | `CachingPokemonCatalog` around `PokemonCatalogPort` (Spring Cache on Caffeine); the use cases can't tell | ✅ |
| Retry with exponential backoff and jitter | `PokeApiResilience` (Resilience4j), only for failures `PokeApiClient` marks transient | ✅ |
| Circuit Breaker | `PokeApiResilience`: fail fast while PokeAPI is down; 404 never counts | ✅ |
| DTO + Mapper | `AuthWebMapper`, `PokemonWebMapper`, `LocalPokemonWebMapper`, `UserPersistenceMapper` (MapStruct), `LocalPokemonPersistenceMapper` | ✅ |

## Tech stack

| Choice | Why |
|---|---|
| Java 21, Spring Boot 4.1 | Current supported Boot line; 3.5 is out of OSS support ([ADR-0006](docs/adr/0006-spring-boot-4.md)) |
| Spring Web MVC + `ProblemDetail` | Standard RFC 7807 errors |
| PostgreSQL 16 + Flyway | The schema is owned by versioned migrations; Hibernate runs `ddl-auto=validate` only |
| Spring Security 7 OAuth2 resource server | Standard JWT verification and role mapping; no hand-written token parsing |
| MapStruct | Mapping generated at compile time; an unmapped field fails the build |
| `RestClient` on the JDK `HttpClient` + virtual threads | Blocking code that reads simply, with cheap parallel fan-out; no reactive stack for one integration |
| springdoc-openapi | OpenAPI 3 document and Swagger UI generated from the code |
| Actuator | Health, liveness/readiness probes and build info; nothing else is exposed |
| JUnit 6, AssertJ, Mockito, ArchUnit, Testcontainers | Fast unit tests, real-database integration tests, architecture tests ([ADR-0004](docs/adr/0004-testcontainers-over-in-memory-databases.md)) |
| Spotless (google-java-format), JaCoCo, Maven Enforcer | Formatting, coverage and dependency gates in every build |
| Docker (multi-stage, layered, non-root) + Compose | One-command local stack; the host needs only Docker |
| Spring Cache + Caffeine | In-process cache with TinyLFU eviction and per-key loading; one class to swap for Redis later ([ADR-0003](docs/adr/0003-caffeine-cache-and-resilience4j-for-pokeapi.md)) |
| Resilience4j (retry, circuit breaker) | Core modules decorated explicitly, with no AOP, configured by validated properties ([ADR-0003](docs/adr/0003-caffeine-cache-and-resilience4j-for-pokeapi.md)) |

## Configuration

Everything that varies between environments comes from environment variables. `docker compose` and the `local` profile both read them from `.env`. Use [.env.example](.env.example) as the template; never commit `.env`.

| Variable | Default | Used for |
|---|---|---|
| `DB_NAME` / `DB_USERNAME` | `pokedex` | Database name and user |
| `DB_PASSWORD` | **none (required)** | Database password. Startup fails naming it if it's missing. |
| `DB_PORT` | `5433` | Host port where Postgres is published (local profile) |
| `DB_URL` | derived | Optional JDBC URL override for the local profile |
| `SERVER_PORT` | `8080` | Host port of the API |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated exact origins allowed to call `/api/**`. Wildcards are rejected at startup. |
| `POKEAPI_BASE_URL` | `https://pokeapi.co/api/v2` | Upstream catalog. Timeouts (2 s connect, 5 s read), the concurrency cap (8), retries (`app.pokeapi.retry.*`) and the circuit breaker (`app.pokeapi.circuit-breaker.*`) are in `application.yml`. |
| `SEED_DEMO_DATA` | `false` (`true` in `.env.example` and Compose) | Seed the [demo data](#demo-data) at startup. The file is `app.seed.location`. |
| `CATALOG_CACHE_TTL` | `PT1H` | How long catalog pages and details are reused (ISO-8601). The size limit is `app.catalog-cache.max-entries` (2000 per cache). |
| `JWT_SECRET` | **none (required)** | HMAC key that signs access tokens, at least 32 bytes. Startup fails naming it if it's missing or short. |
| `JWT_TTL` | `PT1H` | Access-token lifetime (ISO-8601 duration) |
| `BCRYPT_STRENGTH` | `10` | BCrypt cost factor |

| Profile | When |
|---|---|
| `local` (default) | Running on the host; imports `.env` |
| `docker` | Inside the Compose stack; the database is the `postgres` service, port 8080 |
| `test` | Automated tests; the database comes from Testcontainers |

## Testing and quality gates

```bash
# Everything, inside Docker (host needs only Docker): compile, all tests, every gate
./scripts/verify-in-docker.sh

# Same on the host (JDK 21 + Docker for Testcontainers)
./mvnw verify

# One test class / reformat code / coverage table
./scripts/verify-in-docker.sh -Dtest=CorrelationIdFilterTest -Dsurefire.failIfNoSpecifiedTests=false -DskipITs
./mvnw spotless:apply
./scripts/coverage-summary.sh        # after a verify; HTML report: target/site/jacoco/index.html
```

`verify-in-docker.sh` mounts the Docker socket and uses `--network host`, so Testcontainers can start PostgreSQL next to the build. On **Docker Desktop** (macOS or Windows), run `./mvnw verify` on the host instead.

| Layer | Tooling | Examples |
|---|---|---|
| Unit (domain, use cases) | JUnit 6, AssertJ, Mockito | `PasswordPolicyTest`, `RegisterUserUseCaseTest`, `AuthenticateUserUseCaseTest` |
| Unit (adapters, config) | JUnit 6, AssertJ | `JwtTokenIssuerTest`, `BCryptPasswordHasherTest`, `CorsPropertiesTest` |
| Web slice | `@WebMvcTest` via [`@WebSliceTest`](src/test/java/com/ballastlane/pokedex/support/WebSliceTest.java), with real tokens from [`TestTokens`](src/test/java/com/ballastlane/pokedex/support/TestTokens.java) | `AuthControllerTest`, `AccessControlTest`, `InputAndDomainErrorsTest` |
| Persistence (`*IT`) | `@DataJpaTest` + Testcontainers | `UserPersistenceAdapterIT`, `LocalPokemonPersistenceAdapterIT` (arrays, localized names, version, duplicates) |
| PokeAPI adapter | WireMock serving trimmed real captures ([`src/test/resources/pokeapi`](src/test/resources/pokeapi/README.md)) | `PokeApiCatalogAdapterTest` (pages and details: mapping, 404 vs unavailable, refused connection, read timeout, User-Agent, parallel speed-up), `PokeApiDetailMapperTest` (Bulbasaur, Eevee, Pikachu, Ditto), `EvolutionConditionsTest`, `FlavorTextTest`, `BoundedFanOutTest` |
| Integration (`*IT`, Failsafe) | `@SpringBootTest` + Testcontainers PostgreSQL 16 via [`@IntegrationTest`](src/test/java/com/ballastlane/pokedex/support/IntegrationTest.java) (WireMock stands in for PokeAPI) | `LocalPokemonSyncIT` (incl. a real concurrent first-import race), `PokemonCatalogIT`, `PokemonDetailIT`, `AuthFlowIT`, `ShortLivedTokenIT`, `OperationalEndpointsIT`, `SchemaMigrationIT` |
| Architecture | ArchUnit | `ArchitectureTest`, `ArchitectureRulesSelfTest` |

Test names follow `should_<expected>_when_<condition>`, with Given / When / Then bodies. Tests are written first: each change's pull request shows a failing `test:` commit before the `feat:` commit that makes it pass.

**Build gates.** Any of these fails `verify`, locally and in CI:
- compiler warnings (`-Xlint:all -Werror`)
- formatting (Spotless)
- architecture rules (ArchUnit)
- line coverage of `domain` + `application` below 85% (JaCoCo)
- banned dependencies: H2/HSQLDB/Derby and JUnit 4 (Maven Enforcer)

**CI** ([`.github/workflows/ci.yml`](../.github/workflows/ci.yml), one workflow for both apps) runs on every push to `main` and every pull request:
- build and all tests, with a coverage summary and the HTML report as an artifact
- `openspec validate --all --strict`
- a Docker image build
- a gitleaks secret scan over the full history
- the frontend's gates, and the end-to-end suite against the full stack built from the same commit, so an API change that breaks the app fails here

## Spec-driven workflow (OpenSpec)

Every capability is specified before it's built ([ADR-0005](docs/adr/0005-spec-driven-development-with-openspec.md)):
- **Project context and rules:** [`openspec/config.yaml`](../openspec/config.yaml) at the repository root, shared with the frontend, covering the stack, architecture rules, conventions and Definition of Done.
- **In-flight changes:** [`openspec/changes/`](../openspec/changes). Each has a proposal, a design, tasks (tests first) and delta specs with `SHALL` requirements and `WHEN/THEN` scenarios.
- **Source of truth after archiving:** [`openspec/specs/`](../openspec/specs).

The process for each change:
1. Propose.
2. `openspec validate <change> --strict`.
3. Implement test-first.
4. Archive (as the last commit of the PR).
5. Squash-merge once CI is green.

## Architecture decisions

See [docs/adr](docs/adr/README.md). Written so far:
- 0001: Clean/Hexagonal Architecture in one module, enforced by ArchUnit
- 0002: Stateless JWT authentication issued by the API
- 0003: In-process Caffeine cache and Resilience4j in front of PokeAPI
- 0004: Testcontainers instead of H2
- 0005: OpenSpec
- 0006: Spring Boot 4.1

## GenAI exercise and AI use

[docs/GENAI.md](docs/GENAI.md) contains:
- the exact prompt for generating a Task Management REST API
- the generated code, in [docs/genai-sample/](docs/genai-sample/): a standalone project that CI builds and tests
- how that output was validated, with twelve corrections (five just to make it build and run)
- an honest account of how this API was built with an AI pair: the OpenSpec guardrails, and the real AI mistakes caught along the way

## Roadmap

Each item is one OpenSpec change and one pull request:

1. ✅ `bootstrap-project`: platform foundation
2. ✅ `add-user-auth`: register/login (JWT), users table, public vs protected routes, roles
3. ✅ `add-pokemon-catalog`: US01 paginated catalog with parallel detail fetching
4. ✅ `add-pokemon-detail`: US02 stats, description, evolution chain
5. ✅ `add-local-sync`: US03 import into PostgreSQL with proprietary fields
6. ✅ `add-local-crud`: US04 CRUD with validation, ETags and optimistic locking
7. ✅ `add-caching-resilience`: Caffeine cache, timeouts, retry and circuit breaker
8. ✅ `add-seed-data`: demo users and 10 local Pokémon
9. ✅ `add-genai-task-api-doc`: the GenAI exercise write-up (`docs/GENAI.md`)
10. ✅ `add-presentation-doc`: the presentation and code-review guide ([`docs/PRESENTATION.md`](../docs/PRESENTATION.md)), and the full stack from pokedex-web in the quick start
11. ✅ `add-catalog-name-search`: US01 search by part of a name (`?name=pika`), over a cached index of all names
12. ✅ `update-guide-for-catalog-search`: the guide's demo and numbers after the search, and the fixed form finding in `docs/GENAI.md`
13. ✅ `adopt-monorepo`: this API moved into [aleferod/pokedex](../README.md) as `backend/`, next to the web app; one CI, the full stack at the root, and this repository's URL in the `User-Agent` and problem types

Changes 1–12 were pull requests in [aleferod/pokedex-api](https://github.com/aleferod/pokedex-api/pulls?q=is%3Apr+is%3Amerged).

## Trade-offs and future improvements

- **One module plus ArchUnit, not multi-module Maven.** Easier to read and review. The layering is enforced when tests run, not when code compiles. See ADR-0001.
- **Empty `V1__baseline.sql`.** Tables arrive with the feature that needs them, so the history reads one capability per change.
- **Squash merges.** `main` stays linear. The red/green TDD commits remain visible in each pull request: in [aleferod/pokedex-api](https://github.com/aleferod/pokedex-api/pulls?q=is%3Apr+is%3Amerged) for the changes before the move to this repository.
- **409 instead of 412 for a stale `If-Match`.** The exercise asks for 409. The 412 problem type stays documented but isn't returned.
- **Native SQL for the local list.** The tag filter uses PostgreSQL array containment so the GIN index serves it. That ties the query to PostgreSQL, which is the only database, and Testcontainers runs the real one. `name` matches the PokeAPI name only; searching localized names is future work.
- **In-process cache.** Each instance has its own cache, and catalog data can be up to one TTL old (the local replica always reads fresh). A shared Redis cache behind the same Decorator is the next step (ADR-0003).
- **Retries add latency on failure.** That's at most about 0.6 s, and only for fast-failing errors; timeouts aren't retried. The JDK HTTP client may also replay a GET once by itself when a connection drops.
- **Demo data through the use cases, not SQL.** Seeding needs PokeAPI and a few seconds at startup, but it never puts known passwords into a database that didn't ask for them, and it never copies upstream data by hand.
- **Auth limits:** HS256 shared secret, no refresh tokens or revocation, no login rate limiting. Each is mitigated and has a next step in ADR-0002.
- **Future work:**
  - structured JSON logs and distributed tracing (Micrometer/OpenTelemetry)
  - a Redis cache for multi-instance deployments, and serving stale entries while PokeAPI is down (stale-if-error)
  - image publishing and a deploy pipeline
  - contract tests shared with the frontend

## License

[MIT](../LICENSE)
