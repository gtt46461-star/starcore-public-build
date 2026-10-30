#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/owner-host}
mkdir -p "$OUT"
OWNER_TMP=$(mktemp -d)
trap 'rm -rf "$OWNER_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$OWNER_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$OWNER_TMP/shim:$CLASSES" -d "$OWNER_TMP/test" tools/r29/qual_host/EventBridgeR29HostTest.java tools/r29/qual_host/OwnerR29HostTest.java
java -cp "$OWNER_TMP/shim:$OWNER_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.OwnerR29HostTest | tee "$OUT/r29_owner_host.log"
