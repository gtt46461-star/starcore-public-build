#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/critical-mirror-host}
mkdir -p "$OUT"
MIRROR_TMP=$(mktemp -d)
trap 'rm -rf "$MIRROR_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$MIRROR_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$MIRROR_TMP/shim:$CLASSES" -d "$MIRROR_TMP/test" tools/r29/qual_host/EventBridgeR29HostTest.java tools/r29/qual_host/CriticalInputMirrorR29HostTest.java
java -Xmx512m -cp "$MIRROR_TMP/shim:$MIRROR_TMP/test:$CLASSES:$SQLITE_JDBC:${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}" com.ethk3.novaonepro.strategyv2.CriticalInputMirrorR29HostTest | tee "$OUT/r29_critical_mirror_host.log"
