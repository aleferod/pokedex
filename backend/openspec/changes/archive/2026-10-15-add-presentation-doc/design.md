# Design: add-presentation-doc

## Context

The reviewers will see:
- **this repository**, the Java exercise and the entry point: README, ADRs 0001–0006, `docs/GENAI.md`, 17 specs and 9 archived changes
- **pokedex-web**: README, 8 specs, 6 archived changes, `docker-compose.full.yml`, `scripts/e2e.sh` and screenshots

The evidence already exists: pull requests with red/green commits, CI runs, coverage reports, archived designs and an AI-mistakes table. It is spread over two repositories.

## Goals / Non-Goals

**Goals:**
- A presenter can run the session from one document, in a fixed time, without improvising the demo.
- A reviewer can find the code behind each claim in one click.
- Every number in the document says how to reproduce it, and every relative link resolves.

**Non-Goals:**
- Slides, and repeating the READMEs. The guide links to them.

## Decisions

### D1. Where it lives
The guide is `docs/PRESENTATION.md` in this repository, because the exercise is the Java API and reviewers start here. It links into pokedex-web with absolute GitHub URLs to `main`, and within this repository with relative links, which lychee checks offline.

### D2. Shape
The sections, in presentation order:
1. **The 30-minute agenda:** each block with its time and goal.
2. **The demo:**
   - one command from a clone of pokedex-web
   - five steps covering US01–US04
   - what to say at each step
   - a fallback if the network or PokeAPI misbehaves: the API's cache, the screenshots, and the CI end-to-end run as recorded evidence
3. **The architecture:** a diagram of both repositories and the full stack (Mermaid, rendered by GitHub), then the backend's layers with the rule that enforces them, then the frontend's feature folders.
4. **Decisions worth discussing:** each with its reason and a link to its ADR or design decision.
5. **Quality evidence:** the numbers (tests, coverage, gates, end-to-end and accessibility), each with the command that reproduces it.
6. **Code-review entry points:** about ten files per repository, each with what to look at and why.
7. **How it was built:** spec-first and test-first, the human checkpoints, AI use, linking `docs/GENAI.md`.
8. **Likely questions:** the reviewer's likely questions, with short answers and links.

### D3. Numbers
The numbers come from the last verified runs:
- **the API:** 519 unit and slice tests, 63 integration tests, domain and application line coverage 100 %, and the whole code base 97.9 % (`./scripts/verify-in-docker.sh`, pokedex-api#9)
- **the web app:** 215 unit tests at 98.4 % / 89.6 % / 98.9 % / 98.3 % (`npm run test:coverage`), and 10 end-to-end tests (`scripts/e2e.sh`, the CI run on pokedex-web#6)

A re-run of the backend gate for this change confirms them.

### D4. The AI-mistakes table
New rows name the pokedex-web change and pull request, what the AI got wrong, what caught it and the fix, in the same columns as the existing rows. Only cases that really happened are included, from the pull requests and commit messages:
- the MSW 3 bypass
- the act()-hidden sign-out race
- the over-long requirement found only by validating the archived result
- the masked gate exit code
- the cross-file data race in end-to-end tests
- the e2e test-setup slips: the label locator and the duplicated Playwright core

## Risks / Trade-offs

- [Numbers go stale] → each one says where it comes from and how to reproduce it, and the document is dated.
- [Links into the other repository aren't checked offline] → they point at `main` paths the archives created, and lychee checks them online for this change.
