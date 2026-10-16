# Tasks: add-local-pokemon-management-ui

TDD convention: each `test:` commit is red on assertions against stubs, and the `feat:` commit makes it green. Moves without behaviour change are `refactor:` commits, with the existing tests green before and after.

## 1. Groundwork

- [x] 1.1 Move `Page<T>` to `shared/api` and `showServerErrors` to `shared/ui/form` (with a field mapper), and export `StatsTable` and `TypeBadges` from `features/detail` (D2, D4). Verify: all existing tests green.
- [x] 1.2 Write the shell tests for "My Pokédex": the current page on `/local`, the current section on a record, and visitors sent to sign in (D1, D12). Verify: red on the new assertions.
- [x] 1.3 Add `SectionNavLink`, the "My Pokédex" link and the protected routes. Verify: 1.2 green.

## 2. Rules

- [x] 2.1 Write `rules.test.ts` (D4): each rule and message, several broken at once, normalisation, code points, duplicate locales, the request body. Verify: red against stubs.
- [x] 2.2 Implement `features/local/rules.ts`. Verify: 2.1 green.

## 3. List and sync

- [x] 3.1 Write `LocalListPage.test.tsx` with MSW (D2, D5, D6, D11, D13):
  - rows, paging, filters and their request, tag chips, clear
  - both empty states, errors
  - sync one: 201 and 200 messages, invalid identifier, 404, 503
  - sync N: summary and list refresh
  - Verify: red against stubs.
- [x] 3.2 Implement `types.ts`, `api.ts`, the list and sync hooks, `LocalListPage`, `LocalRecordCard`, `LocalFilters`, `SyncPanel` and the flash message. Verify: 3.1 green; all gates green.

## 4. A record's page

- [x] 4.1 Write `LocalRecordPage.test.tsx` with MSW (D7, D8, D10):
  - the read-only and proprietary sections, audit, re-sync
  - optimistic tag add and remove: body, `If-Match`, rollback on 409 and on 400
  - delete for `ADMIN`: confirmation, `If-Match`, message; not offered to `USER`
  - 404
  - Verify: red against stubs.
- [x] 4.2 Implement `LocalRecordPage`, `TagEditor`, `DeleteRecord` and their mutations. Verify: 4.1 green; all gates green.

## 5. Create and edit

- [x] 5.1 Write `LocalFormPages.test.tsx` with MSW (D3, D4, D9):
  - create: 201, 409 on the number, 400 field errors including a localized name, rules before sending
  - edit: prefill, PUT body and `If-Match`, the loaded version kept, 409 and "Load the latest version"
  - Verify: red against stubs.
- [x] 5.2 Implement `ProprietaryFields` (with the localized-name rows), `LocalCreatePage` and `LocalEditPage`. Verify: 5.1 green; all gates green.

## 6. Verification

- [x] 6.1 Run every gate in Docker from a clean checkout. Verify: all pass; record coverage.
- [x] 6.2 Against the real pokedex-api (with its demo data) and the dev server, in a real browser:
  - as `trainer`: the list, filters and a tag chip; sync one (import, then re-sync); sync 5
  - add with every field, edit, tags in place
  - a conflict from a second tab; no "Delete"
  - as `admin`: delete with confirmation
  - as a visitor: redirect to sign in and back
  - 360 px, keyboard, the console
  - Verify: as in the spec; no app console output.
- [x] 6.3 Update the README (My Pokédex, sync, concurrency, roles). Run `openspec validate add-local-pokemon-management-ui --strict` and gitleaks. Verify: valid, no leaks.

- [x] 6.4 Found in 6.2: signing out of `/local` landed on sign-in (D15). Write a test that signs out of `/local` outside `act()`, as a browser schedules it. Verify: red (3 of 3 runs end at `/login`).
- [x] 6.5 Navigate home with `flushSync` before signing out, and render tests with the DOM `RouterProvider`. Verify: 6.4 green; all gates green; checked again in the browser.

## Workflow follow-up

- Push, open the PR and wait for CI to go green.
- Archive as the PR's last commit, and run `openspec validate --all --strict` before committing it. Wait for CI to be green again, then squash-merge.
- Send the status report.
