#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/legacy-live-projection-host}
mkdir -p "$OUT"
PROJECTION_TMP=$(mktemp -d)
trap 'rm -rf "$PROJECTION_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$PROJECTION_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$PROJECTION_TMP/shim:$CLASSES" -d "$PROJECTION_TMP/test" tools/r29/qual_host/EventBridgeR29HostTest.java tools/r29/qual_host/LegacyLiveProjectionR29HostTest.java
java -cp "$PROJECTION_TMP/shim:$PROJECTION_TMP/test:$CLASSES:$SQLITE_JDBC:${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}" com.ethk3.novaonepro.strategyv2.LegacyLiveProjectionR29HostTest | tee "$OUT/r29_legacy_live_projection_host.log"
