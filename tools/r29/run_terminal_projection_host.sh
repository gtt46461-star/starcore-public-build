#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/terminal-projection-host}
mkdir -p "$OUT"
TERMINAL_PROJECTION_TMP=$(mktemp -d)
trap 'rm -rf "$TERMINAL_PROJECTION_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$TERMINAL_PROJECTION_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$TERMINAL_PROJECTION_TMP/shim:$CLASSES" -d "$TERMINAL_PROJECTION_TMP/test" tools/r29/qual_host/TerminalProjectionR29HostTest.java
java -cp "$TERMINAL_PROJECTION_TMP/shim:$TERMINAL_PROJECTION_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.TerminalProjectionR29HostTest | tee "$OUT/r29_terminal_projection_host.log"
