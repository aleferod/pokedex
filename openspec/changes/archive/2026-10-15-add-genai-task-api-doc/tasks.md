# Tasks: add-genai-task-api-doc

## 1. Prompt and raw output

- [x] 1.1 Write the prompt as the first section of `docs/GENAI.md` (D3) and commit it on its own. Verify: committed before any sample code.
- [x] 1.2 Generate the sample from the prompt alone, in one pass, and commit it **unmodified** under `docs/genai-sample/` (D2). Verify: the commit holds only the raw output.

## 2. Validation and corrections

- [x] 2.1 Build the sample and run its generated tests in Docker (D4 steps 1–2). Record the results.
- [x] 2.2 Review against the prompt's checklist and add the edge-case tests it calls for (D4 steps 3–4). Verify: each test that exposes a defect is red.
- [x] 2.3 Fix each defect in its own commit (D4 step 5). Verify: every sample test is green with `./scripts/mvnw-in-docker.sh -f docs/genai-sample/pom.xml verify`.

## 3. Write-up and CI

- [x] 3.1 Complete `docs/GENAI.md`:
  - validation method and results
  - corrections table (before, after, why)
  - how edge cases, authentication and validation are handled
  - how this project was built with AI (workflow and a table of real mistakes)
  - Verify: every correction in the diff from the raw output appears in the table.
- [x] 3.2 Add the `GenAI sample` CI job and link `docs/GENAI.md` from the README. Verify: lychee offline shows no broken links.

## 4. Verification

- [x] 4.1 Run the Pokédex `./scripts/verify-in-docker.sh` (unaffected) and the sample's `verify`. Verify: both exit 0.
- [x] 4.2 Run `openspec validate add-genai-task-api-doc --strict` and gitleaks. Verify: valid, no leaks.

## Workflow follow-up

- Push, open the PR and wait for CI to go green (including the new job).
- Archive as the PR's last commit, wait for CI to be green again, then squash-merge.
- Send the status report.
