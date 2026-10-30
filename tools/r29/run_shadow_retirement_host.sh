#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/shadow-retirement-host}
mkdir -p "$OUT"
SHADOW_TMP=$(mktemp -d)
trap 'rm -rf "$SHADOW_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$SHADOW_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$SHADOW_TMP/shim:$CLASSES" -d "$SHADOW_TMP/test" tools/r29/qual_host/ShadowRetirementAuthorityR29HostTest.java
java -cp "$SHADOW_TMP/shim:$SHADOW_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.ShadowRetirementAuthorityR29HostTest | tee "$OUT/r29_shadow_retirement_host.log"
