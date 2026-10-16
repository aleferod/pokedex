# Spec Delta: web-delivery

## ADDED Requirements

### Requirement: Full stack in one command
`docker-compose.full.yml` SHALL run PostgreSQL, pokedex-api and this web app together, with the demo data, from one clone of this repository and one command. Secrets SHALL come only from `.env`, with `.env.example` holding local placeholders. Every service SHALL report healthy, and the app SHALL reach the API only through its same-origin proxy.

#### Scenario: From a fresh clone
- **WHEN** a reviewer runs `cp .env.example .env && docker compose -f docker-compose.full.yml up --build`
- **THEN** PostgreSQL, the API and the web app become healthy, <http://localhost:8000> serves the app, and signing in as `trainer` shows the ten demo records in My Pokédex

#### Scenario: Missing secrets
- **WHEN** the command runs without a `.env`
- **THEN** Compose stops before starting anything and names the missing variable
