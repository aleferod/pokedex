# web-quality Specification

## Purpose
The gates every change must pass, locally in Docker and in CI: zero-warning lint, formatting, strict types, tests that fail on console output, coverage and secret scanning.

## Requirements

### Requirement: Zero-warning gates
The build SHALL fail on any ESLint warning or error (typescript-eslint strict type-checked, React hooks, accessibility, Fast Refresh rules), on any Prettier formatting difference, and on any TypeScript error in strict mode.

#### Scenario: A lint warning
- **WHEN** code introduces an unused variable
- **THEN** `npm run lint` fails

### Requirement: Tests fail on console output
Tests SHALL run with Vitest, React Testing Library and MSW, with unhandled network requests treated as errors and any `console.error` or `console.warn` failing the test that produced it. Coverage SHALL be at least 85 % for lines, statements and functions and 80 % for branches.

#### Scenario: A React key warning
- **WHEN** a component renders a list without keys during a test
- **THEN** that test fails

#### Scenario: An unmocked request
- **WHEN** a component calls an endpoint no MSW handler covers
- **THEN** the test fails

### Requirement: Docker-only toolchain
Every npm command SHALL be runnable through `scripts/npm-in-docker.sh` (Node 24 LTS), so the host needs only Docker.

#### Scenario: Running the tests
- **WHEN** a developer runs `./scripts/npm-in-docker.sh ci` and then `./scripts/npm-in-docker.sh test`
- **THEN** the tests run inside a `node:24-alpine` container against the working tree

### Requirement: Continuous integration
One CI workflow SHALL run on every push to `main` and every pull request, with the gates of both apps:
- this app's lint, format check, type-check, tests with coverage, production build and Docker image build
- the backend's own jobs
- `openspec validate --all --strict`
- a gitleaks scan of the full history
- the end-to-end suite against the full stack built from the same commit, keeping its report and traces when it fails

#### Scenario: A failing gate
- **WHEN** any of these steps fails
- **THEN** the CI run fails

#### Scenario: A change to both apps
- **WHEN** a pull request changes an API response and the app's use of it
- **THEN** the end-to-end job runs the app against the API of that pull request

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
