#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/event-yield-host}
mkdir -p "$OUT"
EVENT_YIELD_TMP=$(mktemp -d)
trap 'rm -rf "$EVENT_YIELD_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
javac -nowarn -encoding UTF-8 -proc:none -d "$EVENT_YIELD_TMP/shim" $(find tools/r29/scoped_schema_host/shim -name '*.java') tools/r29/schema_owner_host/shim/android/content/ContentValues.java tools/r29/schema_owner_host/shim/android/database/Cursor.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$EVENT_YIELD_TMP/product" app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$EVENT_YIELD_TMP/shim:$EVENT_YIELD_TMP/product:$CLASSES:$ANDROID_JAR" -d "$EVENT_YIELD_TMP/test" tools/r29/qual_host/EventYieldR29HostTest.java
java -Xmx512m -cp "$EVENT_YIELD_TMP/shim:$EVENT_YIELD_TMP/test:$EVENT_YIELD_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.EventYieldR29HostTest | tee "$OUT/r29_event_yield_host.log"
# The actual CP07 Runtime monopolized the worker until cancellation; the same task fixture must reject it.
mkdir -p "$EVENT_YIELD_TMP/old-source" "$EVENT_YIELD_TMP/old-product"
git show '1d2f1565e308659b6e0989f03400213c60dfac1b:app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java' > "$EVENT_YIELD_TMP/old-source/RuntimeR27.java"
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$EVENT_YIELD_TMP/old-product" "$EVENT_YIELD_TMP/old-source/RuntimeR27.java"
set +e
java -Xmx512m -cp "$EVENT_YIELD_TMP/shim:$EVENT_YIELD_TMP/test:$EVENT_YIELD_TMP/old-product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.EventYieldR29HostTest > "$OUT/old-1d-event-yield-control.log" 2>&1
EVENT_YIELD_CONTROL=$?
set -e
if [ "$EVENT_YIELD_CONTROL" -eq 0 ] || ! grep -Eq EVENT_MUST_RELEASE_SOLE_WORKER_AFTER_ONE_BUSY_ATTEMPT "$OUT/old-1d-event-yield-control.log"; then
  cat "$OUT/old-1d-event-yield-control.log"
  exit 1
fi
echo "R29_EVENT_YIELD|OLD_1D_INFINITE_RETRY_REJECTED|exit=$EVENT_YIELD_CONTROL"
