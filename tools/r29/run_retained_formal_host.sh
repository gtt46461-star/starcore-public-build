#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/retained-formal-host}
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
mkdir -p "$OUT"
FORMAL_TMP=$(mktemp -d)
trap 'rm -rf "$FORMAL_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$FORMAL_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$FORMAL_TMP/shim:$CLASSES:$ANDROID_JAR" -d "$FORMAL_TMP/test" app/src/androidTest/java/com/ethk3/novaonepro/strategyv2/CriticalInputProductionTriggerFixtureR29.java app/src/androidTest/java/com/ethk3/novaonepro/strategyv2/RetainedFormalProductionFixtureR29.java tools/r29/qual_host/RetainedFormalCopiesR29HostTest.java
java -Xmx512m -cp "$FORMAL_TMP/shim:$FORMAL_TMP/test:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.RetainedFormalCopiesR29HostTest "$OUT/retained-formal-host.json" | tee "$OUT/r29_retained_formal_host.log"
