# Tasks: add-catalog-ui

TDD convention: each `test:` commit is red on assertions against stubs, and the `feat:` commit makes it green.

## 1. Rules and components

- [x] 1.1 Write `identifier.test.ts` and `Pagination.test.tsx` (D4, D6). Verify: red against stubs.
- [x] 1.2 Implement `features/catalog/identifier.ts`, `shared/ui/Pagination.tsx` and `shared/ui/QueryError.tsx`. Verify: 1.1 green.

## 2. Catalog page

- [x] 2.1 Write `CatalogPage.test.tsx` with MSW (D1–D3, D5, D6):
  - first page and cards
  - null sprite and category
  - Next/Previous and the URL
  - a shared link
  - nonsense parameters
  - page size
  - focus after a page change
  - skeletons
  - past the end
  - 503 with retry
  - a generic error
  - prefetch
  - the finder: by name, by number, invalid
  - Verify: red against stubs.
- [x] 2.2 Implement `useCatalogParams`, `useCatalogPage`, `CatalogPage`, `PokemonCard`, `CardSkeleton` and `FindPokemonForm`; add the `/pokemon` route, the "Catalog" link and a home-page call to action. Verify: 2.1 green; lint, type-check and coverage gate green.

## 3. Verification

- [x] 3.1 Run every gate in Docker from a clean checkout. Verify: all pass; record coverage.
- [x] 3.2 Against the real pokedex-api and the dev server, in a real browser:
  - pages 1 → 2 → back
  - a shared link
  - size 40
  - past the end
  - find "Pikachu"
  - 360 px width
  - keyboard: Tab through cards and pagination
  - the console
  - Verify: as in the spec; no app console output.
- [x] 3.3 Update the README (catalog, URL state). Run `openspec validate add-catalog-ui --strict` and gitleaks. Verify: valid, no leaks.

## Workflow follow-up

- Push, open the PR and wait for CI to go green.
- Archive as the PR's last commit, wait for CI to be green again, then squash-merge.
- Send the status report.
