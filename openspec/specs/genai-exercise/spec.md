# genai-exercise Specification

## Purpose
Documents the interview's GenAI exercise and how this project used AI: the prompt for a Task Management API, the generated code as an isolated sample that CI builds and tests, every correction made to it, and an honest record of AI mistakes caught while building the Pokédex.

## Requirements

### Requirement: GenAI write-up
The repository SHALL contain `docs/GENAI.md` with: the exact prompt for generating a Task Management REST API (tasks with title, description, status and due date, linked to a user); a link to the generated sample; how the output was validated; each correction with its reason; and how edge cases, authentication and validation are handled.

#### Scenario: A reviewer reads the write-up
- **WHEN** a reviewer opens `docs/GENAI.md`
- **THEN** it shows the prompt verbatim, links to `docs/genai-sample/`, and lists every correction with before, after and why

### Requirement: Honest account of building this project with AI
`docs/GENAI.md` SHALL describe how the Pokédex itself was built with AI. That covers the OpenSpec specs used as guardrails, and real AI suggestions or outputs that were rejected or fixed during the build, each naming what caught it.

#### Scenario: Real examples
- **WHEN** a reviewer reads the section on building this project
- **THEN** each listed mistake names the change it happened in and how it was detected (a failing test, a fresh-clone build, a live run or a review)

### Requirement: Isolated, runnable sample
`docs/genai-sample/` SHALL be a standalone Maven project with its own `pom.xml`, outside the Pokédex build and packages. Its tests SHALL pass, and CI SHALL build it and run them.

#### Scenario: The sample builds on its own
- **WHEN** CI runs the sample's build with its own `pom.xml`
- **THEN** compilation and every test, including its Testcontainers integration test, pass

#### Scenario: The Pokédex is unaffected
- **WHEN** the Pokédex build runs
- **THEN** it neither compiles nor tests anything under `docs/genai-sample/`

### Requirement: Traceable corrections
The model's raw output SHALL be committed unmodified before any correction, and each correction SHALL be committed separately, so the history shows exactly what the AI produced and what review changed.

#### Scenario: Comparing raw and final output
- **WHEN** a reviewer compares the raw-output commit with the final sample
- **THEN** every difference corresponds to a correction listed in `docs/GENAI.md`

### Requirement: Open findings followed up
A finding recorded as open in `docs/GENAI.md` SHALL stay listed as open until a merged change fixes it. It SHALL then be recorded as fixed, naming the change and its pull request, and saying how the fix differs from the recorded candidate fix if it does.

#### Scenario: A finding is fixed
- **WHEN** a merged change fixes a finding listed as open
- **THEN** `docs/GENAI.md` no longer lists it as open, and records the fix with its change and pull request
