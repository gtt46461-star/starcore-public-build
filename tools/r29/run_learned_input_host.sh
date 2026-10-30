#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/learned-input-host}
mkdir -p "$OUT"
LEARNED_INPUT_TMP=$(mktemp -d)
trap 'rm -rf "$LEARNED_INPUT_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$LEARNED_INPUT_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$LEARNED_INPUT_TMP/shim:$CLASSES" -d "$LEARNED_INPUT_TMP/test" tools/r29/qual_host/LegacyLearnedInputR29HostTest.java
java -cp "$LEARNED_INPUT_TMP/shim:$LEARNED_INPUT_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.LegacyLearnedInputR29HostTest | tee "$OUT/r29_learned_input_host.log"
