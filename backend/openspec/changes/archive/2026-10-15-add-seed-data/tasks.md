# Tasks: add-seed-data

TDD convention as in earlier changes: each `test:` commit is red on assertions, and the `feat:` commit makes it green.

## 1. Accounts with a chosen role

- [x] 1.1 Write `UserTest` (`provision` keeps the role) and `ProvisionUserUseCaseTest` (D2). Verify: red against stubs.
- [x] 1.2 Implement `User.provision`, `ProvisionUser`, `ProvisionUserCommand` and `ProvisionUserUseCase`, and wire it. Verify: 1.1 green; coverage gate green.

## 2. Seeder

- [x] 2.1 Write `DemoDataSeederTest`, `DemoDataFileTest` and `DemoDataConfigurationTest` (D3, D4, D5). Verify: red.
- [x] 2.2 Implement `DemoData` and its reader, `DemoDataSeeder`, `SeedProperties`, `DemoDataConfiguration`, and `seed/demo-data.json` with the names captured from PokeAPI. Add `SEED_DEMO_DATA` to `application.yml`, Compose and `.env.example`. Verify: 2.1 green; ArchUnit green.

## 3. Integration

- [x] 3.1 Write `DemoDataSeedIT` with a test seed file (#1–#3, #25) and WireMock (D5). Verify: green with full `verify`.
- [x] 3.2 Update the README:
  - a demo section with the credentials table and the never-public warning
  - Quick start
  - the configuration row
  - the Roadmap ✅
  - Verify: lychee offline shows no broken links.

## 4. Integration verification

- [x] 4.1 `./scripts/verify-in-docker.sh` from a clean `target/`. Verify: exit 0; record counts and coverage.
- [x] 4.2 From a fresh clone with `docker compose up --build` against the real PokeAPI:
  - `admin` deletes a seeded record (204)
  - `trainer` lists `?tag=starter`
  - restart: no duplicates
  - Verify: as in the spec; `docker compose down -v`.
- [x] 4.3 Run `openspec validate add-seed-data --strict` and gitleaks. Verify: valid, no leaks.

## Workflow follow-up

- Push, open the PR and wait for CI to go green.
- Archive as the PR's last commit, wait for CI to be green again, then squash-merge.
- Send the status report.
