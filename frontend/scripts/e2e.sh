#!/usr/bin/env sh
# End-to-end tests against a fresh full stack (docker-compose.full.yml), in Playwright's own image.
#   scripts/e2e.sh                  the journeys and accessibility checks
#   scripts/e2e.sh --screenshots    regenerate docs/screenshots/ instead
#   scripts/e2e.sh -- <args>        extra arguments for `playwright test` (e.g. a file or -g)
# The stack runs as Compose project "pokedex-e2e" on ports 8100 (app) and 8180 (API) and is always
# removed afterwards, database included. Needs .env (cp .env.example .env).
set -eu
cd "$(dirname "$0")/.."

PLAYWRIGHT_IMAGE=mcr.microsoft.com/playwright:v1.63.0-noble
export COMPOSE_PROJECT_NAME=pokedex-e2e
export WEB_PORT="${E2E_WEB_PORT:-8100}"
export API_PORT="${E2E_API_PORT:-8180}"

screenshots=0
if [ "${1:-}" = "--screenshots" ]; then
  screenshots=1
  shift
fi
if [ "${1:-}" = "--" ]; then
  shift
fi

if [ ! -f .env ]; then
  echo "No .env: run  cp .env.example .env  first." >&2
  exit 2
fi

compose() {
  docker compose -f docker-compose.full.yml "$@"
}

cleanup() {
  echo "Removing the end-to-end stack..."
  compose down -v --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

echo "Starting a fresh stack (app on :$WEB_PORT, API on :$API_PORT)..."
compose up --build --wait --quiet-pull

docker run --rm --network host --ipc=host \
  -u "$(id -u):$(id -g)" -e HOME=/tmp -e NPM_CONFIG_UPDATE_NOTIFIER=false \
  -e CI="${CI:-}" \
  -e E2E_BASE_URL="http://localhost:$WEB_PORT" \
  -e E2E_API_URL="http://localhost:$API_PORT" \
  -e E2E_SCREENSHOTS="$screenshots" \
  -v "$PWD":/work -w /work \
  "$PLAYWRIGHT_IMAGE" npx playwright test "$@"
