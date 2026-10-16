# Tasks: add-catalog-search-ui

TDD convention: each `test:` commit is red on assertions against stubs, and the `feat:` commit makes it green.

## 1. Rules and URL

- [x] 1.1 Write the `parseName`, `catalogSearch` and `catalogReturn` tests with a name (D1, D5). Verify: red against stubs.
- [x] 1.2 Implement them. Verify: 1.1 green; the existing catalog and detail tests stay green.

## 2. Search on the catalog

- [x] 2.1 Write the `CatalogPage.test.tsx` search tests and the `PokemonDetailPage.test.tsx` way-back and not-found tests (D2–D5). Update the two "find by name" tests to the new behaviour. Verify: red.
- [x] 2.2 Implement the query with a name, the search field (pause, URL sync, Enter), the results header and states, and the way back. Verify: 2.1 green; all gates green.

## 3. Verification

- [x] 3.1 Add the search journey to `e2e/visitor.e2e.ts` and run the whole end-to-end suite with pokedex-api built from a checkout that has `add-catalog-name-search` (`API_CONTEXT`). Check it in a real browser: typing "pika" and pausing; a 2-character pause; "025" and Enter; Back; "Clear search"; a shared link; the keyboard; 360 px; the console. Verify: as specified; no app console output.
- [x] 3.2 Update the README (Catalog). Run `openspec validate add-catalog-search-ui --strict` and gitleaks. Verify: valid, no leaks.

## Workflow follow-up

- Open the PR after pokedex-api `add-catalog-name-search` is merged: CI's end-to-end job builds the API from its `main`.
- Wait for CI to go green.
- Archive as the PR's last commit, with `openspec validate --all --strict` run before committing. Wait for CI to be green again, then squash-merge.
