# Design: add-detail-ui

## Context

The API answers `GET /api/v1/pokemon/{idOrName}` without credentials. The response is:

```
{ id, name, imageUrl|null, category|null, description|null, types[], height, weight, abilities[],
  stats: { hp, attack, defense, specialAttack, specialDefense, speed },
  evolutionChain: { id, name, condition|null, evolvesTo: [ …same shape… ] } }
```

- `height` is in metres and `weight` in kilograms, each with one decimal.
- The chain always starts at the family's base form, whichever member was asked for.
- Status codes: an unknown Pokémon is a 404 `not-found`, a malformed identifier a 400, and an unreachable PokeAPI a 503.

The web app already has:
- the API client and query defaults (no retry after a 4xx)
- the shared `QueryError`
- the identifier rule and the finder (in `features/catalog`)
- the layout, with an error boundary keyed by the address

## Goals / Non-Goals

**Goals:**
- Every field of US02 is readable at 360 px and on desktop, by keyboard and screen reader.
- The evolution chain is navigable and shows branches and conditions faithfully.
- Every state (loading, invalid, not found, failure) is designed, and none calls the API needlessly.

**Non-Goals:**
- Stage sprites in the chain (not in the API contract) and local Pokédex actions.

## Decisions

### D1. Route and identifier
- The route is `pokemon/:idOrName`, next to `pokemon`.
- The page runs the parameter through the catalog's `parsePokemonIdentifier`:
  - `null` → the invalid-identifier view, with no request
  - a different canonical form (`Pikachu` → `pikachu`, `025` → `25`) → `<Navigate replace>` to it, keeping the location state, so each identifier has one address and one cache entry
  - otherwise → the detail query for that identifier

### D2. Data
- **Query:** `usePokemonDetail(identifier)` with the key `['pokemon', identifier]` and the default retry policy, so a 404 shows at once and a 5xx is retried once.
- **No placeholder data:** while another Pokémon loads, showing the previous one under the new address would mislabel it, so the skeleton shows instead.
- **Types:** `PokemonDetail`, `BaseStats` and `EvolutionStage` in `features/detail/types.ts` mirror `PokemonDetailResponse`.

### D3. Presentation
- **Layout:** one column at 360 px; from `md`, the artwork on the left and the facts on the right.
- **Header:** `#25` above the name, an `h1` with `tabIndex=-1` (see D6). The types follow as a list labelled "Types", one badge each.
- **Artwork:** an `img`, 256×256, with `alt` = the name. Without one, a `role="img"` placeholder labelled "No image available".
- **Description:** a paragraph, or "No description available."
- **Facts:** a `dl` with Category (or "Unknown category"), Height (`0.7 m`), Weight (`6.9 kg`) and Abilities.
- **Base stats:** a `table` captioned "Base stats".
  - One row per stat: HP, Attack, Defense, Special Attack, Special Defense, Speed. The row header is the stat's name, then its value, then a bar.
  - A final "Total" row.
  - The bar is `aria-hidden`, and its width is `value / 255`, 255 being the highest base stat in the games. Table semantics give screen readers the name and value pairs; the bars are only visual.
- **Type colours:** a fixed map from the 18 types to Tailwind classes. White text sits on `-700`/`-800` shades, and electric uses dark text on yellow, so each reaches at least 4.5:1 contrast. An unknown type gets the neutral badge.

### D4. Evolution chain
- **Rendering:** a recursive `EvolutionTree` of nested lists, under the heading "Evolution".
  - Each stage is a link to `/pokemon/:id`, with the condition in small text before it ("Level 16"). The base form has no condition.
  - Next stages are an indented `ul` with a left rule. This reads as a tree at 360 px, and Eevee's 8 branches stack as 8 siblings.
  - The stage on screen keeps its link, with `aria-current="page"` and a highlighted style, as navigation links do.
- **No evolutions:** a root with an empty `evolvesTo` shows "<Name> does not evolve." instead of the tree.

