# Spec Delta: web-quality

## ADDED Requirements

### Requirement: End-to-end tests
Playwright tests SHALL drive a real browser against a fresh full stack (real API, real database, real PokeAPI) through the main journeys: browsing the catalog and a Pokémon's page, registering and signing in and out, and the local Pokédex (sync, filters, tags, editing with a real conflict, and deletion by an administrator). Each run SHALL start from a fresh database and remove it afterwards.

#### Scenario: A broken journey
- **WHEN** a change breaks a journey, for example signing out of a protected page lands on sign-in
- **THEN** `scripts/e2e.sh` fails and names the journey and step

### Requirement: Automated accessibility checks
The end-to-end suite SHALL run axe on every main page, with real data, forms showing their errors, and the delete confirmation open. Any serious or critical WCAG 2.1 A or AA violation SHALL fail it.

#### Scenario: A violation
- **WHEN** a page has a form field without a label
- **THEN** the accessibility check fails, naming the rule and the element

## MODIFIED Requirements

### Requirement: Continuous integration
CI SHALL run on every push to `main` and every pull request: lint, format check, type-check, tests with coverage, production build, Docker image build, `openspec validate --all --strict`, a gitleaks scan of the full history, and the end-to-end suite against the full stack, keeping its report and traces when it fails.

#### Scenario: A failing gate
- **WHEN** any of these steps fails
- **THEN** the CI run fails
