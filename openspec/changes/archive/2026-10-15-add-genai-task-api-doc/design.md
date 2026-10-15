# Design: add-genai-task-api-doc

## Context

The brief asks for a GenAI write-up in the backend repository, with a sample of generated code kept apart from the Pokédex domain. Throughout the build, AI mistakes have been recorded as they were caught: in commit messages, PR descriptions, the OpenSpec designs and a running list.

## Goals / Non-Goals

**Goals:**
- A prompt good enough to be reused as is.
- A sample that really compiles and passes its tests, with corrections that are real findings rather than staged ones.
- An honest record of this build, including where the AI was wrong.

**Non-Goals:**
- Making the sample part of the product.

## Decisions

### D1. The sample is a real, standalone project
- **Layout:** `docs/genai-sample/` has its own `pom.xml`, using the same Spring Boot line as the Pokédex (4.1.1) so reviewers see one stack.
  - It's outside the Pokédex reactor (the root `pom.xml` has no modules), and outside its packages (`com.example.tasks`). Spotless, JaCoCo and ArchUnit of the Pokédex never see it.
- **How it's built:** `./scripts/mvnw-in-docker.sh -f docs/genai-sample/pom.xml verify` locally, and a separate CI job.
  - Its integration test uses Testcontainers PostgreSQL, like the Pokédex (ADR-0004).

### D2. Provenance: prompt → raw output → corrections
| Step | Commit |
|---|---|
| 1 | the prompt, as the first section of `docs/GENAI.md` |
| 2 | the model's output for that prompt, **unmodified**, even if it doesn't compile |
| 3 | validation: build, run the generated tests, add the edge-case tests the review calls for, and review against the prompt's checklist |
| 4 | each defect found: a separate `fix(genai-sample):` commit |

The model is the same one building this project (Claude, through Claude Code). It produces the raw output in one pass from the prompt alone, without looking at the Pokédex code. `GENAI.md` says so plainly.

### D3. The prompt's structure
The prompt is the deliverable, so it's explicit:
- role and goal
- exact stack and versions
- domain fields with limits
- authentication and ownership rules (404, not 403, for another user's task)
- every endpoint and status code
- validation and the error format
- code structure rules
- the tests that must exist
- output format ("complete files with paths, no TODOs, list assumptions")

Each requirement in it is checkable, so the output can be validated against it item by item.

### D4. Validation method
1. **Build:** compile with warnings visible, and resolve dependencies against Spring Boot 4.1.1 for real.
2. **Run the generated tests,** unchanged, and record which fail.
3. **Review against the prompt's checklist:** every status code, every validation rule, ownership on every endpoint, no entity in responses, migration matching the entities (`ddl-auto=validate` must pass).
4. **Add the edge-case tests the review calls for,** such as another user's task, a past due date on create versus on unchanged update, an unknown enum value, an unknown member, an oversized page, a stale version and a missing user row. Each one is written red first where it exposes a defect.
5. **Fix, one commit per defect,** each recorded in `GENAI.md` as before, after and why.

### D5. The project section
- **The workflow:** OpenSpec as guardrails (proposal, design, tasks, spec deltas, `validate --strict`, archive as the last PR commit), red-first TDD with mutation checks, Docker-only verification, fresh-clone and live checks, and human checkpoints (scope, irreversible choices, the 409-vs-412 decision).
- **A table of real mistakes:** what the AI got wrong, which change it happened in, how it was caught and the fix.

## Risks / Trade-offs

- [The same model generates and reviews] → validation is mechanical where possible: compiler, tests, Testcontainers, CI. The review checklist comes from the prompt, and the whole history is in the PR for a human reviewer.
- [The sample could drift from Spring Boot updates] → CI builds it on every push.
