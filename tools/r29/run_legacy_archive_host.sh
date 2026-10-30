#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/legacy-archive-host}
CLASSES=${CLASSES:-app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes}
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
mkdir -p "$OUT"
ARCHIVE_TMP=$(mktemp -d)
trap 'rm -rf "$ARCHIVE_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$ARCHIVE_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$ARCHIVE_TMP/shim:$CLASSES:$ANDROID_JAR" -d "$ARCHIVE_TMP/test" tools/r29/qual_host/LegacyArchiveR29HostTest.java
java -Xmx512m -cp "$ARCHIVE_TMP/shim:$ARCHIVE_TMP/test:$CLASSES:$ANDROID_JAR" com.ethk3.novaonepro.LegacyArchiveR29HostTest app/src/main/assets/research/r967_history/R908_125_algorithms.tsv "$OUT/legacy-archive-continuity.json" | tee "$OUT/legacy-archive-host.log"
