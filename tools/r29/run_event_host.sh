#!/usr/bin/env bash
# Synthetic failure injection using production Java and two real SQLite connections. No signing/API inputs.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
cd "$ROOT"
OUT=${OUT:-evidence/r29/event-host}
mkdir -p "$OUT"
EVENT_TMP=$(mktemp -d)
trap 'rm -rf "$EVENT_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$EVENT_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$EVENT_TMP/shim:$CLASSES" -d "$EVENT_TMP/test" tools/r29/qual_host/EventBridgeR29HostTest.java
java -cp "$EVENT_TMP/shim:$EVENT_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.EventBridgeR29HostTest | tee "$OUT/r29_event_host.log"
