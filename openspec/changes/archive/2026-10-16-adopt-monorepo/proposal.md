# Proposal: adopt-monorepo

## Why

The engineering requirement asks for the whole solution in one GitHub repository. Until now the product was split in two: aleferod/pokedex-api (Spring Boot) and aleferod/pokedex-web (React). Reviewers had to clone both, and the full stack built the API from the other repository's `main`, so a pull request in one repository couldn't test the other's matching change.

The setup commits of this repository already brought both in:
- each app's `main` history imported under `backend/` and `frontend/` with every commit SHA unchanged
- the 26 specs and 20 archived changes in one root `openspec/`

Neither app runs as one repository yet:
- **CI:** the two workflows sit in `backend/.github/` and `frontend/.github/`, where GitHub ignores them, so nothing runs.
- **The full stack** still lives in `frontend/` and builds the API from GitHub.
- **The docs** link across repositories.

This serves the auxiliary requirement "Docker, README and repository" and touches both apps.

## What Changes

- **One repository, two self-contained apps.**
  - `backend/` and `frontend/` each keep their own README, Dockerfile, scripts and tests, and build, test and run on their own.
  - The root holds what spans both: the full stack, CI, OpenSpec, the README and the presentation guide.
- **The full stack at the root.**
  - `cp .env.example .env && docker compose up --build` from the repository root builds both apps from the same checkout.
  - It replaces `frontend/docker-compose.full.yml`, and `API_CONTEXT` goes away.
  - The end-to-end suite (`frontend/scripts/e2e.sh`) uses it, so it always tests the API and the app of the same commit.
- **One CI workflow** (`.github/workflows/ci.yml`) runs every job of both former workflows on every push and pull request:
  - backend: build and tests, the GenAI sample, the image
  - frontend: quality gates and production build, the image
  - end-to-end tests
  - `openspec validate --all --strict` once, and one gitleaks scan of the whole history

  The per-app workflows and `backend/.gitleaks.toml` are removed; the root config already covers both.
- **The API names its new home.** The `User-Agent` sent to PokeAPI and the base of every problem `type` URI point to this repository. A `type` URI must resolve to the problem's documentation (`api-errors`), and that now lives at `backend/docs/problems.md` here.
- **Docs:**
  - a root README: what the product is, the layout, the one-command quick start, the demo accounts, and where to read further
  - `docs/PRESENTATION.md` moves to the root, since it covers both apps
  - links between the apps become relative
  - links to the original repositories' pull requests stay, since those hold the red/green commits

## Capabilities

### New Capabilities
- `repository-layout`: one repository with two self-contained apps; how paths in each app's specs are read; the imported history.

### Modified Capabilities
- `web-delivery`: "Full stack in one command" runs from the repository root and builds both apps from the same checkout.
- `web-quality`: "Continuous integration" is one workflow for both apps, and the end-to-end suite tests the API of the same commit.
- `project-presentation`: the guide lives at the root and is followed from a clone of this repository.

## Out of Scope

- Changing either app's code beyond the repository URLs, or their names (`pokedex-api`, `pokedex-web`), Maven or npm coordinates, image names or ports.
- Running only the jobs of the app that changed (path filters). Every job runs on every push, as before.
- Rewriting history (`git filter-repo`). The red/green commits stay in the original repositories' pull requests.
- Archiving the original repositories. That is done after this change is merged: each gets a pointer to this repository, then is archived.

## Impact

- **Files:** root `docker-compose.yml`, `.env.example`, `.github/workflows/ci.yml`, `README.md` and `docs/PRESENTATION.md`; in `backend/` the configuration, `pom.xml` and docs; in `frontend/` `scripts/e2e.sh`, `.env.example`, the README and two test fixtures. Removed: `backend/.github/`, `frontend/.github/`, `backend/.gitleaks.toml` and `frontend/docker-compose.full.yml`.
- **Behaviour:** the API's `User-Agent` and problem `type` URIs change. The web app reads only the URI's fragment, which is unchanged.
