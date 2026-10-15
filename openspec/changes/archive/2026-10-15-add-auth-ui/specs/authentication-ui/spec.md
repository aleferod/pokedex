# Spec Delta: authentication-ui

## ADDED Requirements

### Requirement: Sign in
The `/login` page SHALL ask for a username and a password. On success it SHALL store the session, show the user as signed in, and go to the sanitised `next` path or home. Wrong credentials SHALL show "Invalid username or password." without revealing which part was wrong. While the request runs, the submit button SHALL be disabled and say "Signing in…".

#### Scenario: Successful sign-in
- **WHEN** a visitor signs in as `trainer` with the right password on `/login?next=/my-pokedex`
- **THEN** the app goes to `/my-pokedex` and the header shows "Signed in as trainer" and "Sign out"

#### Scenario: Wrong password
- **WHEN** the API answers 401 to the sign-in
- **THEN** the page shows "Invalid username or password." as an alert and stays on `/login`

#### Scenario: Server unavailable
- **WHEN** the API cannot be reached during sign-in
- **THEN** the page shows "Cannot reach the server. Check your connection and try again." as an alert

### Requirement: Registration with the backend's rules
The `/register` page SHALL validate username, email and password with the same rules as pokedex-api before sending anything, listing every unmet password rule at once. On success it SHALL sign the new account in and go home. A 409 SHALL mark the taken field ("is already taken"), and 400 field errors from the API SHALL appear next to their fields.

#### Scenario: Weak password
- **WHEN** a visitor types the password `abc` and leaves the field
- **THEN** the field lists "Must be 8 to 72 characters", "Must contain an upper-case letter", "Must contain a digit" and "Must contain a character that is neither a letter nor a digit", and nothing is sent

#### Scenario: Username taken
- **WHEN** the API answers 409 with an error for `username`
- **THEN** the username field shows "Is already taken" and keeps focus available on it

#### Scenario: Registered and signed in
- **WHEN** a visitor registers `misty` with valid values
- **THEN** the app signs in as `misty` and goes home

### Requirement: Session lifetime
The session SHALL survive a page reload in the same tab, and SHALL be discarded when its expiry has passed or its stored form is invalid. The app SHALL sign out when the token expires. Signing out SHALL clear the session and all cached server data and go home.

#### Scenario: Reload while signed in
- **WHEN** a signed-in user reloads the page before the token expires
- **THEN** the user is still signed in

#### Scenario: Token expires while the app is open
- **WHEN** the token's expiry time passes
- **THEN** the app signs the user out

#### Scenario: Sign out
- **WHEN** a signed-in user chooses "Sign out"
- **THEN** the header shows "Sign in" and "Register", the session is gone from storage, and the home page is shown

### Requirement: Token on every call and 401 handling
Every API call SHALL carry the session's bearer token. A 401 from any call SHALL end the session; on a protected page the user SHALL be taken to `/login?next=<that page>` with the notice "Your session has expired. Please sign in again."

#### Scenario: Revoked token
- **WHEN** a signed-in user's protected page gets a 401 from the API
- **THEN** the user lands on `/login` with the expiry notice and a `next` pointing back to that page

### Requirement: Protected routes
Protected pages SHALL redirect visitors who are not signed in to `/login?next=<path>`. After signing in, the user SHALL return to that path. A `next` value that is not a same-app path (for example `//evil.example` or `https://evil.example`) SHALL be ignored in favour of home.

#### Scenario: Visitor opens a protected page
- **WHEN** a visitor opens `/my-pokedex?tag=starter`
- **THEN** the app shows `/login?next=%2Fmy-pokedex%3Ftag%3Dstarter`

#### Scenario: Open redirect attempt
- **WHEN** a visitor signs in on `/login?next=//evil.example`
- **THEN** the app goes home

### Requirement: Accessible forms
Every field SHALL have a visible label, mark invalid state with `aria-invalid`, and connect its hint and error messages with `aria-describedby`. Form-level errors SHALL use `role="alert"`, and submitting an invalid form SHALL move focus to the first invalid field.

#### Scenario: Submitting an empty sign-in form
- **WHEN** a visitor submits the empty sign-in form
- **THEN** both fields are marked invalid with "Is required" and focus moves to the username field
