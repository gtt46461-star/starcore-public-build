#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/decision-retention-host}
mkdir -p "$OUT"
DECISION_TMP=$(mktemp -d)
trap 'rm -rf "$DECISION_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$DECISION_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$DECISION_TMP/shim:$CLASSES" -d "$DECISION_TMP/test" tools/r29/qual_host/LegacyDecisionRetentionR29HostTest.java
java -cp "$DECISION_TMP/shim:$DECISION_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.LegacyDecisionRetentionR29HostTest | tee "$OUT/r29_decision_retention_host.log"
