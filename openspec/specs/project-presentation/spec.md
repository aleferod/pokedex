# project-presentation Specification

## Purpose
How the project is presented and reviewed: one guide with a timed agenda, a demo of US01–US04 on the one-command full stack, both repositories' architecture and decisions, quality evidence anyone can reproduce, code-review entry points and likely questions.

## Requirements

### Requirement: Presentation and review guide
The repository SHALL contain `docs/PRESENTATION.md` with: a timed agenda for the live presentation; a demo script that starts the full stack with one command and walks through US01 to US04 with the demo accounts, including a fallback; the architecture of both repositories; the key decisions with links to their ADRs or designs; quality evidence where each number names the command that reproduces it; code-review entry points; and likely questions with answers.

#### Scenario: A reviewer opens the guide
- **WHEN** a reviewer follows the guide's demo steps from a fresh clone of pokedex-web
- **THEN** each step works as written with the demo accounts, and each code-review entry point links to an existing file

#### Scenario: Links
- **WHEN** lychee checks `docs/PRESENTATION.md`
- **THEN** every link resolves
