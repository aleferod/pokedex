# Tasks: update-guide-for-catalog-search

Documentation only. Each task is verified by running what the document claims: the demo steps, the commands behind the numbers, and the links.

## 1. Presentation guide

- [x] 1.1 Update the demo in `docs/PRESENTATION.md` (D1): the search in step 1, Eevee by name in step 2, one search when warming the cache. Verify: the steps work as written on a full stack started from pokedex-web's `main`.
- [x] 1.2 Add the search to the decisions, the code-review entry points and the likely questions. Update the numbers, the change counts and the date (D2). Verify: each number matches a fresh run, and each named file exists.

## 2. GenAI record

- [x] 2.1 Record the Save-click finding as fixed (D3) and add this round's AI mistakes (D4) to `docs/GENAI.md`. Verify: each row matches its commit message or pull request.

## 3. Verification

- [x] 3.1 Check the README for anything quoted that changed. Run lychee (offline for relative links, online for the cross-repository ones), `openspec validate update-guide-for-catalog-search --strict` and gitleaks. Verify: no broken links; valid; no leaks.

## Workflow follow-up

- Push, open the PR and wait for CI to go green.
- Archive as the PR's last commit, with `openspec validate --all --strict` run before committing. Wait for CI to be green again, then squash-merge.
- Send the final report.
