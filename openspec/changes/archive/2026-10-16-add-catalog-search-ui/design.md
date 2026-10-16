# Design: add-catalog-search-ui

## Context

The API to build on:
- `GET /api/v1/pokemon?name=<text>&page&size` returns the catalog entries whose name contains the text.
- The text is trimmed and lower-cased, spaces become hyphens, and it must be 3–50 of `[a-z0-9-]` (400 `errors[name]` otherwise).
- A blank name lists the catalog.

The catalog page today:
- keeps `?page` and `?size` in the URL (`catalogParams`) and queries `['catalog', page, size]` with the previous page kept on screen
- prefetches the next page
- has a "Find a Pokémon" form that opens `/pokemon/<identifier>`

Catalog cards carry the catalog's place for the detail page's way back (`catalogReturn`).

## Decisions

### D1. The search's rule, mirrored
`catalogParams.ts` gains `parseName(raw)`:
- **Normalising:** trim, lower-case, and turn each run of spaces into one hyphen.
- **Valid:** 3–50 of `[a-z0-9-]`, which becomes the query.
- **Otherwise:** `''`, meaning no search.

A `?name=` in the URL that breaks the rule is ignored, like nonsense in `?page`. `catalogSearch(page, size, name)` writes the parameters in the order page, size, name, omitting defaults and an empty name.

### D2. The field
`FindPokemonForm` keeps its label, "Find a Pokémon", and its "Find" button, with two behaviours.

**On the catalog (`autoSearch`):**
- **The text:** the field's text is local state, seeded from `?name`.
- **The pause:** 300 ms after the last change, a valid name is written to the URL with `setSearchParams(…, { replace: true })`, page 1 and the current size. An empty field removes `name`.
- **Too little to search:** 1–2 characters, or a number, do nothing while typing.
- **Characters outside the rule:** shown at once as "Use letters, digits, hyphens or spaces", with no request.
- **Back, or "Clear search":** the URL's `name` changes from outside, and the field follows. This adjusts state during render, as React recommends; the form is never re-mounted, so the field keeps focus and the cursor.

**Submitting (Enter or "Find"):**
- **A number (1–99999):** opens `/pokemon/<n>`.
- **A valid name:** searches at once: on the catalog it writes the URL, and elsewhere it goes to `/pokemon?name=…`.
- **Anything else:** "Type at least 3 letters of a name, or a Pokédex number".

**Why the URL is replaced, not pushed:** each pause would otherwise add a history entry, and Back would step through "pik", then "pika". A search reached by a link, or the not-found page's field, is a normal navigation.

### D3. Data
- **The request:** `fetchCatalogPage(page, size, name)` adds `name` when it isn't empty. The query key is `['catalog', page, size, name]`, keeping the previous results on screen while the next load.
- **Prefetch:** it includes the name.
- **Searching and paging** share one code path, so the skeletons, past-the-end state, errors and pagination all apply to search results.

### D4. Results header and states
With a search:
- **The count:** "17 Pokémon match “pika”", in a polite live region (not `role="status"`, which the loading state uses), next to a "Clear search" link to `/pokemon` with the size kept.
- **Nothing matches:** "No Pokémon match “zzz”."

Without a search, the header stays as it is ("1,351 in total").

### D5. The way back
`catalogReturnState(page, size, name)` carries the search, and `catalogReturnSearch` rebuilds it through `parseName`, so history state can't inject anything. "Back to the catalog" returns to `/pokemon?name=pika` (or with page and size), including through the evolution chain.

### D6. Testing
| Test | What it covers |
|---|---|
| `catalogParams` / `catalogReturn` tests | name normalisation and the rule, the parameter order, the way back with a name |
| `CatalogPage.test.tsx` (MSW) | typing 3+ characters searches after the pause (URL and request); 1–2 characters don't; a number on Enter opens the Pokémon; Enter searches at once; invalid characters explained, nothing sent; the count, "Clear search" and no matches; a shared `?name=` link fills the field; emptying the field returns to the catalog; Back restores the field; a new search keeps the size and starts at page 1 |
| `PokemonDetailPage.test.tsx` | the way back to a search; the not-found field goes to the search results |
| `e2e/visitor.e2e.ts` | the real search: "pika" → Pikachu among the results → its page → back to the search |

## Risks / Trade-offs

- [Names no longer jump to the detail page] → the search shows the Pokémon first for its own name ("pikachu" → Pikachu first), one click away, and numbers still jump.
- [A request per pause while typing] → 300 ms pauses, a 3-character minimum, the API's cache, and TanStack Query cancelling superseded requests.
