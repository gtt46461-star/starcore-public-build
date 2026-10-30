#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
cd "$ROOT"
OUT=${OUT:-evidence/r29/cutover-host}
mkdir -p "$OUT"
CUTOVER_TMP=$(mktemp -d)
trap 'rm -rf "$CUTOVER_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$CUTOVER_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$CUTOVER_TMP/shim:$CLASSES" -d "$CUTOVER_TMP/test" tools/r29/qual_host/EventBridgeR29HostTest.java tools/r29/qual_host/CutoverR29HostTest.java
java -cp "$CUTOVER_TMP/shim:$CUTOVER_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.CutoverR29HostTest | tee "$OUT/r29_cutover_host.log"
