#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/terminal-host}
mkdir -p "$OUT"
TERMINAL_TMP=$(mktemp -d)
trap 'rm -rf "$TERMINAL_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$TERMINAL_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$TERMINAL_TMP/shim:$CLASSES" -d "$TERMINAL_TMP/test" tools/r29/qual_host/EventBridgeR29HostTest.java tools/r29/qual_host/TerminalFeedbackR29HostTest.java
java -cp "$TERMINAL_TMP/shim:$TERMINAL_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.TerminalFeedbackR29HostTest | tee "$OUT/r29_terminal_host.log"
