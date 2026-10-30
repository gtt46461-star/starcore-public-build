#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/runtime-evolution-dataset-pin-host}
mkdir -p "$OUT"
RUNTIME_PIN_TMP=$(mktemp -d)
trap 'rm -rf "$RUNTIME_PIN_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
javac -nowarn -encoding UTF-8 -proc:none -d "$RUNTIME_PIN_TMP/shim" $(find tools/r29/scoped_schema_host/shim -name '*.java') tools/r29/schema_owner_host/shim/android/content/ContentValues.java tools/r29/schema_owner_host/shim/android/database/Cursor.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$RUNTIME_PIN_TMP/shim:$CLASSES:$ANDROID_JAR" -d "$RUNTIME_PIN_TMP/test" tools/r29/qual_host/EvolutionDatasetPinR29HostTest.java tools/r29/qual_host/RuntimeEvolutionDatasetPinR29HostTest.java
java -Xmx768m -cp "$RUNTIME_PIN_TMP/shim:$RUNTIME_PIN_TMP/test:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.RuntimeEvolutionDatasetPinR29HostTest | tee "$OUT/r29_runtime_evolution_dataset_pin_host.log"
# Historical production Runtime with the old mismatch-catching fallback, in the identical actual task fixture.
mkdir -p "$RUNTIME_PIN_TMP/old-source" "$RUNTIME_PIN_TMP/old-classes"
git show 'e5f9292:app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java' > "$RUNTIME_PIN_TMP/old-source/RuntimeR27.java"
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$RUNTIME_PIN_TMP/old-classes" "$RUNTIME_PIN_TMP/old-source/RuntimeR27.java"
set +e
java -Xmx768m -cp "$RUNTIME_PIN_TMP/shim:$RUNTIME_PIN_TMP/test:$RUNTIME_PIN_TMP/old-classes:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.RuntimeEvolutionDatasetPinR29HostTest > "$OUT/old-e5f9292-runtime-control.log" 2>&1
RUNTIME_PIN_CONTROL_STATUS=$?
set -e
if [ "$RUNTIME_PIN_CONTROL_STATUS" -eq 0 ] || ! rg -q 'R2_ACTUAL_RUNTIME_TURN_RESUMES_OLD_PIN_AND_COMMITS_NEXT_GATE\|FAILED' "$OUT/old-e5f9292-runtime-control.log"; then
  cat "$OUT/old-e5f9292-runtime-control.log"
  exit 1
fi
echo "R29_RUNTIME_EVOLUTION_PIN|OLD_E5_RUNTIME_FALLBACK_REJECTED|exit=$RUNTIME_PIN_CONTROL_STATUS"
