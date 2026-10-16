# Proposal: fix-submit-after-blur

## Why

While walking the presentation's demo, clicking **Save changes** on the edit form right after typing an invalid tag did nothing.
- **What happens:** pressing the mouse button moves focus from the field to the button, so the field is "left". These forms validate a field when it's left (`mode: 'onTouched'`), so its error messages appear above the button and push it down. By the time the mouse button is released, the pointer is no longer over the button, and the browser fires no click.
- **Measured** in a real browser with a submit listener: the first click made 0 submits and focus stayed on the button. A second click made 1 submit and moved focus to the first invalid field.
- **Impact:** nothing wrong is ever sent, but the click is lost, and focus doesn't go to the first problem. The same can happen on every form built with the shared submit button: sign-in, registration, and adding or editing a local record. Keyboard users aren't affected.

## What Changes

- **The shared `SubmitButton` keeps focus in the field** when it's pressed with a pointer (`mousedown` → `preventDefault()`). Leaving the field then happens through the submit itself, so nothing moves under the pointer before the click lands. The form validates everything on submit and focuses the first invalid field, as it already does.
- **What stays the same:**
  - errors still appear as soon as a field is left by Tab or by clicking another field
  - keyboard submission (Enter, or Space on the button)
  - the busy label and disabled state
- **A new end-to-end test reproduces the human click**, with a delay between pressing and releasing, on the create form after leaving an invalid field. A unit test pins the mechanism.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `app-shell`: adds "Reliable form submission".

## Out of Scope

- Changing when fields are validated. Validating on submit only would also avoid the shift, but it would drop the immediate feedback the forms were designed for, and the registration page's password rules shown on leaving the field. It would also fix only the forms it was applied to.

## Impact

- **Code:** `src/shared/ui/form/SubmitButton.tsx`.
- **Tests:** `SubmitButton.test.tsx` and `e2e/forms.e2e.ts`.
- **Backend:** none.
