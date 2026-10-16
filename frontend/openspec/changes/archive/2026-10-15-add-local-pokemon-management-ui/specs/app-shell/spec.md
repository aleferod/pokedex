# Spec Delta: app-shell

## MODIFIED Requirements

### Requirement: Current page in the navigation
In the main navigation, the link to the page being shown SHALL be marked as the current page. The link to the section a page belongs to (such as "Catalog" on a Pokémon's page, or "My Pokédex" on a local record) SHALL be marked as the current item without claiming to be the current page.

#### Scenario: Current page and current section
- **WHEN** a visitor is on `/pokemon`
- **THEN** the "Catalog" link has `aria-current="page"`
- **AND WHEN** the visitor is on a Pokémon's page, such as `/pokemon/25`
- **THEN** the "Catalog" link has `aria-current="true"`, so only the evolution stage being viewed is the current page

#### Scenario: My Pokédex
- **WHEN** a signed-in user is on `/local`
- **THEN** the "My Pokédex" link has `aria-current="page"`
- **AND WHEN** the user is on a local record's page
- **THEN** the "My Pokédex" link has `aria-current="true"`
