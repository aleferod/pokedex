# Proposal: add-presentation-doc

## Why

The exercise ends with a live presentation and a code review. Both repositories are complete:
- the API: nine changes, 17 specs
- the web app: six changes, 8 specs, with `docker-compose.full.yml` running the whole product in one command

But nothing yet tells a presenter or a reviewer where to start, what to show in a limited time, or where the evidence for each claim lives. This repository's README still says the full stack "arrives with the frontend changes", and `docs/GENAI.md` lists only the backend's AI mistakes.

## What Changes

- **`docs/PRESENTATION.md`:**
  - a timed agenda for the presentation
  - the five-step demo on the full stack (the one command and the demo accounts)
  - the architecture of both repositories, linked to the code
  - the key decisions, linked to ADRs and design documents
  - the quality evidence, with the command that reproduces each number
  - code-review entry points in both repositories
  - the questions a reviewer is likely to ask, with short answers and links
- **README:**
  - the status line covers the whole product
  - the quick start points to the full stack in pokedex-web, replacing "arrives with the frontend changes"
  - a "Presentation" entry in the contents and the roadmap
- **`docs/GENAI.md`:**
  - the AI-mistakes table gains the frontend's real cases, each with its pull request, what caught it and the fix
  - the guardrails mention the end-to-end tests and the browser checks

## Capabilities

### New Capabilities
- `project-presentation`: the presentation and review guide.

### Modified Capabilities
- None. The new `GENAI.md` rows fall under the existing `genai-exercise` requirement "Honest account of building this project with AI", which already covers the whole Pokédex.

## Out of Scope

- Slides. The guide is Markdown that renders on GitHub and links to the code, and the demo is the product itself.
- Code changes in either repository.

## Impact

- **Files:** `docs/PRESENTATION.md`, `README.md` and `docs/GENAI.md` only.
- **Behaviour:** no runtime change.
