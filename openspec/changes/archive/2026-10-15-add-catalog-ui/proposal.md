# Proposal: add-catalog-ui

## Why

US01 asks to browse the Pokémon catalog page by page. Each entry shows its sprite, name, category, weight and abilities. pokedex-api serves it at `GET /api/v1/pokemon?page&size`, publicly. The web app has no catalog screen yet.

## What Changes

- **The catalog page** (`/pokemon`, linked as "Catalog" in the main navigation and from the home page): a responsive grid of cards (1 column at 360 px, up to 4 on desktop).
  - Each card shows the sprite (or a placeholder), number, name, category, weight in kg and abilities.
  - Each card links to the Pokémon's page (`/pokemon/:id`).
- **Pagination in the URL:** `?page=N` is 1-based for people, and the API's `page` is `N - 1`. There are Previous/Next controls, "Page N of M" and a page-size choice (20, 40 or 60, kept as `?size=`).
  - A value that isn't a valid page falls back to page 1.
  - A page past the end shows an empty state with a link to the last page.
  - Changing pages moves focus to the results heading, and the next page is prefetched.
- **States:** loading skeletons; an error state with "Try again". A 503 says PokeAPI is unavailable for now; other failures show the API's message.
- **Find a Pokémon:** a field that takes a name or a Pokédex number (the API's identifier rule: 1–99999, or a name of letters, digits and hyphens starting with a letter) and opens `/pokemon/<value>`. The catalog API has no name search, so this lookup is the "search" the brief asks for. That address is shareable like the page.

## Capabilities

### New Capabilities
- `catalog-ui`: the catalog grid, pagination in the URL, its states, and finding a Pokémon by name or number.

### Modified Capabilities
- None (`app-shell` navigation gains a "Catalog" link).

## Out of Scope

- The detail page itself (`add-detail-ui`). Card links and the finder lead there.
- Free-text search across the catalog: the API doesn't offer it. Local records get tag and name filters in `add-local-pokemon-management-ui`.

## Impact

- **Code:** `src/features/catalog/*`, the route, the navigation link and a shared `Pagination` component.
- **Backend:** none.
- **Dependencies:** none new.
