#!/usr/bin/env sh
# Prints line coverage from the JaCoCo CSV report as a Markdown table:
# the gated business layers (domain + application) and the whole code base.
#   ./scripts/coverage-summary.sh [target/site/jacoco/jacoco.csv]
set -eu
CSV="${1:-target/site/jacoco/jacoco.csv}"
if [ ! -f "$CSV" ]; then
  echo "No JaCoCo report at $CSV (run ./mvnw verify first)." >&2
  exit 1
fi
LC_ALL=C awk -F, '
  NR == 1 { next }
  {
    missed = $8; covered = $9
    allM += missed; allC += covered
    if ($2 ~ /\.(domain|application)(\.|$)/) { bizM += missed; bizC += covered }
  }
  function pct(c, m) { return (c + m) == 0 ? "n/a (no classes yet)" : sprintf("%.1f%% (%d/%d lines)", 100 * c / (c + m), c, c + m) }
  END {
    print "| Scope | Line coverage |"
    print "|---|---|"
    print "| domain + application (gate: >= 85%) | " pct(bizC, bizM) " |"
    print "| whole code base | " pct(allC, allM) " |"
  }' "$CSV"
