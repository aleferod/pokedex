# Design: add-local-pokemon-management-ui

## Context

pokedex-api serves the local replica under `/api/v1/local-pokemon`, and every call needs a bearer token.

| Call | Answer |
|---|---|
| `POST /sync/{idOrName}` | 201 (imported) or 200 (re-synced, proprietary fields kept); 404, 400, 503 |
| `POST /sync?limit=1..20` | `{ results: [{pokeapiId, name, status}], created, updated }` |
| `GET /?page&size&name&tag` | the paginated envelope, ordered by `pokeapiId`; filters ignore case and combine |
| `GET /{id}` | the record; `ETag: "<version>"` |
| `POST /` | `{ pokeapiId, …proprietary }` → 201; 409 `conflict` with `errors[pokeapiId]` when already stored; 400 for an unknown number |
| `PUT /{id}`, `PATCH /{id}` | `If-Match` required: missing → 428, stale → 409. PUT clears omitted proprietary fields; PATCH is JSON Merge Patch |
| `DELETE /{id}` | `ADMIN` only (403 otherwise); 204; `If-Match` checked when sent |

A record has:
- **identity:** `id` (UUID) and `pokeapiId`
- **upstream (read-only):** `name`, `spriteUrl`, `imageUrl`, `category`, `types`, `height`, `weight`, `abilities`, `stats`
- **proprietary:** `localizedNames` (locale → name), `region`, `habitatNotes`, `internalTags`, `notes`
- **audit:** `createdAt`, `updatedAt`, `syncedAt`, `createdBy`, plus `version`

The proprietary rules and their messages are in the backend's `ProprietaryData`, `InternalTags` and `LocalizedNames`.

The web app already has:
- sessions with a role, `ProtectedRoute` and 401 handling
- the shared `Pagination`, `QueryError` and form parts
- the catalog's page and size parsing
- the detail page's `StatsTable` and `TypeBadges`

## Goals / Non-Goals

**Goals:**
- Every US03/US04 operation is reachable, by keyboard and at 360 px, with the API's rules enforced before a request is sent.
- No lost updates: every write carries the version the user saw, and a conflict is explained, never silently overwritten.
- The URL describes the list view (page, size and filters).

**Non-Goals:**
- Editing upstream fields, bulk delete or bulk tag edits, and a sync action on the public detail page.

## Decisions

### D1. Routes and protection
`local`, `local/new`, `local/:id` and `local/:id/edit` sit under one `ProtectedRoute` layout route. Visitors go to `/login?next=…` and come back afterwards. React Router ranks the static `new` above `:id`.

### D2. Data and cache
- **Query keys:** `['local', 'list', {page, size, name, tag}]` (with `keepPreviousData`) and `['local', 'record', id]`.
- **After a write:** a successful write puts the returned record into its record query and invalidates every list query. A delete removes the record query.
- **Sign-out** already clears the whole cache.
- **Types:** `Page<T>` moves from `features/catalog` to `shared/api`, now that a second feature needs it, as the catalog design anticipated.

### D3. Versions and `If-Match`
- **Where the version comes from:** the API's `ETag` is the record's `version` in quotes, and the body carries `version`. So the client builds `If-Match: "<version>"` from the record it rendered, and `apiRequest` doesn't have to expose response headers.
- **The edit form keeps the version it loaded with** and doesn't follow the cache. Otherwise, a background refetch that picked up someone else's change would turn the user's stale edit into an overwrite without a conflict.

### D4. Forms and rules
- **Rules:** `features/local/rules.ts` mirrors the backend with the same messages.
  - `region` at most 50 characters, `habitatNotes` 500 and `notes` 2000, counted in code points after trimming; blank → `null`
  - tags: typed as comma-separated text, then trimmed, lower-cased and de-duplicated as the API does. Each must match `[a-z0-9][a-z0-9-]{0,29}`, failing with "'<tag>' must be 1 to 30 lower-case letters, digits or hyphens, starting with a letter or digit". At most 20.
  - localized names: rows of locale and name, edited with `useFieldArray`. A locale must match `[a-z]{2,3}(-[A-Za-z0-9]{2,8}){0,2}`, failing with "'<locale>' is not a locale such as en, pt-BR or zh-Hant-TW". A name is 1–50 characters, with at most 10 rows. A locale listed twice is caught here, because a JSON object can't carry it twice.
  - `pokeapiId` (create only): a whole number from 1 to 99999
