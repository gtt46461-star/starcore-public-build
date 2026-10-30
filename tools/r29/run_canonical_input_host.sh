#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/canonical-input-host}
mkdir -p "$OUT"
CANONICAL_TMP=$(mktemp -d)
trap 'rm -rf "$CANONICAL_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
javac -nowarn -encoding UTF-8 -proc:none -d "$CANONICAL_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$CANONICAL_TMP/shim:$CLASSES:$ANDROID_JAR" -d "$CANONICAL_TMP/test" app/src/androidTest/java/com/ethk3/novaonepro/strategyv2/CriticalInputProductionTriggerFixtureR29.java app/src/androidTest/java/com/ethk3/novaonepro/strategyv2/CanonicalInputProductionFixtureR29.java tools/r29/qual_host/CanonicalInputR29HostTest.java
java -Xmx512m -cp "$CANONICAL_TMP/shim:$CANONICAL_TMP/test:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.CanonicalInputR29HostTest "$OUT/canonical-input.json" | tee "$OUT/canonical-input-host.log"
