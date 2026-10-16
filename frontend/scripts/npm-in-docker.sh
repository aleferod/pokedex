#!/usr/bin/env sh
# Runs npm inside a Node 24 LTS container against this working tree, so the host only needs Docker.
#
#   ./scripts/npm-in-docker.sh <npm args...>      e.g. ./scripts/npm-in-docker.sh ci
#                                                       ./scripts/npm-in-docker.sh run test
#
# - Runs as the host user, so node_modules/, dist/ and coverage/ are never owned by root.
# - --network host: the Vite dev server (5173) and preview (4173) are reachable on localhost, and
#   the dev proxy reaches an API on localhost:8080.
# - npm's download cache lives in the named volume "pokedex-web-npm" between runs.
set -eu

IMAGE="${NODE_IMAGE:-node:24-alpine}"
CACHE_VOLUME="${NPM_CACHE_VOLUME:-pokedex-web-npm}"
PROJECT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

# A freshly created named volume is owned by root: hand it to the host user once (idempotent).
docker volume inspect "$CACHE_VOLUME" >/dev/null 2>&1 || docker volume create "$CACHE_VOLUME" >/dev/null
docker run --rm -v "$CACHE_VOLUME":/npm-cache "$IMAGE" chown "$(id -u):$(id -g)" /npm-cache

TTY_FLAG=""
if [ -t 0 ] && [ -t 1 ]; then TTY_FLAG="-it"; fi

exec docker run --rm $TTY_FLAG \
  --network host \
  -u "$(id -u):$(id -g)" \
  -e HOME=/tmp \
  -e npm_config_cache=/npm-cache \
  -e npm_config_update_notifier=false \
  -e npm_config_fund=false \
  -v "$CACHE_VOLUME":/npm-cache \
  -v "$PROJECT_DIR":/app -w /app \
  "$IMAGE" npm "$@"
