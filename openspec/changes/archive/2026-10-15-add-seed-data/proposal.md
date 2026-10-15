# Proposal: add-seed-data

## Why

The brief asks for seed data so the application can be demonstrated right away. A reviewer who runs `docker compose up` should be able to:
- log in as an administrator and as a regular trainer, with documented credentials
- find a populated local replica to browse, filter, edit and delete

Today every account must be registered by hand (and always as `USER`, so `DELETE` can't be shown), and the local replica starts empty.

## What Changes

- **Demo accounts:** `admin` / `Admin@123` with role `ADMIN`, and `trainer` / `Trainer@123` with role `USER`. Their passwords are hashed at startup like any other, so no hash or plain password is ever stored in the database by hand.
- **Demo local Pokémon:** the Kanto starter lines (#1–#9) and Pikachu (#25).
  - They're created through the normal create flow: upstream fields come fresh from PokeAPI, and proprietary fields come from the seed file.
  - The proprietary fields are real names in Japanese, French and German (taken from PokeAPI), a region, a habitat note, tags and a note.
- **Opt-in, idempotent, tolerant:**
  - Seeding runs at startup only when `app.seed.enabled` is true (`SEED_DEMO_DATA`). It's off by default and on in the Compose demo and `.env.example`.
  - Existing accounts and records are left untouched, so a restart neither duplicates nor overwrites edits.
  - If PokeAPI is unavailable, the accounts are still created, the missing Pokémon are logged and retried on the next start, and the application still starts.
  - A warning is logged whenever demo data is enabled, because the passwords are public.
- **Seed file:** `src/main/resources/seed/demo-data.json`, replaceable through `app.seed.location`.
- **New use case `ProvisionUser`:** creates an account with a chosen role, for operators and the seeder. Public registration still always creates `USER`.

## Capabilities

### New Capabilities
- `demo-data`: what is seeded, when, and how seeding behaves on restart and when PokeAPI is down.

### Modified Capabilities
- None. `user-accounts` registration is unchanged; `ProvisionUser` isn't exposed over HTTP.

## Out of Scope

- An admin endpoint for creating accounts or changing roles.
- Seeding the catalog cache, or seeding without PokeAPI (an offline snapshot).
- Resetting demo data on a schedule.

## Impact

- **Serves:** the brief's seed-data requirement and the demo script (log in as `trainer` to browse and edit, as `admin` to delete).
- **Code:**
  - `domain.user.User.provision`
  - `application`: `ProvisionUser` and `ProvisionUserCommand`
  - `adapter.in.seed`: `DemoDataSeeder` (an `ApplicationRunner`), `DemoData` and the JSON reader
  - `infrastructure.seed`: `DemoDataConfiguration` and `SeedProperties`
- **Configuration:** `app.seed.enabled` (`SEED_DEMO_DATA`, default false) and `app.seed.location`. Compose and `.env.example` enable it for the demo.
- **Dependencies and database:** none new.
