# Proposal: add-local-pokemon-management-ui

## Why

US03 and US04 ask for a local Pokédex: Pokémon copied from PokeAPI into the service's own database, extended with proprietary fields (localized names, region, habitat notes, internal tags and notes), and managed by signed-in users. Only administrators may delete records. pokedex-api serves all of it under `/api/v1/local-pokemon`, with sync, list and filter, create, replace, merge-patch and delete. Edits are guarded by `ETag`/`If-Match`. The web app has no screen for any of it yet.

## What Changes

- **"My Pokédex"** (`/local`, protected, linked in the main navigation):
  - a paginated list of local records
  - name and tag filters kept in the URL with the page and size, so a filtered view can be shared and reloaded
  - designed empty states: nothing stored yet, or nothing matching the filters
- **Sync from PokeAPI (US03):**
  - import or re-sync one Pokémon by name or number, then open its record
  - sync the first N catalog entries (1–20) and report how many were imported and updated
  - re-sync a record from its page; the proprietary fields are kept, and the page says so
- **A record's page** (`/local/:id`):
  - the PokeAPI data, read-only and marked as such
  - the proprietary fields
  - who created it and when it was last updated and synced
  - adding and removing tags in place, applied optimistically and rolled back if the API refuses
- **Create** (`/local/new`) and **edit** (`/local/:id/edit`) forms for the proprietary fields. They use the API's own rules and messages: lengths, the tag format, locales and at most 10 localized names. Every broken rule is listed at once, and the API's 400 field errors land on the same fields.
- **Optimistic concurrency:** every change sends `If-Match` with the version the user saw. When someone else changed the record first (409):
  - the edit form explains that the edits weren't saved and offers to load the latest version
  - tag changes are rolled back and the latest version is shown
- **Delete (administrators only):** the button appears only for `ADMIN` sessions. It asks for confirmation inline, sends `If-Match`, and returns to the list with a message.
- **Navigation:** "My Pokédex" follows the same current-page and current-section rule as "Catalog".

## Capabilities

### New Capabilities
- `local-pokemon-ui`: the local Pokédex list and filters, syncing from PokeAPI, a record's page, creating and editing proprietary fields, optimistic concurrency, and administrator-only deletion.

### Modified Capabilities
- `app-shell`: the "Current page in the navigation" rule now covers "My Pokédex" as well as "Catalog".

## Out of Scope

- A "save to my Pokédex" action on the public detail page. The sync form on `/local` covers it, and the record links to the public page.
- Editing upstream fields: the API rejects them as read-only, so the forms never offer them.
- Bulk delete and bulk tag edits.

## Impact

- **Code:** `src/features/local/*`, four protected routes and the navigation link. Small shared moves:
  - `Page<T>` to `shared/api`
  - the server-error helper to `shared/ui/form`
  - a `SectionNavLink` for the main navigation
  - `StatsTable` and `TypeBadges` exported from `features/detail`
- **Backend:** none.
- **Dependencies:** none new.