- **Every broken rule at once:** `criteriaMode: 'all'` with `mode: 'onTouched'`.
- **Server field errors:** `showServerErrors` moves from `features/auth` to `shared/ui/form` and gains a field mapper. `localizedNames.<locale>` lands on that row's name, and any field it can't place goes to the form alert.

### D5. List and filters
- **URL:** `?page` and `?size`, through the catalog's parsers, plus `?name` and `?tag` (trimmed, omitted when empty).
- **The filter form:** `role="search"`, labelled "Filter my Pokédex", with "Name contains" and "Tag" fields. Submitting goes back to page 1. Each tag chip on a row links to `?tag=<tag>`, and "Clear filters" removes both.
- **Rows:** cards with:
  - the sprite, `#pokeapiId`, name, region and tags
  - "Updated <date>"
  - a link to the record
- **Results heading:** it shows the total.
- **Empty states:**
  - nothing stored and no filters: "Your local Pokédex is empty." with the sync and add actions
  - filters set: "No records match these filters." with "Clear filters"

### D6. Sync from PokeAPI
A "Sync from PokeAPI" panel on the list page holds two forms.
- **One Pokémon:**
  - The name or number is checked with the catalog's identifier rule, then sent to `POST /sync/{idOrName}`.
  - A 201 opens the record with "Pikachu was imported from PokeAPI." A 200 opens it with "Pikachu was re-synced from PokeAPI. Its proprietary fields were kept."
  - A 404 puts the API's message on the field. A 503 or any other failure goes to the form alert, with the shared wording.
- **The first N:** a number from 1 to 20 (default 10) sent to `POST /sync?limit=N`. A `role="status"` message says "Synced 3 Pokémon: 3 imported, 0 updated.", and the list refreshes.
- **From a record's page:** "Re-sync from PokeAPI" calls `POST /sync/{pokeapiId}`, updates the record in place and says that the proprietary fields were kept.

### D7. A record's page
- **Header:**
  - the artwork (or sprite), `#pokeapiId` and the name (`h1`)
  - "View on the public page" (`/pokemon/<pokeapiId>`)
- **"From PokeAPI" section:** marked read-only, with:
  - the types, category, height, weight and abilities, and `StatsTable`
  - "Synced <date>" and "Re-sync from PokeAPI"
- **"Your data" section:** region, habitat notes, notes, localized names (a `dl` of locale → name) and the tags, with fallbacks such as "No region yet".
- **Audit line:** "Added by ash on <date> · Updated <date>".
- **Actions:** "Edit" for everyone, and "Delete" for `ADMIN` only.
- **404:** "This record isn't in your local Pokédex. It may have been deleted.", with a link to the list.

### D8. Tags in place, optimistically
- **Controls:** each tag chip has a "Remove tag <tag>" button, and an "Add a tag" field checks the tag rule first.
- **The request:** `PATCH {internalTags}` with `If-Match` set to the record's current version.
- **The optimistic flow:**
  - `onMutate` cancels the record query, keeps a snapshot and writes the new tags.
  - `onError` restores the snapshot and explains. A 409 says "Someone else changed this record, so your tag change was undone. The latest version is shown." and refetches. A 400 shows the API's tag message.
  - `onSuccess` stores the returned record, including its new version.
- **One change at a time:** the tag controls are disabled while a change is pending, so two quick changes can't race each other on the same version.

### D9. Create and edit, and conflicts
- **Create:**
  - `POST` with `pokeapiId` and the proprietary fields.
  - A 201 opens the record with "Bulbasaur was added to your local Pokédex."
  - A 409 puts "is already stored locally" on the number field, and a 400 puts its errors on their fields.
- **Edit:**
  - `PUT` with every proprietary field. A blank field becomes `null`, which matches the API's replace semantics.
  - A 200 opens the record with "Your changes were saved."
  - A 409 shows a form alert: "Someone else changed this record after you opened it, so your changes weren't saved." It offers "Load the latest version", which refetches and resets the form to the new values and version; the alert notes that this replaces the user's edits.
  - A 404 on load shows the not-found view.
  - Both forms keep the user's input on failure.

