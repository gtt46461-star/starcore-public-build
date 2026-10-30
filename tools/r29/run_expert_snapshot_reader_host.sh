#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/expert-snapshot-reader-host}
mkdir -p "$OUT"
EXPERT_READER_TMP=$(mktemp -d)
trap 'rm -rf "$EXPERT_READER_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
javac -nowarn -encoding UTF-8 -proc:none -d "$EXPERT_READER_TMP/shim" $(find tools/r29/scoped_schema_host/shim -name '*.java') tools/r29/schema_owner_host/shim/android/content/ContentValues.java tools/r29/schema_owner_host/shim/android/database/Cursor.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$EXPERT_READER_TMP/product" app/src/main/java/com/ethk3/novaonepro/strategyv2/ExpertWorkshopR26.java app/src/main/java/com/ethk3/novaonepro/strategyv2/ExpertContinuationR26.java app/src/main/java/com/ethk3/novaonepro/strategyv2/LegacyResearchScopeR29.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$EXPERT_READER_TMP/shim:$EXPERT_READER_TMP/product:$CLASSES:$ANDROID_JAR" -d "$EXPERT_READER_TMP/test" tools/r29/qual_host/EventYieldR29HostTest.java tools/r29/qual_host/ExpertSnapshotReaderR29HostTest.java
java -Xmx768m -cp "$EXPERT_READER_TMP/shim:$EXPERT_READER_TMP/test:$EXPERT_READER_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.ExpertSnapshotReaderR29HostTest | tee "$OUT/expert-snapshot-reader-host.log"
mkdir -p "$EXPERT_READER_TMP/old-source" "$EXPERT_READER_TMP/old-product"
for name in ExpertWorkshopR26 ExpertContinuationR26; do git show "2f1e2ab9:app/src/main/java/com/ethk3/novaonepro/strategyv2/$name.java" > "$EXPERT_READER_TMP/old-source/$name.java"; done
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$EXPERT_READER_TMP/old-product" "$EXPERT_READER_TMP/old-source/ExpertWorkshopR26.java" "$EXPERT_READER_TMP/old-source/ExpertContinuationR26.java"
set +e
java -Xmx768m -cp "$EXPERT_READER_TMP/shim:$EXPERT_READER_TMP/test:$EXPERT_READER_TMP/old-product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.ExpertSnapshotReaderR29HostTest > "$OUT/old-2f1-shared-pool-control.log" 2>&1
EXPERT_READER_CONTROL=$?
set -e
if [ "$EXPERT_READER_CONTROL" -eq 0 ] || ! grep -Eq EXPERT_SNAPSHOT_BORROWED_BLOCKED_ROOT_RW_POOL "$OUT/old-2f1-shared-pool-control.log"; then
  cat "$OUT/old-2f1-shared-pool-control.log"
  exit 1
fi
echo "R29_EXPERT_SNAPSHOT|OLD_SHARED_ROOT_POOL_REJECTED|exit=$EXPERT_READER_CONTROL"
mkdir -p "$EXPERT_READER_TMP/old-route-source" "$EXPERT_READER_TMP/old-route-product"
git show "2cf09d56:app/src/main/java/com/ethk3/novaonepro/strategyv2/LegacyResearchScopeR29.java" > "$EXPERT_READER_TMP/old-route-source/LegacyResearchScopeR29.java"
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$EXPERT_READER_TMP/old-route-product" "$EXPERT_READER_TMP/old-route-source/LegacyResearchScopeR29.java"
set +e
java -Xmx768m -cp "$EXPERT_READER_TMP/shim:$EXPERT_READER_TMP/test:$EXPERT_READER_TMP/old-route-product:$EXPERT_READER_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.ExpertSnapshotReaderR29HostTest > "$OUT/old-2cf-authority-route-control.log" 2>&1
EXPERT_ROUTE_CONTROL=$?
set -e
if [ "$EXPERT_ROUTE_CONTROL" -eq 0 ] || ! grep -q ROUTE_PROBE_BORROWED_BLOCKED_ROOT_RW_POOL "$OUT/old-2cf-authority-route-control.log"; then
  cat "$OUT/old-2cf-authority-route-control.log"
  exit 1
fi
echo "R29_EXPERT_SNAPSHOT|OLD_SHARED_AUTHORITY_ROUTE_REJECTED|exit=$EXPERT_ROUTE_CONTROL"
