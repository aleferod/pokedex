# Spec Delta: genai-exercise

## ADDED Requirements

### Requirement: Open findings followed up
A finding recorded as open in `docs/GENAI.md` SHALL stay listed as open until a merged change fixes it. It SHALL then be recorded as fixed, naming the change and its pull request, and saying how the fix differs from the recorded candidate fix if it does.

#### Scenario: A finding is fixed
- **WHEN** a merged change fixes a finding listed as open
- **THEN** `docs/GENAI.md` no longer lists it as open, and records the fix with its change and pull request
