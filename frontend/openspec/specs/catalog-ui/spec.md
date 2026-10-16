# catalog-ui Specification

## Purpose
How people browse the PokeAPI catalog in the web app (US01): a responsive grid of cards with paging kept in the URL, loading, empty and error states, and a finder that opens a Pokémon by name or number.

## Requirements

### Requirement: Catalog grid
The `/pokemon` page SHALL show one page of the catalog as a list of cards, each with the sprite (with the Pokémon's name as alternative text, or a placeholder when there is none), the Pokédex number, the name, the category, the weight in kilograms and the abilities, and a link to that Pokémon's page. The page SHALL be reachable from the main navigation as "Catalog" and be titled "Catalog · Pokédex".

#### Scenario: First page
- **WHEN** a visitor opens `/pokemon`
- **THEN** the app requests `page=0&size=20` and shows a card for Bulbasaur with "#1", "Seed Pokémon", "6.9 kg" and the abilities "overgrow" and "chlorophyll", linking to `/pokemon/1`

#### Scenario: Missing sprite and category
- **WHEN** an entry has no sprite and no category
- **THEN** its card shows a placeholder image and "Unknown category", and the other cards are unaffected

### Requirement: Pagination in the URL
The current page and page size SHALL live in the URL (`?page=` 1-based, `?size=` one of 20, 40 or 60) so they can be shared and reloaded. Invalid values SHALL fall back to page 1 and size 20. The page SHALL offer Previous and Next (disabled at the ends), show "Page N of M", let the user choose the page size (going back to page 1), and move focus to the results heading after a change.

#### Scenario: Next page
- **WHEN** a visitor on page 1 of 68 chooses "Next"
- **THEN** the URL becomes `/pokemon?page=2`, the app requests `page=1&size=20`, and "Page 2 of 68" is shown

#### Scenario: Shared link
- **WHEN** a visitor opens `/pokemon?page=3&size=40`
- **THEN** the app requests `page=2&size=40`

#### Scenario: Nonsense in the URL
- **WHEN** a visitor opens `/pokemon?page=abc&size=7`
- **THEN** the app shows page 1 with 20 entries per page

### Requirement: Catalog states
While a page loads, the catalog SHALL show skeleton cards and announce that it is loading. A page with no entries SHALL show an empty state with a link to the last page. A failure SHALL show an error with a "Try again" button; when the API answers 503 the message SHALL say that PokeAPI is temporarily unavailable.

#### Scenario: Loading
- **WHEN** the catalog request has not answered yet
- **THEN** skeleton cards are shown and a status says "Loading Pokémon…"

#### Scenario: Past the end
- **WHEN** a visitor opens `/pokemon?page=500`
- **THEN** the page says there are no Pokémon on this page and links to page 68

#### Scenario: PokeAPI down
- **WHEN** the API answers 503
- **THEN** the page says "PokeAPI is temporarily unavailable. Please try again in a moment." with a "Try again" button that requests the page again

### Requirement: Find a Pokémon
The catalog page SHALL offer a "Find a Pokémon" field. Typing at least 3 characters of a name SHALL search the catalog by part of the name after a short pause, keeping the search in the URL (`?name=`) with the page size and starting from page 1; clearing the field SHALL show the whole catalog. A Pokédex number (1–99999) with Enter SHALL open `/pokemon/<number>`. Text the API wouldn't accept SHALL be explained in place without a request.

#### Scenario: Find by name
- **WHEN** a visitor types "Pikachu" and chooses "Find"
- **THEN** the URL becomes `/pokemon?name=pikachu` at once, and the results start with Pikachu

#### Scenario: Invalid identifier
- **WHEN** a visitor types "pika!"
- **THEN** the field says "Use letters, digits, hyphens or spaces" and no search request is made

#### Scenario: Search while typing
- **WHEN** a visitor types "pika" and pauses
- **THEN** the URL becomes `/pokemon?name=pika`, the app requests `name=pika&page=0&size=20`, the grid shows Pikachu, and a status says how many Pokémon match

#### Scenario: Too short to search
- **WHEN** a visitor types "pi"
- **THEN** no search request is made and the catalog stays as it was

#### Scenario: A number
- **WHEN** a visitor types "025" and presses Enter
- **THEN** the app goes to `/pokemon/25`

#### Scenario: Nothing matches
- **WHEN** a search for "zzz" finds nothing
- **THEN** the page says "No Pokémon match “zzz”." and offers "Clear search"

#### Scenario: A shared search
- **WHEN** a visitor opens `/pokemon?name=pika&size=40`
- **THEN** the field shows "pika" and the app requests `name=pika&page=0&size=40`