### D5. States
- **Loading:** a skeleton of the same layout (`aria-hidden`) and a visually hidden `role="status"`: "Loading Pokémon…".
- **Invalid identifier:** a "Pokémon not found" heading, then "“pika chu” isn't a Pokédex number or a name.", the finder (`FindPokemonForm`) and a link back to the catalog.
- **404:** the same heading, then "There is no Pokémon called “missingno”." for a name or "There is no Pokémon number 99999." for a number. The finder and the link follow.
- **503 and others:** the shared `QueryError`, titled "Couldn’t load this Pokémon", with "Try again".
- **Document title:**
  - "<Name> · Pokédex" once loaded
  - "Pokémon not found · Pokédex" for the invalid and 404 views
  - "Pokémon · Pokédex" while loading or failed

### D6. The way back, and focus
- **Way back:**
  - Catalog cards pass `state={{ catalogSearch }}`, the catalog's own `catalogSearch(page, size)`. Evolution links pass the received state on.
  - "Back to the catalog" goes to `/pokemon` plus that search.
  - The detail page accepts only `{ catalogSearch: string }`, then re-reads the value through `parsePage` and `parseSize` and rebuilds it with `catalogSearch`. History state therefore can't produce an odd link.
  - Both sides live in `features/catalog/catalogReturn.ts`: `catalogReturnState(page, size)` for the cards and `catalogReturnSearch(state)` for the detail page. The catalog writes the state, and detail depends on catalog, not the reverse.
- **Focus:** when the loaded Pokémon changes and the address was reached inside the app (`location.key !== 'default'`), focus moves to the name. Otherwise, after clicking a chain link, focus would fall back to the document as the link disappears. On a fresh load, the browser's own focus is left alone.

### D7. Shared pieces
- `features/catalog/index.ts` also exports `FindPokemonForm`, `displayName` and `catalogReturnSearch`. The dependency runs one way: detail → catalog.
- `shared/format.ts` has `formatKilograms` (`6.9 kg`) and `formatMetres` (`0.7 m`): one `Intl.NumberFormat('en')` with at most one decimal. `PokemonCard` moves to it.

### D8. Testing
| Test | What it covers |
|---|---|
| `format.test.ts` | whole numbers, one decimal, rounding, grouping |
| `catalogReturn.test.ts` | the state a card carries; reading valid, missing, malformed and non-canonical state |
| `PokemonDetailPage.test.tsx` (MSW) | contents (artwork, number, name, types, category, description, height, weight, abilities, the stats table and total); null artwork, description and category; a linear chain with conditions, links and the current stage; Eevee's 8 branches; Ditto; following the chain (new request, title, focus); canonical address; invalid identifier with no request; 404 by name and by number; 503 with "Try again"; the way back from catalog page 3 and through the chain; the route from a catalog card |

### D9. Current page vs current section (found in the browser check)
The browser check found two `aria-current="page"` links on a Pokémon's page: the evolution stage being viewed, and the "Catalog" link in the main navigation. React Router's `NavLink` marks a link as the current page on its child routes too. A screen reader then announces "Catalog, current page" on Pikachu's page.

The fix: the "Catalog" link uses `aria-current="page"` only on `/pokemon` itself (`useMatch('/pokemon')`), and `aria-current="true"` on the pages below it, meaning the current item of the navigation. The visual underline stays for both. A new `app-shell` requirement, "Current page in the navigation", says this, and the shell tests cover both cases. (A first draft added the rule to "Accessible layout" instead. That passed the change's own validation but pushed the merged requirement past OpenSpec's 500-character limit, which strict validation of the archived spec caught, so it became a requirement of its own.)

## Risks / Trade-offs

- [On a 0–255 scale most bars are short] → it's the honest scale, and every value is also shown as a number.
- [Deep or wide chains at 360 px] → each level is indented 1 rem, families are at most 3 levels deep, and branches stack vertically.
- [Type colours could fail contrast] → shades were chosen for 4.5:1, and they are checked in the browser during verification.
