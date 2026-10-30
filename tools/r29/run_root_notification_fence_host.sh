#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/root-notification-fence-host};mkdir -p "$OUT"
NOTIFICATION_TMP=$(mktemp -d);trap 'rm -rf "$NOTIFICATION_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
javac -nowarn -encoding UTF-8 -proc:none -d "$NOTIFICATION_TMP/shim" $(find tools/r29/scoped_schema_host/shim -name '*.java') tools/r29/schema_owner_host/shim/android/content/ContentValues.java tools/r29/schema_owner_host/shim/android/database/Cursor.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
NOTIFICATION_SOURCES=(ResearchCutoverR29 ResearchRouteR29 LegacyCapabilityAdapterR29 MigrationCopyR29)
for name in "${NOTIFICATION_SOURCES[@]}";do NOTIFICATION_PATHS+=("app/src/main/java/com/ethk3/novaonepro/strategyv2/$name.java");done
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$NOTIFICATION_TMP/product" "${NOTIFICATION_PATHS[@]}"
javac -nowarn -encoding UTF-8 -proc:none -cp "$NOTIFICATION_TMP/shim:$NOTIFICATION_TMP/product:$CLASSES:$ANDROID_JAR" -d "$NOTIFICATION_TMP/test" app/src/androidTest/java/com/ethk3/novaonepro/strategyv2/CriticalInputProductionTriggerFixtureR29.java app/src/androidTest/java/com/ethk3/novaonepro/strategyv2/CanonicalInputProductionFixtureR29.java tools/r29/qual_host/RootNotificationFenceR29HostTest.java
java -Xmx768m -cp "$NOTIFICATION_TMP/shim:$NOTIFICATION_TMP/test:$NOTIFICATION_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.RootNotificationFenceR29HostTest | tee "$OUT/root-notification-fence-host.log"
mkdir -p "$NOTIFICATION_TMP/old-source" "$NOTIFICATION_TMP/old-product"
git show '51cca638e3dd2e22ff315a30235cf31b3974eb72:app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchCutoverR29.java' > "$NOTIFICATION_TMP/old-source/ResearchCutoverR29.java"
javac -nowarn -encoding UTF-8 -proc:none -cp "$NOTIFICATION_TMP/product:$CLASSES:$ANDROID_JAR" -d "$NOTIFICATION_TMP/old-product" "$NOTIFICATION_TMP/old-source/ResearchCutoverR29.java"
set +e
java -Xmx768m -cp "$NOTIFICATION_TMP/shim:$NOTIFICATION_TMP/test:$NOTIFICATION_TMP/old-product:$NOTIFICATION_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.RootNotificationFenceR29HostTest --old-notification > "$OUT/old-51-p0-notification-false-rejection.log" 2>&1
NOTIFICATION_STATUS=$?
set -e
if [ "$NOTIFICATION_STATUS" -eq 0 ] || ! grep -Fq 'N3_P0_ONLY_NOTIFICATION_MUST_NOT_BLOCK_SEALED_RESEARCH_OWNER' "$OUT/old-51-p0-notification-false-rejection.log" || ! grep -Fq 'OLD51_P0_ONLY_RESULT|ROOT_RESEARCH_ADVANCED_AFTER_SEAL' "$OUT/old-51-p0-notification-false-rejection.log";then cat "$OUT/old-51-p0-notification-false-rejection.log";exit 1;fi
echo "R29_ROOT_NOTIFICATION_FENCE|OLD51_ACTUAL_P0_NOTIFICATION_FALSE_REJECTION_REPRODUCED|exit=$NOTIFICATION_STATUS"
