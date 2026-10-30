#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/cold-start-fairness-host}
mkdir -p "$OUT"
COLD_TMP=$(mktemp -d)
trap 'rm -rf "$COLD_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
mkdir -p "$COLD_TMP/shim-source"
python3 - "$COLD_TMP/shim-source/SQLiteDatabase.java" <<'PY'
import pathlib,sys
s=pathlib.Path('tools/r29/scoped_schema_host/shim/android/database/sqlite/SQLiteDatabase.java').read_text()
method='''public long setMaximumSize(long bytes){long size;try(Cursor c=rawQuery("PRAGMA page_size",null)){c.moveToFirst();size=c.getLong(0);}try(Cursor c=rawQuery("PRAGMA max_page_count="+((bytes+size-1)/size),null)){c.moveToFirst();return c.getLong(0)*size;}}'''
s=s.replace('    public boolean isOpen()', '    '+method+'\n    public boolean isOpen()',1)
pathlib.Path(sys.argv[1]).write_text(s)
PY
javac -nowarn -encoding UTF-8 -proc:none -d "$COLD_TMP/shim" $(find tools/r29/scoped_schema_host/shim -name '*.java' ! -path '*/android/database/sqlite/SQLiteDatabase.java') "$COLD_TMP/shim-source/SQLiteDatabase.java" tools/r29/schema_owner_host/shim/android/content/ContentValues.java tools/r29/schema_owner_host/shim/android/database/Cursor.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
COLD_SOURCES=(app/src/main/java/com/ethk3/novaonepro/ResearchSchemaCoordinatorR926.java app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java)
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$COLD_TMP/product" "${COLD_SOURCES[@]}"
javac -nowarn -encoding UTF-8 -proc:none -cp "$COLD_TMP/shim:$COLD_TMP/product:$CLASSES:$ANDROID_JAR" -d "$COLD_TMP/test" tools/r29/qual_host/EventYieldR29HostTest.java tools/r29/qual_host/ResearchSchemaOwnerR29HostTest.java tools/r29/qual_host/ColdStartFairnessR29HostTest.java
java -Xmx768m -cp "$COLD_TMP/shim:$COLD_TMP/test:$COLD_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.ColdStartFairnessR29HostTest | tee "$OUT/r29_cold_start_fairness_host.log"
mkdir -p "$COLD_TMP/old-source" "$COLD_TMP/old-product"
for path in "${COLD_SOURCES[@]}"; do git show "2cf09d56fbffb67d7b942aceba9293a01d00143c:$path" > "$COLD_TMP/old-source/$(basename "$path")"; done
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$COLD_TMP/old-product" "$COLD_TMP/old-source/"*.java
for control in overlay migration; do
  set +e
  java -Xmx768m -cp "$COLD_TMP/shim:$COLD_TMP/test:$COLD_TMP/old-product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.ColdStartFairnessR29HostTest "--old-$control-control" > "$OUT/old-2cf-$control-control.log" 2>&1
  COLD_STATUS=$?
  set -e
  COLD_EXPECTED=C1_OPTIONAL_OVERLAY_WAITS_OUTSIDE_ROOT_WRITER
  if [ "$control" = migration ]; then COLD_EXPECTED=C2_MIGRATION_DOES_NOT_CREATE_STAGE_OR_ROOT_TRACKERS_BEFORE_DEPENDENCY; fi
  if [ "$COLD_STATUS" -eq 0 ] || ! grep -q "$COLD_EXPECTED" "$OUT/old-2cf-$control-control.log"; then cat "$OUT/old-2cf-$control-control.log"; exit 1; fi
  echo "R29_COLD_START_FAIRNESS|OLD_2CF_${control}_REJECTED|exit=$COLD_STATUS"
done
