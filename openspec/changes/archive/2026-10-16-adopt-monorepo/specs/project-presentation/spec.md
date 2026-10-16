# Spec Delta: project-presentation

## MODIFIED Requirements

### Requirement: Presentation and review guide
The repository root SHALL contain `docs/PRESENTATION.md` with: a timed agenda for the live presentation; a demo script that starts the full stack with one command and walks through US01 to US04 with the demo accounts, including a fallback; the architecture of both apps; the key decisions with links to their ADRs or designs; quality evidence where each number names the command that reproduces it; code-review entry points; and likely questions with answers.

#### Scenario: A reviewer opens the guide
- **WHEN** a reviewer follows the guide's demo steps from a fresh clone of this repository
- **THEN** each step works as written with the demo accounts, and each code-review entry point links to an existing file

#### Scenario: Links
- **WHEN** lychee checks `docs/PRESENTATION.md`
- **THEN** every link resolves

### Requirement: Guide kept current
When a merged change in either app alters a demo step, a decision or a number that `docs/PRESENTATION.md` quotes, the guide SHALL be updated in a change of its own. Each number SHALL be re-run, not copied, and each changed demo step SHALL be walked again on the full stack.

#### Scenario: The catalog search arrives
- **WHEN** pokedex-web's "Find a Pokémon" searches the catalog by part of a name
- **THEN** the guide's demo shows the search and the way back to it, and its numbers match fresh runs of the commands it names
