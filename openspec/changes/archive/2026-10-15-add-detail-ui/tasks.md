# Tasks: add-detail-ui

TDD convention: each `test:` commit is red on assertions against stubs, and the `feat:` commit makes it green.

## 1. Helpers

- [x] 1.1 Write `shared/format.test.ts` and `features/catalog/catalogReturn.test.ts` (D6, D7). Verify: red against stubs.
- [x] 1.2 Implement `shared/format.ts` (and move `PokemonCard` to it) and `features/catalog/catalogReturn.ts`. Verify: 1.1 green; the catalog tests stay green.

## 2. Detail page

- [x] 2.1 Write `PokemonDetailPage.test.tsx` with MSW (D1–D6):
  - contents and the stats table
  - missing artwork, category and description
  - a linear chain with conditions, links and the current stage
  - Eevee's branches
  - Ditto
  - following the chain: request, title, focus
  - the canonical address
  - an invalid identifier, with no request
  - 404 by name and by number
  - 503 with "Try again"
  - the way back from catalog page 3, through the chain, and when opened directly
  - Verify: red against stubs.
- [x] 2.2 Implement `api.ts`, `types.ts`, `usePokemonDetail`, `PokemonDetailPage`, `DetailSkeleton`, `StatsTable`, `TypeBadges`, `EvolutionTree` and `DetailNotFound`. Add the route, the catalog exports and the cards' link state. Verify: 2.1 green; lint, type-check and coverage gate green.

- [x] 2.3 Found in 3.2: on a Pokémon's page the "Catalog" nav link said `aria-current="page"` (D9). Write shell tests for "current page" on `/pokemon` and "current section" on `/pokemon/25`. Verify: red on the second.
- [x] 2.4 Mark the "Catalog" link `aria-current="true"` below `/pokemon`. Verify: 2.3 green; all gates green.

## 3. Verification

- [x] 3.1 Run every gate in Docker from a clean checkout. Verify: all pass; record coverage.
- [x] 3.2 Against the real pokedex-api and the dev server, in a real browser, check:
  - Bulbasaur: stats and the chain to Venusaur
  - Pikachu by name: the chain from Pichu
  - Eevee's 8 branches
  - Ditto
  - `/pokemon/Pikachu` becomes the canonical address
  - `missingno`: not found
  - `pika chu`: invalid
  - catalog page 3 → card → chain → back
  - keyboard focus after following the chain
  - 360 px
  - type badge contrast
  - the console
  - Verify: as in the spec; no app console output.
- [x] 3.3 Update the README (detail page, way back). Run `openspec validate add-detail-ui --strict` and gitleaks. Verify: valid, no leaks.

## Workflow follow-up

- Push, open the PR and wait for CI to go green.
- Archive as the PR's last commit, wait for CI to be green again, then squash-merge.
- Send the status report.
