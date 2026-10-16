# Tasks: add-e2e-and-polish

The end-to-end journeys are the tests of this change: each is written and run red before the pieces it needs exist (the stack, the runner), then green against the real stack.

## 1. Full stack

- [x] 1.1 Add `docker-compose.full.yml` (D1) and the backend placeholders in `.env.example`. Verify: from a fresh clone, `cp .env.example .env && docker compose -f docker-compose.full.yml up --build --wait` brings all three services up healthy; the app answers on 8000; without `.env` Compose names the missing variable.

## 2. End-to-end tooling

- [x] 2.1 Add `@playwright/test` 1.63.0 and `@axe-core/playwright` 4.13.0, `playwright.config.ts`, `tsconfig.e2e.json`, the ESLint and Vitest settings, `scripts/e2e.sh` and `npm run test:e2e` (D2, D7). Verify: lint, type-check and the unit suite stay green; `scripts/e2e.sh` with an empty suite starts and removes a stack.

## 3. Journeys

- [x] 3.1 Write `visitor.e2e.ts`, `account.e2e.ts` and `local.e2e.ts` (D3). Verify: run against the stack; any failure is investigated (app bug → fix test-first in the unit suite too; test bug → fix the test).
- [x] 3.2 Write `a11y.e2e.ts` (D4). Verify: run; fix every serious or critical violation in the app, with a unit test where the fix is behavioural.

## 4. CI, screenshots and docs

- [x] 4.1 Add the "End-to-end" CI job (D5). Verify: the workflow parses; the job runs green on the PR.
- [x] 4.2 Add `screenshots.e2e.ts` and generate `docs/screenshots/` (D6). Verify: the images show the real stack with demo data.
- [x] 4.3 Update the README: the full stack, end-to-end tests, screenshots, status and roadmap. Run `openspec validate add-e2e-and-polish --strict`, gitleaks and lychee. Verify: valid, no leaks, no broken links.

## Workflow follow-up

- Push, open the PR and wait for CI (including End-to-end) to go green.
- Archive as the PR's last commit, with `openspec validate --all --strict` run before committing. Wait for CI to be green again, then squash-merge.
- Send the status report.
