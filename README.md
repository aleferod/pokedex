# Pokédex

[![CI](https://github.com/aleferod/pokedex/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/aleferod/pokedex/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-6db33f)
![React 19](https://img.shields.io/badge/React-19-61dafb)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A full-stack Pokédex for the Ballast Lane Java technical exercise:
- **The API** ([`backend/`](backend)) is Java 21 and Spring Boot 4.1, with Clean / Hexagonal Architecture, PostgreSQL and JWT. It wraps [PokeAPI](https://pokeapi.co/docs/v2) and keeps a local replica with proprietary fields.
- **The web app** ([`frontend/`](frontend)) is React 19 with strict TypeScript, Vite, TanStack Query and Tailwind.

Both are built **spec-first** with [OpenSpec](https://github.com/Fission-AI/OpenSpec) and **test-first**, and everything runs in Docker.

## Contents

- [Quick start](#quick-start)
- [What's in the repository](#whats-in-the-repository)
- [User stories](#user-stories)
- [Quality gates and CI](#quality-gates-and-ci)
- [Where to read further](#where-to-read-further)
- [History](#history)

## Quick start

You only need **Docker** (with Compose v2):

```bash
git clone https://github.com/aleferod/pokedex.git && cd pokedex
cp .env.example .env && docker compose up --build
```

| What | Where |
|---|---|
| The app | <http://localhost:8000> |
| Swagger UI (the API) | <http://localhost:8080/swagger-ui.html> |
| Demo accounts | `trainer / Trainer@123` (browse and edit) and `admin / Admin@123` (also delete) |

- **What it starts:** PostgreSQL 16, the API and the web app, built from this checkout. Every service has a health check, so `docker compose up --build --wait` returns once the product works.
- **Demo data:** two demo accounts and ten Pokémon in the local Pokédex.
- **One origin:** the app reaches the API only through its own `/api` proxy.
- **Secrets:** they come only from `.env`, which git ignores. [`.env.example`](.env.example) holds placeholders that are safe only for local use. Without a `.env`, Compose stops and names the missing variable.
- **Stopping:** `docker compose down` keeps the data, and `docker compose down -v` removes it.

## What's in the repository

```
backend/                  pokedex-api: the REST API (Java 21, Spring Boot 4.1)
frontend/                 pokedex-web: the web app (React 19, TypeScript)
docs/PRESENTATION.md      the presentation and code-review guide
openspec/                 the specs of both apps, and every change, proposed and archived
docker-compose.yml        the whole product: database, API and app
.github/workflows/ci.yml  one pipeline for both apps
```

Each app builds, tests and runs on its own, with only Docker installed:

| | Backend | Frontend |
|---|---|---|
| Build and test | `backend/scripts/verify-in-docker.sh` | `frontend/scripts/npm-in-docker.sh ci`, then `frontend/scripts/npm-in-docker.sh test` |
| Run alone | in `backend/`: `cp .env.example .env && docker compose up --build` (API and database) | `frontend/scripts/npm-in-docker.sh run dev` (proxies `/api` to the API on :8080) |
| Read more | [backend/README.md](backend/README.md) | [frontend/README.md](frontend/README.md) |

The end-to-end tests drive a real browser against a fresh full stack: run `frontend/scripts/e2e.sh` after `cp .env.example .env` here.

## User stories

| Story | The API | The app |
|---|---|---|
| US01 Paginated catalog | `GET /api/v1/pokemon?page&size`, and `?name=pika` to search by part of a name | the catalog with paging in the URL, and search as you type |
| US02 Detail view | `GET /api/v1/pokemon/{idOrName}`: stats, description and the evolution tree | a Pokémon's page, with its evolution chain |
| US03 Local replica | `POST /api/v1/local-pokemon/sync/{idOrName}` imports or re-syncs, keeping proprietary fields | My Pokédex: sync from PokeAPI |
| US04 Manage the replica | `GET`, `POST`, `PUT`, `PATCH` (JSON Merge Patch) and `DELETE` on `/api/v1/local-pokemon`, with `If-Match`, and 409 on a stale version | list, filters, tags, edit with conflict recovery, and delete for admins |
| Accounts | `POST /api/v1/auth/register`, `POST /api/v1/auth/login` (JWT) | registration, sign-in, protected pages |

The full endpoint table is in the [backend README](backend/README.md#user-stories-and-endpoints).

## Quality gates and CI

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on every push to `main` and every pull request:
- **The backend:** compiled with `-Xlint:all -Werror`, unit, slice and Testcontainers tests, ArchUnit layering rules, JaCoCo (domain and application ≥ 85 %), Spotless, and the GenAI sample's own build.
- **The frontend:** zero-warning ESLint, Prettier, strict TypeScript, Vitest with coverage (a test fails on any console output), and the production build.
- **Both images**, and the **end-to-end suite** (Playwright with axe) against the full stack built from the same commit.
- `openspec validate --all --strict` and a **gitleaks** scan of the full history.

## Where to read further

- **[The presentation and code-review guide](docs/PRESENTATION.md):** the demo, the architecture, the decisions, the quality evidence and where to start reading the code.
- **[The backend README](backend/README.md):** endpoints, architecture, design patterns, caching and resilience, and trade-offs, with the [ADRs](backend/docs/adr).
- **[The frontend README](frontend/README.md):** project structure, state management, each page, and the tests.
- **[The GenAI exercise and how AI was used](backend/docs/GENAI.md):** the prompt, the generated sample with every correction, and the real AI mistakes caught while building this.
- **The specs:** [`openspec/specs/`](openspec/specs), one folder per capability, with the project's rules in [`openspec/config.yaml`](openspec/config.yaml). Every change is in [`openspec/changes/archive/`](openspec/changes/archive).

## History

The two apps were built in their own repositories, which are now archived: [aleferod/pokedex-api](https://github.com/aleferod/pokedex-api) and [aleferod/pokedex-web](https://github.com/aleferod/pokedex-web). **Their pull requests show each change's failing `test:` commit before the commit that makes it pass**: [pokedex-api's](https://github.com/aleferod/pokedex-api/pulls?q=is%3Apr+is%3Amerged) and [pokedex-web's](https://github.com/aleferod/pokedex-web/pulls?q=is%3Apr+is%3Amerged).

This repository imported both `main` branches with every commit unchanged, so for example `git show 4e85c6b` shows the API's catalog search. `git log -- backend/<path>` stops at the import, so to follow a file further back, read the imported branch directly:

```bash
git log 10ab518^2 -- README.md                                   # backend/README.md before the move
git log d5def31^2 -- src/features/catalog/FindPokemonForm.tsx    # a frontend file before the move
```

## License

[MIT](LICENSE)
