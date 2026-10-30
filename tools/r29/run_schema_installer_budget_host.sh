#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/schema-installer-budget-host}
mkdir -p "$OUT"
INSTALLER_TMP=$(mktemp -d)
trap 'rm -rf "$INSTALLER_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
javac -nowarn -encoding UTF-8 -proc:none -d "$INSTALLER_TMP/shim" tools/r29/scoped_schema_host/shim/android/database/sqlite/SQLiteDatabase.java tools/r29/schema_owner_host/shim/android/content/ContentValues.java tools/r29/schema_owner_host/shim/android/database/Cursor.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
INSTALLER_SOURCES=(app/src/main/java/com/ethk3/novaonepro/ResearchSchemaCoordinatorR926.java app/src/main/java/com/ethk3/novaonepro/strategyv2/StrategySchemaSU1.java app/src/main/java/com/ethk3/novaonepro/strategyv2/StrategySchemaDomainR29.java app/src/main/java/com/ethk3/novaonepro/r952/ResearchMigrationRegistryR952.java)
sha256sum "${INSTALLER_SOURCES[@]}" > "$OUT/product-source-sha256.txt"
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$INSTALLER_TMP/product" "${INSTALLER_SOURCES[@]}"
javac -nowarn -encoding UTF-8 -proc:none -cp "$INSTALLER_TMP/shim:$INSTALLER_TMP/product:$CLASSES:$ANDROID_JAR" -d "$INSTALLER_TMP/test" tools/r29/qual_host/ResearchSchemaOwnerR29HostTest.java tools/r29/qual_host/ResearchPhaseStampR29HostTest.java tools/r29/qual_host/SchemaInstallerBudgetR29HostTest.java
java -Xmx768m -cp "$INSTALLER_TMP/shim:$INSTALLER_TMP/test:$INSTALLER_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.SchemaInstallerBudgetR29HostTest | tee "$OUT/r29_schema_installer_budget_host.log"
mkdir -p "$INSTALLER_TMP/old-source" "$INSTALLER_TMP/old-product"
for path in "${INSTALLER_SOURCES[@]}"; do git show "84c360e0:$path" > "$INSTALLER_TMP/old-source/$(basename "$path")"; done
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$INSTALLER_TMP/old-product" "$INSTALLER_TMP/old-source/"*.java
for control in pending rp-ready; do
  set +e
  java -Xmx768m -cp "$INSTALLER_TMP/shim:$INSTALLER_TMP/test:$INSTALLER_TMP/old-product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.SchemaInstallerBudgetR29HostTest "--old-$control-control" > "$OUT/old-84-$control-control.log" 2>&1
  INSTALLER_STATUS=$?
  set -e
  INSTALLER_EXPECTED=B2_EIGHT_REAL_PENDING_UNITS_REDUCE_OLD_QUERY_COUNT_BY_AT_LEAST_ONE_QUARTER
  if [ "$control" = rp-ready ]; then INSTALLER_EXPECTED=RP_FRESH_COMPLETE_READY_PROOF_AT_MOST_TEN_QUERIES; fi
  if [ "$INSTALLER_STATUS" -eq 0 ] || ! grep -Fq "$INSTALLER_EXPECTED|FAILED" "$OUT/old-84-$control-control.log"; then cat "$OUT/old-84-$control-control.log"; exit 1; fi
  echo "R29_INSTALLER_BUDGET|OLD_84_${control}_QUERY_AMPLIFICATION_REJECTED|exit=$INSTALLER_STATUS"
done
