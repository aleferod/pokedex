# Proposal: add-catalog-search-ui

## Why

"Find a Pokémon" only opens a Pokémon by its exact name or number: "pika" leads to "There is no Pokémon called “pika”". People remember part of a name and expect results while typing. pokedex-api now searches the catalog by part of a name (`GET /api/v1/pokemon?name=pika`, `add-catalog-name-search`: 3 to 50 characters, contained in the name, with the usual paging).

## What Changes

- **Search as you type:** on the catalog, typing **at least 3 characters** of a name in "Find a Pokémon" filters the grid after a short pause (300 ms). "pika" shows Pikachu and its forms.
  - Fewer than 3 characters does nothing yet, and the hint says so.
  - Emptying the field shows the whole catalog again.
  - The field keeps focus while the results change, so typing isn't interrupted.
- **The search lives in the URL** as `?name=pika`, next to the page size: shareable, reloadable, and Back works. A new search starts from page 1, and it pages like the catalog.
- **States:**
  - a live count of the matches
  - "No Pokémon match “zzz”."
  - "Clear search" to return to the catalog
  - characters the API wouldn't accept are explained in place, with no request
- **Numbers still open a Pokémon:** a number and Enter goes to `/pokemon/<number>`, as before.
- **A name and Enter (or "Find") searches at once,** without waiting for the pause. Names don't open the detail page directly any more: with partial names an exact-match jump would be a guess.
- **Elsewhere:** on a Pokémon's "not found" page, the field leads to the catalog's search results (`/pokemon?name=…`). "Back to the catalog" on a Pokémon's page returns to the search the visitor came from.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `catalog-ui`: "Find a Pokémon" becomes a search by part of a name, kept in the URL; a number still opens the Pokémon.
- `pokemon-detail-ui`: "Way back to the catalog" includes the search.

## Out of Scope

- Suggestions in a drop-down. The grid is the result list: it's accessible as it stands, pages, and can be shared.
- Searching localized names, which belong to the local Pokédex, whose own name filter already matches part of a name.

## Impact

- **Code:** `features/catalog` (the search field, the URL parameters, the query and its key, the results header and the way-back state) and the detail page's not-found finder.
- **Backend:** depends on pokedex-api `add-catalog-name-search`.
- **Dependencies:** none new.
