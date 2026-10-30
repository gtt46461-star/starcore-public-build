#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/schema-owner-host}
mkdir -p "$OUT"
OWNER_TMP=$(mktemp -d)
trap 'rm -rf "$OWNER_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
javac -nowarn -encoding UTF-8 -proc:none -d "$OWNER_TMP/shim" $(find tools/r29/schema_owner_host/shim -name '*.java') tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$OWNER_TMP/shim:$CLASSES:$ANDROID_JAR" -d "$OWNER_TMP/test" tools/r29/qual_host/ResearchSchemaOwnerR29HostTest.java
java -Xmx768m -cp "$OWNER_TMP/shim:$OWNER_TMP/test:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.ResearchSchemaOwnerR29HostTest | tee "$OUT/r29_research_schema_owner_host.log"
# Exact historical production DbHelper from the failing API34 run. Identical real SQLite fixture/installer.
mkdir -p "$OWNER_TMP/old-source" "$OWNER_TMP/old-classes"
git show '8778561bca9679d543a35299ead21aab50a9ef74:app/src/main/java/com/ethk3/novaonepro/DbHelper.java' > "$OWNER_TMP/old-source/DbHelper.java"
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$OWNER_TMP/old-classes" "$OWNER_TMP/old-source/DbHelper.java"
set +e
java -Xmx768m -cp "$OWNER_TMP/shim:$OWNER_TMP/test:$OWNER_TMP/old-classes:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.ResearchSchemaOwnerR29HostTest > "$OUT/old-8778561-control.log" 2>&1
OWNER_CONTROL_STATUS=$?
set -e
if [ "$OWNER_CONTROL_STATUS" -eq 0 ] || ! grep -Eq 'ACTUAL_RESEARCH_SCHEMA_FAILED:FAILED\|cursor=R906_RESEARCH_R64_VERIFIED.*no such table.*v57_(selected_evidence_r906|oos_label_r35)' "$OUT/old-8778561-control.log"; then
  cat "$OUT/old-8778561-control.log"
  exit 1
fi
echo "R29_SCHEMA_OWNER|OLD_8778561_IMPLEMENTATION_REJECTED|exit=$OWNER_CONTROL_STATUS"
