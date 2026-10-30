#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/research-phase-stamp-host}
mkdir -p "$OUT"
PHASE_TMP=$(mktemp -d)
trap 'rm -rf "$PHASE_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
javac -nowarn -encoding UTF-8 -proc:none -d "$PHASE_TMP/shim" tools/r29/scoped_schema_host/shim/android/database/sqlite/SQLiteDatabase.java tools/r29/schema_owner_host/shim/android/content/ContentValues.java tools/r29/schema_owner_host/shim/android/database/Cursor.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$PHASE_TMP/product" app/src/main/java/com/ethk3/novaonepro/ResearchSchemaCoordinatorR926.java app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$PHASE_TMP/shim:$PHASE_TMP/product:$CLASSES:$ANDROID_JAR" -d "$PHASE_TMP/test" tools/r29/qual_host/ResearchSchemaOwnerR29HostTest.java tools/r29/qual_host/ResearchPhaseStampR29HostTest.java
java -Xmx768m -cp "$PHASE_TMP/shim:$PHASE_TMP/test:$PHASE_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.ResearchPhaseStampR29HostTest | tee "$OUT/r29_research_phase_stamp_host.log"
mkdir -p "$PHASE_TMP/old-source" "$PHASE_TMP/old-product"
git show '2cf09d56fbffb67d7b942aceba9293a01d00143c:app/src/main/java/com/ethk3/novaonepro/ResearchSchemaCoordinatorR926.java' > "$PHASE_TMP/old-source/ResearchSchemaCoordinatorR926.java"
javac -nowarn -encoding UTF-8 -proc:none -cp "$PHASE_TMP/product:$CLASSES:$ANDROID_JAR" -d "$PHASE_TMP/old-product" "$PHASE_TMP/old-source/ResearchSchemaCoordinatorR926.java"
set +e
java -Xmx768m -cp "$PHASE_TMP/shim:$PHASE_TMP/test:$PHASE_TMP/old-product:$PHASE_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.ResearchPhaseStampR29HostTest > "$OUT/old-2cf09d56-phase-stamp-control.log" 2>&1
PHASE_OLD_STATUS=$?
set -e
if [ "$PHASE_OLD_STATUS" -eq 0 ] || ! grep -Fq 'P2_SAME_PENDING_STAGE_HAS_NO_REPEATED_FULL_FINGERPRINT|FAILED' "$OUT/old-2cf09d56-phase-stamp-control.log" || ! grep -Fq 'sameStageUnits=8|fullCatalogueFingerprints=8' "$OUT/old-2cf09d56-phase-stamp-control.log"; then cat "$OUT/old-2cf09d56-phase-stamp-control.log"; exit 1; fi
echo "R29_PHASE_STAMP|OLD_2CF_REPEATED_FULL_FINGERPRINT_REJECTED|exit=$PHASE_OLD_STATUS"
