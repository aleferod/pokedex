# Design: adopt-monorepo

## Context

This repository's setup commits, on `main` before this change:
1. the root `.gitignore`, `.gitleaks.toml` (pokedex-api's single exception, for its old and new path) and `LICENSE` (both repositories had the same MIT license)
2. `git subtree add` of aleferod/pokedex-api `main` under `backend/`, and of aleferod/pokedex-web `main` under `frontend/`. Each is a merge that keeps every imported commit and its SHA.
3. one `openspec/` at the root: the specs and archives moved without changes to their text, and one project config

Both originals squash-merged their pull requests, so their `main` holds one commit per change. The red/green commits of each change live in that repository's pull request.

## Decisions

### D1. Layout
```
backend/     pokedex-api: Spring Boot, its own README, Dockerfile, docker-compose.yml (API + database), scripts, docs
frontend/    pokedex-web: React, its own README, Dockerfile, scripts, e2e tests
docs/        PRESENTATION.md: the presentation and review guide, for both apps
openspec/    one config, the specs of both apps, and every archived change
docker-compose.yml, .env.example    the whole product
.github/workflows/ci.yml            one pipeline
```
- **Each app stays self-contained.** Its scripts resolve paths from their own folder, so `backend/scripts/verify-in-docker.sh` and `frontend/scripts/npm-in-docker.sh` work unchanged, and each image builds from its own folder.
- **Rejected:** a build tool spanning both apps (Maven reactor, npm workspaces, Nx). The two stacks share nothing at build time. Docker and Compose already join them.

### D2. Paths in specs
The specs were written for one app per repository. Rather than rewriting them, a path in a capability's spec is read from that capability's app folder:
- **frontend capabilities:** `api-client`, `app-shell`, `authentication-ui`, `catalog-ui`, `local-pokemon-ui`, `pokemon-detail-ui`, `web-delivery` and `web-quality`, read from `frontend/`
- **repository-wide capabilities:** `repository-layout` and `project-presentation`, read from the repository root
- **every other capability is the backend's,** read from `backend/`

For example, `docs/genai-sample/` in `genai-exercise` is `backend/docs/genai-sample/`. A path that starts at the repository root says so.

### D3. The full stack at the root
- **The file:** `docker-compose.yml` at the root is `frontend/docker-compose.full.yml` with build contexts `./backend` and `./frontend`. Plain `docker compose up --build` then works from the root.
- **Kept the same:** the project name `pokedex-full`, the ports, the healthy-before-ready chain and the variables. That leaves it distinct from `backend/docker-compose.yml` (project `pokedex`, API and database only).
- **`.env.example` at the root** holds the full stack's variables, moved from `frontend/.env.example`, which keeps only the Vite variables.
- **`API_CONTEXT` is gone:** the API is always the checkout's own.

### D4. End-to-end tests
`frontend/scripts/e2e.sh` stays with Playwright and its tests, and runs the root compose file with the root `.env`, as Compose project `pokedex-e2e`. A pull request that changes both apps is tested as one. Before, the suite ran a pull request's frontend against the other repository's `main`.

### D5. One CI workflow
`.github/workflows/ci.yml` carries the jobs of both former workflows, each with its working directory:

| Job | From | Runs in |
|---|---|---|
| Backend: build and test | pokedex-api `build` | `backend/` |
| Backend: GenAI sample | pokedex-api `genai-sample` | `backend/` |
| Frontend: lint, type-check, test, build | pokedex-web `quality` | `frontend/` |
| Images | both `docker` jobs, as a matrix over the two contexts | root |
| End-to-end | pokedex-web `e2e` | root, then `frontend/` |
| Specs | both `openspec` jobs, once | root |
| Secret scan | both `secrets` jobs, once, with the root config | root |

- **Same as before:** the actions, versions, cache keys (`cache-dependency-path`) and artifacts.
- **Rejected: path filters.** They would skip jobs whose app didn't change, but a change in one app can break the other through the API contract, which the end-to-end job checks. The pipeline takes about as long as the slower of the two did before.

### D6. The API's repository URLs
Two default values name the repository: the `User-Agent` for PokeAPI (`application.yml`, and the fallback in `PokeApiProperties`), and `app.problems.base-uri`.
- `api-errors` requires each problem `type` URI to resolve to its documentation. That now lives at `backend/docs/problems.md` in this repository. The old URL would keep resolving, to a frozen copy in the archived repository.
- So both become `https://github.com/aleferod/pokedex…`, driven by tests that pin the exact defaults:
  - `ProblemTypesDocumentedTest` pins the base URI
  - `PokeApiPropertiesTest` pins the fallback user agent
  - a new assertion checks the user agent in `application.yml`
- **Impact on clients:** the problem `type` URI changes. pokedex-web reads only its fragment (`ApiError.type`), which is unchanged. Its test fixtures are updated to stay realistic.

### D7. Docs and links
- **The root README** is the entry point. It covers:
  - what the product is
  - the layout
  - the one-command quick start
  - the demo accounts
  - each app's README, the presentation guide, the GenAI write-up and the specs
  - where the history is
- **Links between the apps** become relative. Links to the original repositories' pull requests, commits and CI runs stay as they are, because that is where the red/green commits are.
- **The archived changes are records and are not edited.** Their links point at the original repositories, which stay readable once archived.

## Risks / Trade-offs

- [`git log` doesn't follow files into the imported history] → `git log -- backend/<path>` stops at the import merge. The README shows how to read an app's earlier history: `git log <import-merge>^2 -- <path>`, or the original repository.
- [CI for one app runs on changes to the other] → a few minutes per push. It's the same cost as before, and it buys end-to-end tests of every commit.
- [The problem `type` URIs change] → no client depends on the full URI. The pokedex-web client keeps only the fragment.
