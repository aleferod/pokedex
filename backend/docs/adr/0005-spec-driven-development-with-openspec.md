# ADR-0005: Spec-driven development with OpenSpec

- **Status:** Accepted
- **Date:** 2026-10-14

## Context

The project is built with heavy use of an AI coding assistant. Left alone, AI assistants drift. They invent scope, quietly drop edge cases, and "fix" tests until they pass, because the requirements only live in chat history. The interview panel also looks for evidence of critical thinking about AI output and for a clear link from user stories to code.

## Decision

Every capability is delivered as an [OpenSpec](https://github.com/Fission-AI/OpenSpec) change (`openspec/changes/<id>/`), and work only starts once its plan exists. Each change contains:
- `proposal.md`: why, what, and what's out of scope
- delta specs: `SHALL` requirements with `WHEN/THEN` scenarios
- `design.md`: decisions, patterns and trade-offs
- `tasks.md`: tests listed before implementation

The workflow for each change:
1. Propose.
2. `openspec validate <id> --strict`. CI runs the same check.
3. Implement test-first, ticking tasks as they're done.
4. Archive, which folds the deltas into `openspec/specs/`.

The archive is the last commit of the change's pull request, so `main` always holds code and specs that agree. Project-wide constraints (stack, architecture rules, Definition of Done) live in `openspec/config.yaml`, and the assistant must read them for every change.

## Consequences

- Positive:
  - Each spec scenario maps to at least one test.
  - Reviewers can trace a user story → requirement → scenario → test → commit.
  - Scope changes are visible as spec diffs.
- Positive: the AI is held to the agreed spec. Where an AI suggestion conflicted with the spec, the spec won, and those cases are recorded in `docs/GENAI.md`.
- Negative: more up-front writing per change, and specs can go stale if they're edited by hand after archiving.
- Mitigation: CI fails on invalid specs. Archived changes stay in `openspec/changes/archive/` as history.
