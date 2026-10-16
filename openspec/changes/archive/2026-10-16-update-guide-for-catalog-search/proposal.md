# Proposal: update-guide-for-catalog-search

## Why

Three changes merged after the presentation guide was written:
- **pokedex-api `add-catalog-name-search`** (pokedex-api#11): `GET /api/v1/pokemon?name=` searches by part of a name.
- **pokedex-web `fix-submit-after-blur`** (pokedex-web#7): it fixed the lost Save click that `docs/GENAI.md` lists as an open finding.
- **pokedex-web `add-catalog-search-ui`** (pokedex-web#8): "Find a Pokémon" searches as you type.

So `docs/PRESENTATION.md` now describes a finder that no longer exists ("typing `eevee` also works, but then the way back starts at page 1"), and its quality numbers are out of date. `docs/GENAI.md` still lists the fixed finding as open, and it lacks this round's AI mistakes.

## What Changes

- **`docs/PRESENTATION.md`:**
  - the demo shows the search: `pika` as you type, and Eevee found by name with the way back keeping the search
  - warming the cache includes one search, which loads the name index
  - the search joins the decisions, the code-review entry points and the likely questions ("Why not ask PokeAPI to search?")
  - the quality numbers, the change counts and the date, from fresh runs
- **`docs/GENAI.md`:**
  - the Save-click finding is recorded as fixed, with its pull request and how the fix differs from the candidate fix
  - this round's real AI mistakes join the tables, in both repositories
- **README:** the roadmap entry and the status, if they quote anything that changed.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `project-presentation`: the guide follows the product when a merged change alters what it shows or quotes.
- `genai-exercise`: an open finding stays listed until a change fixes it, and is then recorded as fixed.

## Out of Scope

- Code changes in either repository.
- Re-recording the screenshots: none of them shows the finder's old behaviour.

## Impact

- **Files:** `docs/PRESENTATION.md`, `docs/GENAI.md` and possibly `README.md`.
- **Behaviour:** no runtime change.
