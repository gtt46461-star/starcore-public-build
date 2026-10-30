#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/scoped-schema-pending-host}
mkdir -p "$OUT"
SCOPED_TMP=$(mktemp -d)
trap 'rm -rf "$SCOPED_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
javac -nowarn -encoding UTF-8 -proc:none -d "$SCOPED_TMP/shim" $(find tools/r29/scoped_schema_host/shim -name '*.java') tools/r29/schema_owner_host/shim/android/content/ContentValues.java tools/r29/schema_owner_host/shim/android/database/Cursor.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$SCOPED_TMP/shim:$CLASSES:$ANDROID_JAR" -d "$SCOPED_TMP/test" tools/r29/qual_host/ResearchSchemaOwnerR29HostTest.java tools/r29/qual_host/ScopedSchemaPendingR29HostTest.java
java -Xmx768m -cp "$SCOPED_TMP/shim:$SCOPED_TMP/test:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.ScopedSchemaPendingR29HostTest | tee "$OUT/r29_scoped_schema_pending_host.log"
# Actual failed API34 source, invoked through the identical real DCo/F01/scope fixture.
mkdir -p "$SCOPED_TMP/old-source" "$SCOPED_TMP/old-classes"
git show 'e5f9292:app/src/main/java/com/ethk3/novaonepro/ResearchSchemaCoordinatorR926.java' > "$SCOPED_TMP/old-source/ResearchSchemaCoordinatorR926.java"
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$SCOPED_TMP/old-classes" "$SCOPED_TMP/old-source/ResearchSchemaCoordinatorR926.java"
set +e
java -Xmx768m -cp "$SCOPED_TMP/shim:$SCOPED_TMP/test:$SCOPED_TMP/old-classes:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.ScopedSchemaPendingR29HostTest > "$OUT/old-e5f9292-control.log" 2>&1
SCOPED_STATUS=$?
set -e
if [ "$SCOPED_STATUS" -eq 0 ] || ! rg -q PENDING_DID_NOT_SURVIVE_REAL_F01 "$OUT/old-e5f9292-control.log"; then
  cat "$OUT/old-e5f9292-control.log"
  exit 1
fi
echo "R29_SCOPED_SCHEMA_PENDING|OLD_E5_IMPLEMENTATION_REJECTED|exit=$SCOPED_STATUS"
