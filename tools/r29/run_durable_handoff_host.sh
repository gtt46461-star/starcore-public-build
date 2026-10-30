#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/durable-handoff-host};mkdir -p "$OUT"
HANDOFF_TMP=$(mktemp -d);trap 'rm -rf "$HANDOFF_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
javac -nowarn -encoding UTF-8 -proc:none -d "$HANDOFF_TMP/shim" $(find tools/r29/scoped_schema_host/shim -name '*.java') tools/r29/schema_owner_host/shim/android/content/ContentValues.java tools/r29/schema_owner_host/shim/android/database/Cursor.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
HANDOFF_SOURCES=(ResearchCutoverR29 ResearchRouteR29 LegacyCapabilityAdapterR29 MigrationCopyR29 RuntimeR27)
for name in "${HANDOFF_SOURCES[@]}"; do HANDOFF_PATHS+=("app/src/main/java/com/ethk3/novaonepro/strategyv2/$name.java");done
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$HANDOFF_TMP/product" "${HANDOFF_PATHS[@]}"
javac -nowarn -encoding UTF-8 -proc:none -cp "$HANDOFF_TMP/shim:$HANDOFF_TMP/product:$CLASSES:$ANDROID_JAR" -d "$HANDOFF_TMP/test" tools/r29/qual_host/EventYieldR29HostTest.java tools/r29/qual_host/DurableHandoffR29HostTest.java
java -Xmx768m -cp "$HANDOFF_TMP/shim:$HANDOFF_TMP/test:$HANDOFF_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.DurableHandoffR29HostTest | tee "$OUT/r29_durable_handoff_host.log"
mkdir -p "$HANDOFF_TMP/old-source" "$HANDOFF_TMP/old-product"
for name in ResearchCutoverR29 ResearchRouteR29 RuntimeR27;do git show "84c360e09eaff62fdb2ce63dffdf21572f282c52:app/src/main/java/com/ethk3/novaonepro/strategyv2/$name.java" > "$HANDOFF_TMP/old-source/$name.java";done
javac -nowarn -encoding UTF-8 -proc:none -cp "$HANDOFF_TMP/product:$CLASSES:$ANDROID_JAR" -d "$HANDOFF_TMP/old-product" "$HANDOFF_TMP/old-source/"*.java
set +e
java -Xmx768m -cp "$HANDOFF_TMP/shim:$HANDOFF_TMP/test:$HANDOFF_TMP/old-product:$HANDOFF_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.DurableHandoffR29HostTest --old-gap > "$OUT/old-84c360e0-sealed-owner-gap-control.log" 2>&1
HANDOFF_STATUS=$?
set -e
if [ "$HANDOFF_STATUS" -eq 0 ] || ! grep -Fq H0_SEALED_RETRY_MUST_NOT_LOSE_ROOT_MODEL_OR_TWO_EVENT_PAGES "$OUT/old-84c360e0-sealed-owner-gap-control.log" || ! grep -Fq 'rootACK=64|rpInbox=0|rootModel=2|rpModel=1' "$OUT/old-84c360e0-sealed-owner-gap-control.log";then cat "$OUT/old-84c360e0-sealed-owner-gap-control.log";exit 1;fi
echo "R29_DURABLE_HANDOFF|OLD_84_SEALED_OWNER_GAP_REJECTED|exit=$HANDOFF_STATUS"
