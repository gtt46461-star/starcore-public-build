#!/usr/bin/env bash
# R29-010/035 HOST test on real SQLite: production classes + JDBC-backed android.database shim (never packaged).
# Env: CLASSES = compiled app classes (javac output of the app sources), SQLITE_JDBC = sqlite-jdbc jar, OUT = log dir.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd); cd "$ROOT"
OUT=${OUT:-evidence/r29/host}; mkdir -p "$OUT"
T=$(mktemp -d)
javac -nowarn -encoding UTF-8 -proc:none -d "$T/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$T/shim:$CLASSES" -d "$T/test" tools/r29/qual_host/QualInboxR29HostTest.java
java -cp "$T/shim:$T/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.QualInboxR29HostTest | tee "$OUT/r29_qual_host.log"
