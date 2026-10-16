# Spec Delta: web-delivery

## MODIFIED Requirements

### Requirement: Full stack in one command
The root `docker-compose.yml` SHALL run PostgreSQL, pokedex-api and this web app together, with the demo data, from one clone of the repository and one command, building both apps from that checkout. Secrets SHALL come only from the root `.env`, with the root `.env.example` holding local placeholders. Every service SHALL report healthy, and the app SHALL reach the API only through its same-origin proxy.

#### Scenario: From a fresh clone
- **WHEN** a reviewer runs `cp .env.example .env && docker compose up --build` at the repository root
- **THEN** PostgreSQL, the API and the web app become healthy, <http://localhost:8000> serves the app, and signing in as `trainer` shows the ten demo records in My Pokédex

#### Scenario: Missing secrets
- **WHEN** the command runs without a `.env`
- **THEN** Compose stops before starting anything and names the missing variable

#### Scenario: Both apps from the same commit
- **WHEN** a commit changes both `backend/` and `frontend/`
- **THEN** the stack built from that commit runs both changes, with nothing fetched from another repository
