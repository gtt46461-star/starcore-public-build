#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/migrated-lease-host}
mkdir -p "$OUT"
LEASE_TMP=$(mktemp -d)
trap 'rm -rf "$LEASE_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
sha256sum app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java > "$OUT/product-source.sha256"
javac -nowarn -encoding UTF-8 -proc:none -d "$LEASE_TMP/shim" $(find tools/r29/scoped_schema_host/shim -name '*.java') tools/r29/schema_owner_host/shim/android/content/ContentValues.java tools/r29/schema_owner_host/shim/android/database/Cursor.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$LEASE_TMP/product" app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$LEASE_TMP/shim:$LEASE_TMP/product:$CLASSES:$ANDROID_JAR" -d "$LEASE_TMP/test" tools/r29/qual_host/EventYieldR29HostTest.java tools/r29/qual_host/MigratedLeaseR29HostTest.java
java -Xmx768m -cp "$LEASE_TMP/shim:$LEASE_TMP/test:$LEASE_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.MigratedLeaseR29HostTest | tee "$OUT/r29_migrated_lease_host.log"
mkdir -p "$LEASE_TMP/old-source" "$LEASE_TMP/old-product"
git show 'b8de949cb79d5f7d944440580ba7f3d83e43b298:app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java' > "$LEASE_TMP/old-source/RuntimeCoordinatorR26.java"
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$LEASE_TMP/old-product" "$LEASE_TMP/old-source/RuntimeCoordinatorR26.java"
set +e
java -Xmx768m -cp "$LEASE_TMP/shim:$LEASE_TMP/test:$LEASE_TMP/old-product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.MigratedLeaseR29HostTest --old-control > "$OUT/old-b8-migrated-lease-control.log" 2>&1
LEASE_OLD_STATUS=$?
set -e
if [ "$LEASE_OLD_STATUS" -eq 0 ] || ! grep -Fq 'M1_COPY_DRAIN_HANDOFF_CLAIMABLE_WITHOUT_30S_IDLE|FAILED' "$OUT/old-b8-migrated-lease-control.log"; then cat "$OUT/old-b8-migrated-lease-control.log"; exit 1; fi
echo "R29_MIGRATED_LEASE|OLD_B8_STALE_COPIED_LEASE_REJECTED|exit=$LEASE_OLD_STATUS"
sha256sum app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java > "$LEASE_TMP/final-source.sha256"
cmp "$OUT/product-source.sha256" "$LEASE_TMP/final-source.sha256"
