# Spec Delta: pokemon-detail-ui

## MODIFIED Requirements

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