### D10. Delete, for administrators
- **Who sees it:** "Delete" is rendered only when the session's role is `ADMIN`.
- **Confirmation:** activating it opens an inline confirmation, a `role="group"` labelled "Delete Pikachu?", saying it can't be undone. It offers "Delete Pikachu" and "Cancel", and focus moves to "Cancel".
- **The request:** `DELETE` with `If-Match`. A 204 returns to `/local` with "Pikachu was deleted.".
- **Failures:**
  - a 409 explains and refetches
  - a 404 returns to the list with "Pikachu was already deleted."
  - a 403 (a role changed meanwhile) says that only administrators can delete records

### D11. Messages across navigation
- **Carrying the message:** a navigation can carry `state: { flash }`, for example after a sync, a create or a delete.
- **Where it shows:** the layout (`shared/ui/FlashMessage`), above every page, so a message reaches pages built in other tasks.
  - The region is a persistent `aria-live="polite"` area, so screen readers hear the message when it appears. It deliberately isn't `role="status"`: pages keep their own status messages, such as "Loading Pokémon…".
- **Showing it once:** the message is copied into the layout's state for that address, adjusted during render as React recommends for derived state, and the history entry is replaced without it. It disappears on the next navigation, and a reload or Back doesn't repeat it.

### D12. Navigation
`shared/ui/SectionNavLink` wraps `NavLink`:
- **`aria-current`:** `"page"` on its exact path (`useMatch`), and `"true"` below it.
- **Who uses it:** "Catalog" and the new "My Pokédex".
- **Visitors:** "My Pokédex" is shown to everyone; visitors land on the sign-in page and come back afterwards.

### D13. Dates
Dates use `Intl.DateTimeFormat('en', { dateStyle: 'medium', timeStyle: 'short' })` inside `<time dateTime>`. Tests assert `dateTime`, so they don't depend on the time zone.

### D14. Testing
| Test | What it covers |
|---|---|
| `rules.test.ts` | each rule and message, several broken at once, normalisation (trim, lower-case, de-duplicate, blank → null), code points, duplicate locales, the request body |
| `App.test.tsx` | "My Pokédex" as the current page and as the current section; visitors sent to sign in |
| `LocalListPage.test.tsx` (MSW) | rows, paging, filters in the URL and the request, tag chips, clear, both empty states, sync one (201 and 200 messages, invalid, 404, 503), sync N (summary, list refresh), errors |
| `LocalRecordPage.test.tsx` (MSW) | the read-only and proprietary sections, audit, re-sync, tags added and removed optimistically (request body and `If-Match`, rollback on 409 and on 400), delete for `ADMIN` (confirmation, `If-Match`, message) and not for `USER`, 404 |
| `LocalFormPages.test.tsx` (MSW) | create (201, 409 on the number, 400 field errors including a localized name, rules before sending); edit (prefill, PUT body and `If-Match`, the loaded version kept, 409 with "Load the latest version") |

### D15. Signing out of a protected page (found in the browser check)
**The symptom:** signing out on `/local` landed on `/login?next=%2Flocal` instead of the home page. `add-auth-ui` had fixed this by navigating home before clearing the session, and its test passed. But that test clicks through React Testing Library's `act()`, which flushes React Router's transition to `/` before the session is cleared.

**What a browser does:** it renders the urgent sign-out first, on the protected page that is still showing, and `ProtectedRoute` redirects to sign-in. `/local` is the app's first real protected page, so this is the first place a user could meet the bug.

**The fix:** the navigation home is flushed synchronously (`flushSync`) before the session is cleared. The tests now render with the DOM `RouterProvider`, as the app does, so that option works in them too. A new test clicks "Sign out" on `/local` with React's act environment off, as a browser schedules updates; it failed 3 of 3 times before the fix.

## Risks / Trade-offs

- [A background refetch while editing] → the form keeps its loaded version (D3), so a conflict is reported rather than overwritten.
- [Quick successive tag changes] → the controls are disabled while one is pending (D8).
- [An optimistic change that fails] → it's rolled back and the user is told why (D8).
- [Client rules drifting from the API's] → the messages are copied from the backend, and the API's 400s still land on the fields.
- [Showing "My Pokédex" to visitors] → it leads to sign-in and back. That's clearer than a link that appears only after signing in.
