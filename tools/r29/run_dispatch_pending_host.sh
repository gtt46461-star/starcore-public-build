#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/dispatch-pending-host}
mkdir -p "$OUT"
PENDING_TMP=$(mktemp -d)
trap 'rm -rf "$PENDING_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$PENDING_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$PENDING_TMP/shim:$CLASSES" -d "$PENDING_TMP/test" tools/r29/qual_host/EventBridgeR29HostTest.java tools/r29/qual_host/CriticalInputMirrorR29HostTest.java tools/r29/qual_host/LegacyDispatchPendingR29HostTest.java
java -cp "$PENDING_TMP/shim:$PENDING_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.LegacyDispatchPendingR29HostTest | tee "$OUT/r29_dispatch_pending_host.log"
