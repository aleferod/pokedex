# Tasks: add-presentation-doc

Documentation only. Each task is verified by running what the document claims: the commands, the demo steps and the links.

## 1. Guide

- [x] 1.1 Write `docs/PRESENTATION.md` (D1–D3). Verify: the demo steps run on a fresh full stack (pokedex-web's `docker-compose.full.yml`) as written; every entry-point file exists; lychee finds no broken links (offline for relative links, online for the cross-repository ones).

## 2. README and GenAI record

- [x] 2.1 Update the README: the status, the full-stack pointer in the quick start, and the contents and roadmap entries. Verify: lychee offline, no broken links.
- [x] 2.2 Add the frontend's AI mistakes to `docs/GENAI.md` and mention the end-to-end and browser checks among the guardrails (D4). Verify: each row matches its pull request or commit message.

## 3. Verification

- [x] 3.1 Re-run `./scripts/verify-in-docker.sh` to confirm the backend numbers quoted. Run `openspec validate add-presentation-doc --strict` and gitleaks. Verify: BUILD SUCCESS with the quoted counts; valid; no leaks.

## Workflow follow-up

- Push, open the PR and wait for CI to go green.
- Archive as the PR's last commit, with `openspec validate --all --strict` run before committing (a new capability: write its Purpose). Wait for CI to be green again, then squash-merge.
- Send the final report.
