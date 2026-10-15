# Spec Delta: catalog-ui

## ADDED Requirements

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
The catalog page SHALL offer a "Find a Pokémon" field that accepts a Pokédex number (1–99999) or a name (letters, digits and hyphens, starting with a letter, up to 50 characters, any case) and opens `/pokemon/<number or lower-case name>`. Anything else SHALL be rejected in place without navigating.

#### Scenario: Find by name
- **WHEN** a visitor types "Pikachu" and chooses "Find"
- **THEN** the app goes to `/pokemon/pikachu`

#### Scenario: Invalid identifier
- **WHEN** a visitor types "pika chu"
- **THEN** the field shows "Enter a Pokédex number or a name such as pikachu" and the app stays on the catalog
