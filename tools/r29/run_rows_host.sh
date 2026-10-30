#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/rows-host}
mkdir -p "$OUT"
ROWS_TMP=$(mktemp -d)
trap 'rm -rf "$ROWS_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$ROWS_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$ROWS_TMP/shim:$CLASSES" -d "$ROWS_TMP/test" app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRowsR29.java tools/r29/qual_host/ResearchRowsR29HostTest.java
java -cp "$ROWS_TMP/shim:$ROWS_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.ResearchRowsR29HostTest | tee "$OUT/r29_rows_host.log"
