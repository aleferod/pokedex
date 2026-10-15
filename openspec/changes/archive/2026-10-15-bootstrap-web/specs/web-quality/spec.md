# Spec Delta: web-quality

## ADDED Requirements

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
CI SHALL run on every push to `main` and every pull request: lint, format check, type-check, tests with coverage, production build, Docker image build, `openspec validate --all --strict`, and a gitleaks scan of the full history.

#### Scenario: A failing gate
- **WHEN** any of these steps fails
- **THEN** the CI run fails
