#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/critical-production-trigger-host}
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
mkdir -p "$OUT"
TRIGGER_TMP=$(mktemp -d)
trap 'rm -rf "$TRIGGER_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$TRIGGER_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$TRIGGER_TMP/shim:$CLASSES:$ANDROID_JAR" -d "$TRIGGER_TMP/test" app/src/androidTest/java/com/ethk3/novaonepro/strategyv2/CriticalInputProductionTriggerFixtureR29.java tools/r29/qual_host/CriticalProductionTriggerR29HostTest.java
java -Xmx512m -cp "$TRIGGER_TMP/shim:$TRIGGER_TMP/test:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.CriticalProductionTriggerR29HostTest "$OUT/critical-production-trigger.json" | tee "$OUT/r29_critical_production_trigger_host.log"
