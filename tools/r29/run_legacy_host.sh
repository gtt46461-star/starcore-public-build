#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/legacy-host}
mkdir -p "$OUT"
LEGACY_TMP=$(mktemp -d)
trap 'rm -rf "$LEGACY_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$LEGACY_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$LEGACY_TMP/shim:$CLASSES" -d "$LEGACY_TMP/test" tools/r29/qual_host/EventBridgeR29HostTest.java tools/r29/qual_host/LegacySnapshotR29HostTest.java
# Android type declarations satisfy linkage of the production CAS class; all exercised I/O is Java/real SQLite.
java -cp "$LEGACY_TMP/shim:$LEGACY_TMP/test:$CLASSES:$SQLITE_JDBC:${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}" com.ethk3.novaonepro.strategyv2.LegacySnapshotR29HostTest | tee "$OUT/r29_legacy_host.log"
