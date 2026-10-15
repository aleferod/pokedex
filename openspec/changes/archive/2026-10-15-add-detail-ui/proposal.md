# Proposal: add-detail-ui

## Why

US02 asks to see everything about one Pokémon: its image, stats, description and evolution chain. pokedex-api serves it publicly at `GET /api/v1/pokemon/{idOrName}`, by number or name. The catalog cards and the "Find a Pokémon" form already link to `/pokemon/<id or name>`, but that address has no page yet, so today it shows "Page not found".

## What Changes

- **The detail page** (`/pokemon/:idOrName`), by Pokédex number or name. An identifier written another way (`Pikachu`, `025`) is replaced in the address by its canonical form (`pikachu`, `25`). The page is titled "<Name> · Pokédex".
- **Contents:**
  - the official artwork (or a placeholder), `#number`, name and types
  - the category and the description (each with a fallback when the API has none)
  - height in metres, weight in kilograms, and abilities
  - the six base stats as labelled bars, with their total
- **The evolution chain** as a tree, from the base form, with each stage linking to its own page. Each stage shows how it is reached ("Level 16", "Use Thunder Stone"). Branching families (Eevee's 8) show every branch, and the Pokémon on screen is marked as the current one. A Pokémon without evolutions says so.
- **States:**
  - loading: a skeleton with a status message
  - an identifier the API would reject: a message and the finder, without calling the API
  - unknown Pokémon (404): a "not found" view that names what was asked for, with the finder
  - 503 and other failures: the shared error view with "Try again"
- **A way back:** "Back to the catalog" returns to the catalog page and size the visitor came from. Following the evolution chain keeps that place.
- **Focus:** after moving to another Pokémon inside the app, focus goes to its name, so keyboard and screen-reader users start at the new content.

## Capabilities

### New Capabilities
- `pokemon-detail-ui`: the detail page, its contents, the evolution chain, its states and the way back to the catalog.

### Modified Capabilities
- `app-shell`: on a Pokémon's page, the "Catalog" link in the main navigation is marked as the current section, not the current page (found in the browser check, design D9).
- Catalog cards keep linking to `/pokemon/:id`; they now also remember the catalog page for the way back. The `catalog-ui` requirements are unchanged.

## Out of Scope

- Sprites for the evolution stages: the API returns only `id`, `name` and `condition` for each stage. Adding images there is a backend change, and building PokeAPI sprite URLs in the browser would bypass the API contract.
- Local Pokédex actions on the detail page (adding notes, tags or names): these come with `add-local-pokemon-management-ui`.

## Impact

- **Code:** `src/features/detail/*`, the route, the main navigation's "Catalog" link, small exports from `features/catalog` (the identifier rule, the finder, `displayName`), and shared measure formatting (`shared/format`) used by both the cards and the detail page.
- **Backend:** none.
- **Dependencies:** none new.
