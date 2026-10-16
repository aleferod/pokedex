# repository-layout Specification

## Purpose
How the repository is organised: the backend and the frontend side by side, each building, testing and running on its own, with what spans both at the root (the full stack, CI, specs and docs); how paths in each capability's spec are read; and the history imported from the two original repositories.

## Requirements

### Requirement: One repository, two apps
The repository SHALL hold the backend (pokedex-api) in `backend/` and the frontend (pokedex-web) in `frontend/`. Each SHALL build, test and run on its own, with its own README, Dockerfile and scripts. The root SHALL hold what spans both: the full stack, the CI workflow, the OpenSpec specs, the README and the presentation guide.

#### Scenario: Each app on its own
- **WHEN** a developer runs `backend/scripts/verify-in-docker.sh`, or `frontend/scripts/npm-in-docker.sh ci` and then `frontend/scripts/npm-in-docker.sh test`
- **THEN** that app builds and its tests run, with no step in the other app's folder

#### Scenario: The README
- **WHEN** a reviewer opens the root README
- **THEN** it explains the layout, starts the whole product with one command, and links each app's README, the presentation guide and the specs

### Requirement: Paths in specs
A path in a capability's spec SHALL be read from that capability's app folder:
- `frontend/` for `api-client`, `app-shell`, `authentication-ui`, `catalog-ui`, `local-pokemon-ui`, `pokemon-detail-ui`, `web-delivery` and `web-quality`
- the repository root for `repository-layout` and `project-presentation`
- `backend/` for every other capability

A path that starts at the repository root SHALL say so.

#### Scenario: A backend path
- **WHEN** `genai-exercise` names `docs/genai-sample/`
- **THEN** it means `backend/docs/genai-sample/`

### Requirement: Imported history
The repository SHALL contain the `main` history of aleferod/pokedex-api and aleferod/pokedex-web with every commit SHA unchanged. The README SHALL link both original repositories, whose pull requests hold each change's red/green commits.

#### Scenario: A change merged before the move
- **WHEN** a reviewer runs `git show 4e85c6b` in a clone of this repository
- **THEN** it shows pokedex-api's squash-merged `add-catalog-name-search`
