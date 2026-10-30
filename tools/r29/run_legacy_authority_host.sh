#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/legacy-authority-host}
mkdir -p "$OUT"
AUTHORITY_TMP=$(mktemp -d)
trap 'rm -rf "$AUTHORITY_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$AUTHORITY_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$AUTHORITY_TMP/shim:$CLASSES" -d "$AUTHORITY_TMP/test" tools/r29/qual_host/EventBridgeR29HostTest.java tools/r29/qual_host/LegacyAuthorityProjectionR29HostTest.java
java -cp "$AUTHORITY_TMP/shim:$AUTHORITY_TMP/test:$CLASSES:$SQLITE_JDBC:${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}" com.ethk3.novaonepro.strategyv2.LegacyAuthorityProjectionR29HostTest | tee "$OUT/r29_legacy_authority_host.log"
