#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
cd "$ROOT"
OUT=${OUT:-evidence/r29/registration-host}
mkdir -p "$OUT"
REGISTRATION_TMP=$(mktemp -d)
trap 'rm -rf "$REGISTRATION_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$REGISTRATION_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$REGISTRATION_TMP/shim:$CLASSES" -d "$REGISTRATION_TMP/test" tools/r29/qual_host/EventBridgeR29HostTest.java tools/r29/qual_host/RegistrationR29HostTest.java
java -cp "$REGISTRATION_TMP/shim:$REGISTRATION_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.RegistrationR29HostTest | tee "$OUT/r29_registration_host.log"
