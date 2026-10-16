# Spec Delta: project-presentation

## ADDED Requirements

### Requirement: Guide kept current
When a merged change in either repository alters a demo step, a decision or a number that `docs/PRESENTATION.md` quotes, the guide SHALL be updated in a change of its own. Each number SHALL be re-run, not copied, and each changed demo step SHALL be walked again on the full stack.

#### Scenario: The catalog search arrives
- **WHEN** pokedex-web's "Find a Pokémon" searches the catalog by part of a name
- **THEN** the guide's demo shows the search and the way back to it, and its numbers match fresh runs of the commands it names
