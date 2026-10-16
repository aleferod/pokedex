# web-delivery Specification

## Purpose
How the SPA is packaged and served: a multi-stage image with nginx, SPA fallback, caching rules, security headers and a same-origin proxy to the API.

## Requirements

### Requirement: Container image
The repository SHALL build a multi-stage Docker image: Node 24 builds the app, and `nginx:alpine` serves the static files on port 80. The image SHALL have a health check on `/healthz`.

#### Scenario: Image builds and is healthy
- **WHEN** `docker build` runs and the container starts
- **THEN** `GET /healthz` answers 200 and Docker reports the container healthy

### Requirement: SPA fallback and caching
nginx SHALL answer any path that is not a file with `index.html`, so deep links and reloads work. Hashed assets SHALL be cached for a year as immutable, and `index.html` SHALL NOT be cached.

#### Scenario: Reload on a deep link
- **WHEN** a browser requests `/pokemon/25` directly
- **THEN** nginx answers 200 with `index.html`

### Requirement: Same-origin API proxy
nginx SHALL proxy `/api/` to the URL in the `API_UPSTREAM` environment variable (default `http://api:8080`), forwarding the original host and client address. The API base URL used by the app SHALL be configurable at build time (`VITE_API_BASE_URL`).

#### Scenario: Proxying to the backend
- **WHEN** the container runs with `API_UPSTREAM=http://backend:8080` and a browser calls `/api/v1/pokemon`
- **THEN** nginx forwards the request to `http://backend:8080/api/v1/pokemon`

### Requirement: Security headers
Every response SHALL carry `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin` and `X-Frame-Options: DENY`.

#### Scenario: Headers present
- **WHEN** a browser requests `/`
- **THEN** the three headers are present

### Requirement: Full stack in one command
`docker-compose.full.yml` SHALL run PostgreSQL, pokedex-api and this web app together, with the demo data, from one clone of this repository and one command. Secrets SHALL come only from `.env`, with `.env.example` holding local placeholders. Every service SHALL report healthy, and the app SHALL reach the API only through its same-origin proxy.

#### Scenario: From a fresh clone
- **WHEN** a reviewer runs `cp .env.example .env && docker compose -f docker-compose.full.yml up --build`
- **THEN** PostgreSQL, the API and the web app become healthy, <http://localhost:8000> serves the app, and signing in as `trainer` shows the ten demo records in My Pokédex

#### Scenario: Missing secrets
- **WHEN** the command runs without a `.env`
- **THEN** Compose stops before starting anything and names the missing variable
