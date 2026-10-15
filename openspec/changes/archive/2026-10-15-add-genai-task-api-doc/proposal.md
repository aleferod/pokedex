# Proposal: add-genai-task-api-doc

## Why

The interview includes a GenAI exercise. The backend repository must contain `docs/GENAI.md` with:
1. the exact prompt I would use to generate a Task Management REST API (title, description, status, due date, linked to a user)
2. a representative sample of the output, kept apart from the Pokédex code
3. how that output was validated, what was corrected and why, and how edge cases, authentication and validation were handled

It must also record honestly how this project itself was built with AI: the OpenSpec specs used as guardrails, and real AI suggestions that were rejected or fixed.

## What Changes

- **`docs/GENAI.md`** with the four parts above, plus how the Pokédex was built with AI:
  - the workflow: spec first, red tests first, Docker-only verification, human checkpoints
  - a catalogue of real mistakes caught during the build, each with what caught it and the fix
- **`docs/genai-sample/`:** a standalone Maven project (Spring Boot 4.1, Java 21, PostgreSQL, Flyway, JWT resource server). It's generated from the prompt and isn't part of the Pokédex build or packages.
- **Provenance:**
  - the prompt is committed first
  - the model's raw output is committed **unmodified**
  - each correction found by building, testing and reviewing it is a separate commit
  - `GENAI.md` shows each correction as before and after, with the reason
- **CI:** a job builds the sample and runs its tests, unit and Testcontainers, so the sample can't rot.
- **README:** a link to `docs/GENAI.md`.

## Capabilities

### New Capabilities
- `genai-exercise`: what the GenAI write-up contains, and the rules for the sample (isolated, runnable, traceable corrections).

### Modified Capabilities
- `build-governance`: CI also verifies the GenAI sample.

## Out of Scope

- Using the generated Task API in the Pokédex, or deploying it.
- Comparing several AI tools.

## Impact

- **Serves:** the brief's GenAI exercise.
- **Code:** only under `docs/genai-sample/`. The Pokédex's source, tests and coverage are untouched.
- **CI:** one more job, about a minute.
