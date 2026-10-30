#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/evolution-dataset-pin-host}
mkdir -p "$OUT"
PIN_TMP=$(mktemp -d)
trap 'rm -rf "$PIN_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
javac -nowarn -encoding UTF-8 -proc:none -d "$PIN_TMP/shim" $(find tools/r29/schema_owner_host/shim -name '*.java') tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$PIN_TMP/shim:$CLASSES:$ANDROID_JAR" -d "$PIN_TMP/test" tools/r29/qual_host/EvolutionDatasetPinR29HostTest.java
java -Xmx768m -cp "$PIN_TMP/shim:$PIN_TMP/test:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.EvolutionDatasetPinR29HostTest | tee "$OUT/r29_evolution_dataset_pin_host.log"
