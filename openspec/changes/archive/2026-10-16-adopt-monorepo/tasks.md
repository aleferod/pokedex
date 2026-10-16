# Tasks: adopt-monorepo

## 1. The API's repository URLs (backend)

- [x] 1.1 Red: `ProblemTypesDocumentedTest` pins `app.problems.base-uri` to `https://github.com/aleferod/pokedex/blob/main/backend/docs/problems.md#`, and `PokeApiPropertiesTest` pins the default user agent (fallback and `application.yml`) to name `https://github.com/aleferod/pokedex`. Verify: the tests fail on the old URLs.
- [x] 1.2 Green: update `application.yml`, `PokeApiProperties`, `pom.xml`'s `<url>` and the example in `docs/problems.md`; update pokedex-web's problem fixtures. Verify: `backend/scripts/verify-in-docker.sh` passes; the frontend tests pass.

## 2. The full stack at the root

- [x] 2.1 Move `frontend/docker-compose.full.yml` to the root `docker-compose.yml`, with the backend built from `./backend` and the app from `./frontend`; move the full stack's variables to the root `.env.example`. Verify: from a fresh clone, `cp .env.example .env && docker compose up --build --wait` makes the three services healthy, the app answers on :8000, and `trainer` sees the ten demo records; without `.env`, Compose names the missing variable.
- [x] 2.2 Point `frontend/scripts/e2e.sh` at the root compose file and `.env`. Verify: the whole suite passes.

## 3. One CI workflow

- [x] 3.1 Write `.github/workflows/ci.yml` with every job of both former workflows (D5); remove `backend/.github/`, `frontend/.github/` and `backend/.gitleaks.toml`. Verify: the workflow is valid YAML, and on the pull request every job runs and passes.

## 4. Docs

- [x] 4.1 Write the root README (D7).
- [x] 4.2 Move `backend/docs/PRESENTATION.md` to `docs/PRESENTATION.md` and make its links relative; update the backend and frontend READMEs and `backend/docs/GENAI.md`. Verify: lychee finds no broken links (relative offline, GitHub online); no living document links one app to the other through GitHub.

## 5. Verification

- [x] 5.1 Run the backend gate (`backend/scripts/verify-in-docker.sh`), the frontend gates (lint, format, types, tests with coverage, build) and the end-to-end suite from the root; `openspec validate adopt-monorepo --strict`; gitleaks over the full history. Verify: all pass; no leaks.

## Workflow follow-up

- Push, open the PR and wait for CI to go green.
- Archive as the PR's last commit (a new capability: write its Purpose), with `openspec validate --all --strict` run before committing. Wait for CI to be green again, then squash-merge.
- Then, in aleferod/pokedex-api and aleferod/pokedex-web: a README pointer to this repository, merged, then archive the repository.
