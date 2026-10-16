# Spec Delta: catalog-ui

## MODIFIED Requirements

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
