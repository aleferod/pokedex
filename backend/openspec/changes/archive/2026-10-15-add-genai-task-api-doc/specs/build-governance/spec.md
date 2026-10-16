# Spec Delta: build-governance

## ADDED Requirements

### Requirement: GenAI sample verified in CI
Continuous integration SHALL build the GenAI sample in `docs/genai-sample/` with its own `pom.xml` and run its tests on every push and pull request, as a job separate from the Pokédex build.

#### Scenario: A change breaks the sample
- **WHEN** a commit breaks compilation or a test of the GenAI sample
- **THEN** the CI job for the sample fails
