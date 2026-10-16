# Spec Delta: web-quality

## MODIFIED Requirements

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
