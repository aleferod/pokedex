# Spec Delta: app-shell

## ADDED Requirements

### Requirement: Reliable form submission
A form's submit button SHALL submit on the first click, even when leaving the field being edited makes new error messages appear and the layout move. The form SHALL then validate every field, list the problems, and move focus to the first invalid field. Validating a field when it is left by keyboard or by clicking another field SHALL stay as it is.

#### Scenario: Clicking Add right after an invalid tag
- **WHEN** a user types an invalid region and then an invalid tag on the create form and clicks "Add to my Pokédex" once
- **THEN** the form validates everything, focus moves to "Pokédex number", and no request is sent
