#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/report-host}
mkdir -p "$OUT"
REPORT_TMP=$(mktemp -d)
trap 'rm -rf "$REPORT_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$REPORT_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$REPORT_TMP/shim:$CLASSES" -d "$REPORT_TMP/test" tools/r29/qual_host/EventBridgeR29HostTest.java tools/r29/qual_host/CutoverR29HostTest.java tools/r29/qual_host/ReportSnapshotR29HostTest.java
java -cp "$REPORT_TMP/shim:$REPORT_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.ReportSnapshotR29HostTest | tee "$OUT/r29_report_host.log"
