# Tasks: fix-submit-after-blur

## 1. Tests first

- [x] 1.1 Write `SubmitButton.test.tsx` and `e2e/forms.e2e.ts` (D2). Verify: the unit test is red, and the end-to-end test is red against the current stack, with focus left on the button.

## 2. Fix

- [x] 2.1 Cancel `mousedown` in `SubmitButton` (D1). Verify: 1.1 green; all unit gates and the whole end-to-end suite green.

## 3. Verification

- [x] 3.1 Check it by hand in a real browser on the edit and registration forms. Run `openspec validate fix-submit-after-blur --strict` and gitleaks. Verify: one click submits and focuses the first problem; valid; no leaks.

## Workflow follow-up

- Push, open the PR and wait for CI to go green.
- Archive as the PR's last commit, with `openspec validate --all --strict` run before committing. Wait for CI to be green again, then squash-merge.
