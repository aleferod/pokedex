# user-accounts Specification

## Purpose
Lets people create an account with a unique username and email and a strong password. Credentials are stored safely, and every new account gets the regular user role.

## Requirements

### Requirement: Account registration
`POST /api/v1/auth/register` with `username`, `email` and `password` SHALL create an account with role `USER`. It SHALL answer 201 with the account's `id`, `username`, `email`, `role` and `createdAt`. The response SHALL NOT contain the password or any form of it.

#### Scenario: Successful registration
- **WHEN** a client registers with `username` `ash`, `email` `ash@pallet.town` and `password` `Pikachu@25`
- **THEN** the response status is 201
- **AND** the body contains a UUID `id`, `username` `ash`, `email` `ash@pallet.town`, `role` `USER` and an ISO-8601 `createdAt`
- **AND** the body contains no `password` or password-hash member

#### Scenario: Role cannot be chosen by the caller
- **WHEN** a client registers with an additional member `"role": "ADMIN"`
- **THEN** the response status is 400, and `errors` contains an entry for field `role`
- **AND** no account is created

### Requirement: Username rules
A username SHALL be 3 to 30 characters of ASCII letters, digits, dots, underscores or hyphens, after trimming surrounding whitespace. Usernames SHALL be case-insensitive and are stored in lower case.

#### Scenario: Invalid username
- **WHEN** a client registers with `username` `a!`
- **THEN** the response status is 400 and `errors` contains an entry for field `username`

#### Scenario: Username normalised
- **WHEN** a client registers with `username` `  Misty  `
- **THEN** the account is created with `username` `misty`

### Requirement: Email rules
An email address SHALL have the form `local@domain`, with a dot in the domain, and be at most 254 characters after trimming. Email addresses SHALL be case-insensitive and are stored in lower case.

#### Scenario: Invalid email
- **WHEN** a client registers with `email` `not-an-email`
- **THEN** the response status is 400 and `errors` contains an entry for field `email`

### Requirement: Password strength
A password SHALL be 8 to 72 characters long. It SHALL contain at least one upper-case letter, one lower-case letter, one digit and one character that is neither a letter nor a digit. A rejected password SHALL produce one `errors[]` entry on field `password` for each broken rule.

#### Scenario: Weak password
- **WHEN** a client registers with `password` `password`
- **THEN** the response status is 400
- **AND** `errors` contains separate `password` entries for the missing upper-case letter, digit and special character

#### Scenario: Password too long
- **WHEN** a client registers with a 73-character password that meets every other rule
- **THEN** the response status is 400 and an `errors` entry on `password` states the 8–72 length limit

#### Scenario: Demo passwords are strong enough
- **WHEN** `admin` registers with `Admin@123`, or `trainer` registers with `Trainer@123`
- **THEN** the password is accepted

### Requirement: Unique usernames and emails
The system SHALL reject a registration whose username or email is already used by another account (ignoring case) with 409. `errors[]` SHALL name the conflicting field. This SHALL also hold when two identical registrations arrive at the same time.

#### Scenario: Duplicate username
- **WHEN** `ash` exists and a client registers `ASH` with a different email
- **THEN** the response status is 409, the problem type is `conflict`, and `errors` contains an entry for field `username`

#### Scenario: Duplicate email
- **WHEN** `ash@pallet.town` is used and a client registers a new username with `Ash@Pallet.Town`
- **THEN** the response status is 409 and `errors` contains an entry for field `email`

#### Scenario: Concurrent duplicate registration
- **WHEN** the uniqueness check passes but the database rejects the insert because of a concurrent registration
- **THEN** the response status is 409, not 500

### Requirement: Safe password storage
Passwords SHALL be stored only as a salted, adaptive one-way hash (BCrypt). They SHALL never be stored, logged or returned in plain text.

#### Scenario: Stored credential
- **WHEN** an account is registered with `Pikachu@25`
- **THEN** the stored credential is a BCrypt hash that verifies `Pikachu@25`
- **AND** the stored credential is not equal to `Pikachu@25`
