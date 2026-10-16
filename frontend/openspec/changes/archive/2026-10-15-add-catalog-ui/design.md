# Design: add-catalog-ui

## Context

The API answers `GET /api/v1/pokemon?page=<0-based>&size=<1..100>` with `{content: [{id, name, spriteUrl|null, category|null, weight, abilities[]}], page, size, totalElements, totalPages}`. Building a page costs PokeAPI work, but it's cached server-side after the first visit. A detail can be looked up by number or by lower-case name (`/api/v1/pokemon/{idOrName}`). The web app already has the API client, query defaults, the layout and accessible form parts.

## Goals / Non-Goals

**Goals:**
- A URL that fully describes the view, for sharing and reloads.
- Smooth paging: no layout jump, the previous page stays visible while the next loads, and the next page is prefetched.
- Every state (loading, empty, error) is designed, not accidental.

**Non-Goals:**
- Catalog free-text search (no API support) and the detail page.

## Decisions

### D1. URL state
- `useCatalogParams()` reads `?page` and `?size` through `useSearchParams` and normalises them:
  - page is an integer ≥ 1, otherwise 1
  - size is one of 20, 40 or 60, otherwise 20
- Writing uses `setSearchParams`, which pushes a history entry, so Back works. Defaults are omitted from the URL (`/pokemon` means page 1, size 20).
- The UI uses 1-based pages, mapped to the API's 0-based `page`.

### D2. Data
- **Query:** `useCatalogPage(page, size)` with the key `['catalog', page, size]` and `placeholderData: keepPreviousData`. While the next page loads, the previous one stays on screen, marked busy, so there's no flash of skeletons.
- **Prefetch:** `queryClient.query(...)` for the next page once a page has loaded (cheap, and cached server-side), with its errors swallowed. TanStack Query 5.104 deprecates `prefetchQuery` in favour of `query(options)`; the type-aware lint flagged it.
- **Types:** `CatalogEntry` and `Page<T>` in `features/catalog/types.ts`. `Page<T>` is generic, so it moves to `shared/api` when a second feature needs it.

### D3. Presentation
- **Results:** a heading ("Pokémon", focusable with `tabIndex=-1`, focused after a page change), then a `ul` of cards (`li` > `article`).
- **Cards:**
  - the sprite `img`, 96×96, with `loading="lazy"` and `alt` = the capitalised name
  - the number as `#1` (as the spec's scenario says) and the name as a heading
  - the category, or "Unknown category"
  - the weight via `Intl.NumberFormat('en', { maximumFractionDigits: 1 })` plus " kg"
  - the abilities as a list
  - The whole card is one link to `/pokemon/:id`, with no nested interactive elements.
- **Grid columns:** 1 → 2 (sm) → 3 (md) → 4 (lg).
- **Skeletons:** cards with the same box size (`aria-hidden`), plus a visually hidden `role="status"` "Loading Pokémon…".

### D4. Pagination component (`shared/ui/Pagination`)
- A `nav` labelled "Pagination".
- **Previous/Next** are buttons, disabled at the ends.
- **"Page N of M"** as text.
- **A size select** labelled "Per page", 20/40/60.
- It's controlled through props (`page`, `totalPages`, `size`, `onPageChange`, `onSizeChange`) and doesn't know about URLs, so the local Pokémon list can reuse it.

### D5. Errors
- **`QueryError`** (`shared/ui/QueryError`): `role="alert"` with a heading "Couldn't load Pokémon", a message, and a "Try again" button calling `refetch()`.
- **Message choice:**
  - 503 → "PokeAPI is temporarily unavailable. Please try again in a moment."
  - network → the client's network message
  - anything else → `ApiError.message`
- **Past the end** (`content` empty and `page > totalPages`): "There are no Pokémon on this page." with a link to the last page. If the catalog itself were empty (`totalElements === 0`): "No Pokémon to show."

### D6. Find a Pokémon
- A small React Hook Form + Zod form, with the API's identifier rule:
  - numbers 1–99999 (leading zeros allowed: `025` → `25`)
  - names `^[a-z][a-z0-9-]{0,49}$` after trim and lower-case
- Invalid input is rejected in place with "Enter a Pokédex number or a name such as pikachu".
- Valid input navigates to `/pokemon/<normalised>`.
- The rule lives in `features/catalog/identifier.ts`, so the detail page can reuse it.

### D7. Testing
| Test | What it covers |
|---|---|
| `identifier.test.ts` | numbers, leading zeros, bounds, names, case, rejects |
| `useCatalogParams` (through the page) | defaults, normalisation, writing |
| `CatalogPage.test.tsx` (MSW) | first page request and card contents; null sprite and category; Next/Previous update the URL and request; shared link; nonsense params; page size resets the page; focus moves to the heading; loading skeletons; past-the-end empty state; 503 and retry; generic error; prefetch of the next page; find by name, find by number, invalid identifier |
| `Pagination.test.tsx` | disabled ends, labels, callbacks |

### D8. MSW 2.15 instead of MSW 3 (found in CI)
CI failed the 503 test once: the page showed "Cannot reach the server" instead of the 503 message. The same failure came back locally in 5 of 11 full runs limited to one CPU. Logging MSW's lifecycle events and the fetch error showed the cause:
- The failed request never reached MSW: there was no `request:start` event for it.
- Undici really connected to `localhost:3000` and got `ECONNREFUSED` (an `AggregateError`).

MSW 3.0.3 with `@mswjs/interceptors` 0.45.8 intercepts Node's `fetch` at the socket level, and under load it sometimes misses a request that follows another one to the same origin. Such a request skips the handlers and the unhandled-request check entirely, which breaks the `web-quality` rule that an unmocked request is an error. A short reproduction outside the app did not trigger it, so there is no upstream report yet.

MSW 2.15.0, the mature line, has the same `http`, `HttpResponse` and `delay` APIs. It replaces `fetch` and answers in memory, without sockets. With it: 10 of 10 one-CPU runs passed with coverage, and no test tried the real network. The only change is the option's v2 name, `onUnhandledRequest: 'error'`. Move back to MSW 3 once a release fixes this.

## Risks / Trade-offs

- [The first visit to a page is slow (the API fans out to PokeAPI)] → skeletons on first load, the previous page kept on later loads, and the next page prefetched. The backend cache makes repeat visits instant.
- [No catalog search] → the finder covers "go to a Pokémon", and local records get real filters later.
