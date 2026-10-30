#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/fetch-host}
mkdir -p "$OUT"
FETCH_TMP=$(mktemp -d)
trap 'rm -rf "$FETCH_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$FETCH_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$FETCH_TMP/shim:$CLASSES" -d "$FETCH_TMP/test" tools/r29/qual_host/EventBridgeR29HostTest.java tools/r29/qual_host/FetchObservationR29HostTest.java
java -cp "$FETCH_TMP/shim:$FETCH_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.FetchObservationR29HostTest | tee "$OUT/r29_fetch_host.log"
