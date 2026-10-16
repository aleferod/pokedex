# Spec Delta: pokemon-detail

## Purpose

Shows everything a user needs about one Pokémon: its artwork, base stats, types, size, a readable description and its whole evolution family, including branching evolutions and how each one happens.

## ADDED Requirements

### Requirement: Detail by number or name
`GET /api/v1/pokemon/{idOrName}` SHALL be reachable without credentials and SHALL accept either a Pokédex number or a name. Names SHALL be matched case-insensitively.

#### Scenario: Name in any letter case
- **WHEN** a client calls `GET /api/v1/pokemon/PIKACHU` or `GET /api/v1/pokemon/Pikachu`
- **THEN** the response is the same as for `GET /api/v1/pokemon/pikachu`, with `id` 25

### Requirement: Detail contents
The detail SHALL contain:
- `id`, `name`
- `imageUrl`: the official artwork, or `null`
- `category`: the English genus
- `description`
- `types`, in slot order
- `height` in metres and `weight` in kilograms, one decimal each
- `abilities`, in slot order
- `stats`: `hp`, `attack`, `defense`, `specialAttack`, `specialDefense`, `speed`
- `evolutionChain`

#### Scenario: Bulbasaur by number
- **WHEN** an anonymous client calls `GET /api/v1/pokemon/1`
- **THEN** the response status is 200
- **AND** the body has `name` `bulbasaur`, an `imageUrl` ending in `/official-artwork/1.png`, `types` `["grass", "poison"]`, `height` 0.7, `weight` 6.9, `category` `Seed Pokémon`
- **AND** `stats` is `hp` 45, `attack` 49, `defense` 49, `specialAttack` 65, `specialDefense` 65, `speed` 45

### Requirement: Clean English description
`description` SHALL be the most recent English flavor text PokeAPI provides. Control characters (such as form feeds and line breaks) and runs of whitespace SHALL become single spaces, with no leading or trailing whitespace. When there is no English text, `description` SHALL be `null`.

#### Scenario: Text with line breaks
- **WHEN** the latest English flavor text is `While it is young, it uses the nutrients that are\nstored in the seed on its back in order to grow.`
- **THEN** `description` is `While it is young, it uses the nutrients that are stored in the seed on its back in order to grow.`

#### Scenario: Text with a form feed
- **WHEN** the latest English flavor text is `A strange seed was\nplanted on its\nback at birth.\fThe plant sprouts\nand grows with\nthis POKéMON.`
- **THEN** `description` is `A strange seed was planted on its back at birth. The plant sprouts and grows with this POKéMON.`

### Requirement: Evolution chain as a tree
`evolutionChain` SHALL be the Pokémon's whole evolution family, starting from its base form, whichever member was requested. Each stage SHALL have:
- `id`
- `name`
- `condition`: a human-readable evolution condition, `null` for the base form
- `evolvesTo`: the list of next stages, in PokeAPI's order, empty for a final stage

Branching evolutions SHALL appear as several entries in one `evolvesTo`.

#### Scenario: Branching evolution (Eevee)
- **WHEN** a client calls `GET /api/v1/pokemon/eevee`
- **THEN** `evolutionChain.name` is `eevee` and `evolutionChain.evolvesTo` lists 8 stages: `vaporeon`, `jolteon`, `flareon`, `espeon`, `umbreon`, `leafeon`, `glaceon`, `sylveon`
- **AND** `vaporeon` has condition `Use Water Stone`, `espeon` `Level up with high friendship during the day`, `umbreon` `Level up with high friendship at night`, `sylveon` `Level up with high affection knowing a Fairy move`

#### Scenario: Requesting a middle stage
- **WHEN** a client calls `GET /api/v1/pokemon/pikachu`
- **THEN** `evolutionChain` starts at `pichu` (condition `null`), whose only next stage is `pikachu` (`Level up with high friendship`), whose only next stage is `raichu` (`Use Thunder Stone`)

#### Scenario: Level-based evolution
- **WHEN** a client calls `GET /api/v1/pokemon/1`
- **THEN** the chain is `bulbasaur` → `ivysaur` (`Level 16`) → `venusaur` (`Level 32`)

#### Scenario: No evolutions
- **WHEN** a client calls `GET /api/v1/pokemon/ditto`
- **THEN** `evolutionChain.name` is `ditto` and `evolutionChain.evolvesTo` is empty

#### Scenario: Location-specific methods
- **WHEN** PokeAPI lists several methods for one evolution, some tied to specific in-game locations
- **THEN** `condition` describes the first method that does not depend on a location (Leafeon: `Use Leaf Stone`)

### Requirement: Unknown Pokémon
A well-formed identifier that matches no Pokémon SHALL answer 404 with a problem of type `not-found` whose `detail` names the identifier.

#### Scenario: No such Pokémon
- **WHEN** a client calls `GET /api/v1/pokemon/missingno`
- **THEN** the response status is 404, the problem type is `not-found`, and `detail` mentions `missingno`

### Requirement: Identifier format
An identifier SHALL be either a whole number from 1 to 99999, or a name of 1 to 50 characters that starts with a letter and contains only letters, digits and hyphens. Anything else SHALL answer 400, with an `errors[]` entry for `idOrName`, and without calling PokeAPI.

#### Scenario: Malformed identifiers
- **WHEN** a client calls `GET /api/v1/pokemon/{idOrName}` with `pika chu`, `<script>`, `0`, `-1` or a 51-character name
- **THEN** the response status is 400 and `errors` contains an entry for `idOrName`

### Requirement: Upstream unavailability for details
When PokeAPI is unreachable, too slow or failing, or when a related resource (species or evolution chain) of an existing Pokémon cannot be fetched, the detail endpoint SHALL answer 503 `service-unavailable` with `Retry-After`, exactly like the catalog.

#### Scenario: PokeAPI failing
- **WHEN** PokeAPI answers `/pokemon/1` with 500
- **THEN** `GET /api/v1/pokemon/1` answers 503 with a `Retry-After` header
