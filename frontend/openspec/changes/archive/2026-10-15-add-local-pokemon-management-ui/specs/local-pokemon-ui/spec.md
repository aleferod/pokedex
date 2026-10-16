# Spec Delta: local-pokemon-ui

## ADDED Requirements

### Requirement: Local Pokédex list
The page at `/local` SHALL require a session and SHALL list local records a page at a time, ordered by Pokédex number, with the shared pagination and the page and size in the URL. Each record SHALL show its sprite, number, name, region, tags and last update, and SHALL link to its page. The page SHALL be titled "My Pokédex · Pokédex".

#### Scenario: Signed-in user
- **WHEN** a signed-in user opens `/local`
- **THEN** the app requests `/api/v1/local-pokemon?page=0&size=20` and shows a card for Pikachu with "#25", its region and its tags, linking to `/local/<its id>`

#### Scenario: Visitor
- **WHEN** a visitor opens `/local?tag=starter`
- **THEN** they are sent to the sign-in page, which brings them back to `/local?tag=starter` afterwards

### Requirement: List filters in the URL
The list SHALL offer filters by name (contains) and by tag. Both SHALL live in the URL with the page and size, combine with each other, go back to page 1 when they change, and be sent to the API. Each tag shown on a record SHALL link to the list filtered by it, and "Clear filters" SHALL remove both.

#### Scenario: Filter by name and tag
- **WHEN** a user filters by name "saur" and tag "starter"
- **THEN** the URL becomes `/local?name=saur&tag=starter` and the app requests `name=saur&tag=starter` with `page=0`

#### Scenario: Follow a tag
- **WHEN** a user on page 2 follows the tag "kanto" on a record
- **THEN** the URL becomes `/local?tag=kanto`

### Requirement: List states
The list SHALL show a loading state and an error with "Try again". With no records and no filters, it SHALL say that the local Pokédex is empty and offer the sync and add actions. With filters and no matching record, it SHALL say that no records match and offer "Clear filters".

#### Scenario: Empty Pokédex
- **WHEN** the API answers with no records and no filter is set
- **THEN** the page says "Your local Pokédex is empty." and offers to sync from PokeAPI or add a Pokémon

#### Scenario: Nothing matches
- **WHEN** the API answers with no records for `?tag=legendary`
- **THEN** the page says "No records match these filters." and offers "Clear filters"

### Requirement: Sync from PokeAPI
The list page SHALL sync one Pokémon by name or number, checked with the API's identifier rule before any request, and then open its record, saying whether it was imported or re-synced with its proprietary fields kept. It SHALL also sync the first N catalog entries (1 to 20, default 10) and report how many were imported and updated. Failures SHALL be shown next to the form that caused them.

#### Scenario: Import
- **WHEN** a user syncs "Pikachu" and the API answers 201
- **THEN** the record's page opens with "Pikachu was imported from PokeAPI."

#### Scenario: Re-sync
- **WHEN** a user syncs "25" and the API answers 200
- **THEN** the record's page opens with "Pikachu was re-synced from PokeAPI. Its proprietary fields were kept."

#### Scenario: Sync the first three
- **WHEN** a user syncs the first 3 catalog entries and the API reports 2 created and 1 updated
- **THEN** a status says "Synced 3 Pokémon: 2 imported, 1 updated." and the list is loaded again

### Requirement: A record's page
The page at `/local/:id` SHALL show the PokeAPI data as read-only, with when it was last synced and a "Re-sync from PokeAPI" action, and the proprietary fields (region, habitat notes, notes, localized names and tags) with fallbacks when empty. It SHALL say who added the record and when it was last updated, offer "Edit", and link to the public page. An unknown record SHALL show that it isn't in the local Pokédex.

#### Scenario: Pikachu's record
- **WHEN** a user opens Pikachu's record
- **THEN** the page shows "#25", the stats table, "Kanto", the localized name "pt-BR: Pikachu", the tag "starter-line" and "Added by ash"

#### Scenario: Unknown record
- **WHEN** the API answers 404 for the record
- **THEN** the page says the record isn't in the local Pokédex and links to the list

### Requirement: Proprietary field rules
The create and edit forms SHALL apply the API's rules with its messages before sending: region at most 50 characters, habitat notes 500, notes 2000, at most 20 tags of 1 to 30 lower-case letters, digits or hyphens, and at most 10 localized names with valid, distinct locales and names of 1 to 50 characters. Every broken rule SHALL be listed at once, and the API's 400 field errors SHALL appear on the same fields.

#### Scenario: Several broken rules
- **WHEN** a user enters a 51-character region and the tag "Bad Tag!"
- **THEN** the form shows "must be at most 50 characters" on the region and "'Bad Tag!' must be 1 to 30 lower-case letters, digits or hyphens, starting with a letter or digit" on the tags, and sends nothing

#### Scenario: Normalised input
- **WHEN** a user saves the tags "Starter, starter,  KANTO " and the region "  Kanto  "
- **THEN** the request carries `internalTags` `["starter", "kanto"]` and `region` `Kanto`

### Requirement: Create a local record
The page at `/local/new` SHALL create a record from a Pokédex number and the proprietary fields, and then open it with a confirmation. A number that is already stored SHALL be reported on the number field.

#### Scenario: Add Bulbasaur
- **WHEN** a user creates number 1 with the region "Kanto" and the API answers 201
- **THEN** Bulbasaur's record opens with "Bulbasaur was added to your local Pokédex."

#### Scenario: Already stored
- **WHEN** the API answers 409 with an error for `pokeapiId`
- **THEN** the number field says "is already stored locally" and the input is kept

### Requirement: Edit with optimistic concurrency
The page at `/local/:id/edit` SHALL edit every proprietary field and save it with `If-Match` set to the version that was loaded. When someone else changed the record first, it SHALL say that the changes weren't saved, keep the user's input, and offer to load the latest version.

#### Scenario: Save
- **WHEN** a user loads version 3, changes the region and saves
- **THEN** the app sends `PUT` with `If-Match: "3"` and opens the record with "Your changes were saved."

#### Scenario: Someone else saved first
- **WHEN** the API answers 409
- **THEN** an alert says the changes weren't saved because someone else changed the record, and "Load the latest version" fills the form with the new values

### Requirement: Tags in place
A record's page SHALL add and remove tags in place, with the tag rule checked first. The change SHALL show at once and be sent as a merge patch with `If-Match`. If the API refuses it, the change SHALL be undone and the reason shown; after a 409 the latest version SHALL be shown.

#### Scenario: Remove a tag
- **WHEN** a user removes the tag "starter-line" from a record at version 3
- **THEN** the tag disappears at once and the app sends `PATCH` with `If-Match: "3"` and `{"internalTags": []}`

#### Scenario: Conflict
- **WHEN** the API answers 409 to a tag change
- **THEN** the tag change is undone, a message says someone else changed the record, and the latest version is loaded

### Requirement: Administrator-only deletion
A record's page SHALL offer "Delete" only to administrators. Deleting SHALL ask for confirmation, send `If-Match`, and return to the list with a message.

#### Scenario: Administrator deletes
- **WHEN** an administrator confirms deleting Pikachu at version 3
- **THEN** the app sends `DELETE` with `If-Match: "3"` and the list opens with "Pikachu was deleted."

#### Scenario: Regular user
- **WHEN** a user with the `USER` role opens a record
- **THEN** there is no "Delete" action
