# demo-data Specification

## Purpose
Gives a fresh installation known demo accounts and a populated local replica, so the application can be demonstrated immediately. Seeding is opt-in, safe to repeat, and goes through the same rules as the API.

## Requirements

### Requirement: Demo data is opt-in
The service SHALL create demo data at startup only when `app.seed.enabled` is true (environment variable `SEED_DEMO_DATA`). It SHALL be false by default; the Compose stack and `.env.example` SHALL enable it. Whenever it is enabled, a warning SHALL be logged naming the demo accounts, but never their passwords.

#### Scenario: Disabled by default
- **WHEN** the service starts without `app.seed.enabled`
- **THEN** no demo account or demo local record is created

#### Scenario: Warning when enabled
- **WHEN** the service starts with `app.seed.enabled=true`
- **THEN** a warning names the accounts `admin` and `trainer` and says demo data must never be enabled on a public deployment

### Requirement: Demo accounts
With demo data enabled, the service SHALL ensure two accounts exist: `admin` (email `admin@pokedex.example`, password `Admin@123`, role `ADMIN`) and `trainer` (email `trainer@pokedex.example`, password `Trainer@123`, role `USER`). Their passwords SHALL be stored as BCrypt hashes, and SHALL follow the same password policy as registration.

#### Scenario: The admin can delete
- **WHEN** a client logs in as `admin` with `Admin@123`
- **THEN** it receives a token whose account has role `ADMIN`, and `DELETE /api/v1/local-pokemon/{id}` with it answers 204

#### Scenario: The trainer is a regular user
- **WHEN** a client logs in as `trainer` with `Trainer@123`
- **THEN** `GET /api/v1/auth/me` shows role `USER`

### Requirement: Demo local Pokémon
With demo data enabled, the service SHALL ensure local records exist for Pokémon #1 to #9 and #25. Their upstream fields SHALL be read from PokeAPI, their proprietary fields SHALL come from the seed file, and their `createdBy` SHALL be `admin`.

#### Scenario: A populated replica
- **WHEN** the demo stack has started and `trainer` calls `GET /api/v1/local-pokemon?tag=starter`
- **THEN** the response lists Bulbasaur, Charmander and Squirtle, each with localized names and a region

### Requirement: Seeding is idempotent
Seeding SHALL leave existing data untouched: an account whose username exists is skipped, even if its role or password differ, and a Pokémon already stored locally is skipped, even if its proprietary fields were edited. Restarting the service SHALL NOT create duplicates.

#### Scenario: Restart keeps edits
- **WHEN** a user changed the notes of the demo Bulbasaur and the service restarts with demo data enabled
- **THEN** there is still exactly one Bulbasaur, with the user's notes

### Requirement: Seeding tolerates PokeAPI outages
If PokeAPI cannot answer while seeding, the service SHALL still create the demo accounts and start normally. It SHALL log each Pokémon it could not create, and SHALL create those missing records on a later start.

#### Scenario: PokeAPI down at first start
- **WHEN** the service starts with demo data enabled while PokeAPI is unreachable
- **THEN** the service starts, `admin` and `trainer` can log in, and no local record exists yet
