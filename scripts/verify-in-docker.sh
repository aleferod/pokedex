#!/usr/bin/env sh
# Full build inside Docker: compile (warnings are errors), unit + slice + Testcontainers
# integration tests, ArchUnit, JaCoCo coverage gate, Spotless format check.
# Exit status is Maven's. Extra arguments are passed through (e.g. -Dtest=SomeTest).
set -eu
exec "$(dirname -- "$0")/mvnw-in-docker.sh" -B -ntp verify "$@"
