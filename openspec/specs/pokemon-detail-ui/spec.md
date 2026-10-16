# pokemon-detail-ui Specification

## Purpose
How people see one Pokémon in full in the web app (US02): its artwork, types, description, measures and base stats, its evolution family as a navigable tree, one canonical address per identifier, designed loading, not-found and error states, and a way back to the catalog page they came from.

## Requirements

### Requirement: Detail page address
The page at `/pokemon/:idOrName` SHALL show one Pokémon, identified by Pokédex number or name with the API's identifier rule. An identifier written in a non-canonical form SHALL be replaced in the address by its canonical form. The page SHALL be titled "<Name> · Pokédex" once the Pokémon is loaded.

#### Scenario: By number
- **WHEN** a visitor opens `/pokemon/1`
- **THEN** the app requests `/api/v1/pokemon/1` and the page is titled "Bulbasaur · Pokédex"

#### Scenario: Non-canonical identifier
- **WHEN** a visitor opens `/pokemon/Pikachu` or `/pokemon/025`
- **THEN** the address becomes `/pokemon/pikachu` or `/pokemon/25` respectively, and only the canonical identifier is requested

### Requirement: Detail contents
The detail page SHALL show the official artwork (with the name as alternative text, or a placeholder when there is none), the Pokédex number, the name, the types, the category, the description, the height in metres, the weight in kilograms and the abilities. It SHALL show the six base stats (HP, Attack, Defense, Special Attack, Special Defense, Speed) as a table of names and values with a bar for each, and their total. A missing category or description SHALL show a fallback text.

#### Scenario: Bulbasaur
- **WHEN** the detail of Bulbasaur has loaded
- **THEN** the page shows its artwork, "#1", the name "Bulbasaur", the types "grass" and "poison", "Seed Pokémon", its description, "0.7 m", "6.9 kg" and its abilities
- **AND** the "Base stats" table has the rows HP 45, Attack 49, Defense 49, Special Attack 65, Special Defense 65, Speed 45 and Total 318

#### Scenario: Missing artwork, category and description
- **WHEN** a Pokémon has no artwork, no category and no description
- **THEN** the page shows a placeholder labelled "No image available", "Unknown category" and "No description available."

### Requirement: Evolution chain
The detail page SHALL show the whole evolution family as a tree from its base form. Each stage SHALL link to its own page and show how it is reached; branches SHALL all be shown, and the stage being viewed SHALL be marked as the current one. A Pokémon whose family has a single member SHALL say that it does not evolve. Following a stage SHALL show that Pokémon and move focus to its name.

#### Scenario: Middle stage
- **WHEN** the detail of Pikachu has loaded
- **THEN** the chain shows Pichu, then "Level up with high friendship" Pikachu, then "Use Thunder Stone" Raichu, each linking to its page, with Pikachu marked as current

#### Scenario: Branching family
- **WHEN** the detail of Eevee has loaded
- **THEN** the chain shows Eevee with 8 next stages, among them "Use Water Stone" Vaporeon and "Level up with high affection knowing a Fairy move" Sylveon

#### Scenario: No evolutions
- **WHEN** the detail of Ditto has loaded
- **THEN** the page says "Ditto does not evolve."

#### Scenario: Following the chain
- **WHEN** a visitor on Bulbasaur's page follows the Ivysaur stage
- **THEN** the address becomes `/pokemon/2`, Ivysaur's detail is shown, and focus is on the heading "Ivysaur"

### Requirement: Detail states
While the detail loads, the page SHALL show a skeleton and announce that it is loading. An identifier that breaks the API's rule SHALL show a "not found" view without calling the API. A 404 SHALL show a "not found" view naming what was asked for. Both "not found" views SHALL offer the finder and a link to the catalog. Any other failure SHALL show an error with "Try again"; a 503 SHALL say that PokeAPI is temporarily unavailable.

#### Scenario: Invalid identifier
- **WHEN** a visitor opens `/pokemon/pika%20chu`
- **THEN** the page says that "pika chu" isn't a Pokédex number or a name, offers "Find a Pokémon", and no request is made

#### Scenario: Unknown Pokémon
- **WHEN** the API answers 404 for `missingno`
- **THEN** the page says there is no Pokémon called "missingno" and offers "Find a Pokémon" and a link to the catalog

#### Scenario: PokeAPI unavailable
- **WHEN** the API answers 503
- **THEN** an alert says that PokeAPI is temporarily unavailable, and "Try again" loads the detail again

### Requirement: Way back to the catalog
The detail page SHALL offer "Back to the catalog", leading to the catalog page, size and search the visitor came from, including after following the evolution chain, and to the first catalog page otherwise.

#### Scenario: From catalog page 3
- **WHEN** a visitor on `/pokemon?page=3&size=40` opens a card and then follows a stage of its evolution chain
- **THEN** "Back to the catalog" leads to `/pokemon?page=3&size=40`

#### Scenario: From a search
- **WHEN** a visitor searching `/pokemon?name=pika` opens Pikachu's card
- **THEN** "Back to the catalog" leads to `/pokemon?name=pika`

#### Scenario: Opened directly
- **WHEN** a visitor opens `/pokemon/25` directly
- **THEN** "Back to the catalog" leads to `/pokemon`
