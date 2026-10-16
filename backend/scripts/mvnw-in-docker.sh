#!/usr/bin/env sh
# Runs the Maven Wrapper inside a Maven/JDK 21 container, so the host only needs Docker.
#
#   ./scripts/mvnw-in-docker.sh <maven args...>      e.g. ./scripts/mvnw-in-docker.sh -B verify
#
# - Docker socket + --network host: Testcontainers inside the build container starts sibling
#   containers on the host daemon and reaches their mapped ports via localhost (Linux hosts).
#   Docker Desktop (macOS/Windows): drop --network host and set
#   TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal, or simply run ./mvnw verify on the host.
# - Runs as the host user (plus the socket's group) so nothing in target/ ends up owned by root.
# - Dependencies are cached in the named volume "pokedex-m2" between runs.
set -eu

IMAGE="${MAVEN_IMAGE:-maven:3.9-eclipse-temurin-21}"
M2_VOLUME="${M2_VOLUME:-pokedex-m2}"
SOCKET="/var/run/docker.sock"
PROJECT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

if [ ! -S "$SOCKET" ]; then
  echo "Docker socket not found at $SOCKET; Testcontainers-based tests need it." >&2
  exit 1
fi

# A freshly created named volume is owned by root: hand it to the host user once (idempotent).
docker volume inspect "$M2_VOLUME" >/dev/null 2>&1 || docker volume create "$M2_VOLUME" >/dev/null
docker run --rm -v "$M2_VOLUME":/m2 "$IMAGE" chown "$(id -u):$(id -g)" /m2

TTY_FLAG=""
if [ -t 1 ]; then TTY_FLAG="-t"; fi

exec docker run --rm $TTY_FLAG \
  --network host \
  -u "$(id -u):$(id -g)" \
  --group-add "$(stat -c %g "$SOCKET")" \
  -v "$SOCKET":"$SOCKET" \
  -v "$M2_VOLUME":/var/maven/.m2 \
  -e HOME=/var/maven \
  -e MAVEN_CONFIG=/var/maven/.m2 \
  -e MAVEN_OPTS="-Duser.home=/var/maven" \
  -e TESTCONTAINERS_HOST_OVERRIDE=localhost \
  -v "$PROJECT_DIR":/app -w /app \
  "$IMAGE" ./mvnw "$@"
