# Design: update-guide-for-catalog-search

## Context

`docs/PRESENTATION.md` was written on 2026-10-15 against the product at that time (pokedex-api#10). Since then:
- the API searches the catalog by part of a name, using a cached index of all names (pokedex-api#11)
- the web app's form buttons keep focus in the field when pressed (pokedex-web#7)
- "Find a Pokémon" searches as you type, keeps the search in the URL and in the way back, and still opens a Pokémon by number (pokedex-web#8)

## Decisions

### D1. Where the search goes in the demo
- **Step 1 (US01)** gains the search. Typing `pika` shows the matches after a pause, with Pikachu first, and the URL becomes `?name=pika`. The step also shows that two letters don't search yet and that a number opens that Pokémon.
- **Step 2 (US02)** reaches Eevee by searching `eevee`, and "Back to the catalog" returns to that search. The step no longer needs page 4.

The demo stays at five steps and ten minutes. The search replaces the finder's old sentence rather than adding a step.

### D2. Numbers
Every number is re-run for this change, not copied:
- **the API:** `./scripts/verify-in-docker.sh` on `main`
- **the web app:** `npm run test:coverage`, and the end-to-end count from `scripts/e2e.sh`, on pokedex-web's `main` after pokedex-web#8
- **the specs:** `openspec validate --all --strict` in each repository

### D3. The fixed finding
The finding moves from "still open" to fixed, naming pokedex-web `fix-submit-after-blur` (pokedex-web#7). The record keeps the original candidate fix, which was validating on submit and then on change, and says why the fix differs: the submit button cancels `mousedown`, so leaving a field still validates it, and every form is fixed at once.

### D4. New AI-mistake rows
Only cases that really happened are added, taken from the commit messages and pull requests:
- **pokedex-api `add-catalog-name-search`:**
  - MockMvc's URI template encoded `%20` a second time
  - `ivy` also matches `snivy`, which had no fixture
- **pokedex-web `add-catalog-search-ui`:**
  - `void name` stubs, rejected by lint
  - React 19's deprecated `FormEvent`
  - result tests that read the grid once while the previous results were still on screen
  - Testing Library's 1 s wait, too short for a cold first render under coverage
- **The process slips list:** an archive commit message that quoted a count taken before archiving, and one with a `<date>` placeholder caught before pushing.

## Risks / Trade-offs

- [The guide's cross-repository links point at files that pokedex-web#8 creates] → this change is opened only after that pull request is merged, and lychee checks those links online.
