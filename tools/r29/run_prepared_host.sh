#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/prepared-host}
mkdir -p "$OUT"
PREPARED_TMP=$(mktemp -d)
trap 'rm -rf "$PREPARED_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$PREPARED_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$PREPARED_TMP/shim:$CLASSES" -d "$PREPARED_TMP/test" tools/r29/qual_host/EventBridgeR29HostTest.java tools/r29/qual_host/PreparedForecastR29HostTest.java
java -cp "$PREPARED_TMP/shim:$PREPARED_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.PreparedForecastR29HostTest | tee "$OUT/r29_prepared_host.log"
