# Design: fix-submit-after-blur

## Context

**The forms:** sign-in, registration, create and edit use React Hook Form with `mode: 'onTouched'` and `criteriaMode: 'all'`. A field is validated when first left, then on every change. Every error message of a field is listed under it, so an error appearing on blur adds height above anything below that field, including the submit button.

**What a click is:** a browser fires `click` only when `mousedown` and `mouseup` land on the same element. The default action of `mousedown` on a button is to focus it, which blurs the field being edited.

**The bug:** blur → validation → re-render → the button moves → `mouseup` lands outside it → no click. The first click is lost, and only a second one submits.

## Decisions

### D1. Fix the cause, in the shared button
`SubmitButton` calls `event.preventDefault()` on `mousedown`. Focus stays in the field, so no blur-time validation runs before the click, and the layout doesn't move. The click then submits. `handleSubmit` validates all fields and focuses the first invalid one, so the user still lands on the problem.
- **Why not change when fields are validated:** validating only on submit would also avoid the shift, but it would remove the on-leave feedback that registration's password rules rely on, and it would fix only the forms it was applied to.
- **What's left alone:** keyboard users never had the problem, since Tab moves focus before Enter, and nothing changes for them. Touch devices dispatch a compatibility `mousedown` after `touchend`; preventing it likewise keeps focus put, and the `click` still follows.
- **The trade-off:** a pointer press no longer focuses the button. Nothing relies on that: the form handles submission and focus.

### D2. Tests
- **`SubmitButton.test.tsx` (the mechanism):** pressing the button cancels `mousedown`, so focus stays in the field; the button still submits by click and by Enter in a field.
- **`e2e/forms.e2e.ts` (the behaviour, in a real browser):** on `/local/new`, type an invalid region, then an invalid tag, then press **Add to my Pokédex** once, holding the button for 150 ms as a person does.
  - **Expected:** the submit happens on that one click, so focus moves to **Pokédex number**, the first invalid field, and no request is sent.
  - **Before the fix:** focus stays on the button.

## Risks / Trade-offs

- [A future form that relies on the submit button receiving focus] → none does today. The button's own comment says why it doesn't take focus on press.
