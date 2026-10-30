#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/cutover-snapshot-host}
mkdir -p "$OUT"
SNAPSHOT_TMP=$(mktemp -d)
trap 'rm -rf "$SNAPSHOT_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
# Private HOST shim: Android OPEN_READONLY starts with query_only=0. Its normal non-exclusive session
# transaction issues BEGIN IMMEDIATE; SQLite's actual file-readonly WAL path still permits concurrent writes.
# Keep the existing shared shim untouched so this new regression cannot alter other suite semantics.
mkdir -p "$SNAPSHOT_TMP/shim-source/android/database/sqlite"
python3 - "$SNAPSHOT_TMP/shim-source/android/database/sqlite/SQLiteDatabase.java" <<'PY'
from pathlib import Path
import sys
source=Path('tools/r29/scoped_schema_host/shim/android/database/sqlite/SQLiteDatabase.java').read_text()
old='if(readOnly)try(Cursor c=db.rawQuery("PRAGMA query_only=1",null)){c.moveToFirst();}'
assert source.count(old)==1
source=source.replace(old,'')
needle='properties.setProperty("open_mode",readOnly?"1":create?"6":"2");'
assert source.count(needle)==1
source=source.replace(needle,needle+'properties.setProperty("transaction_mode","IMMEDIATE");')
Path(sys.argv[1]).write_text(source)
PY
javac -nowarn -encoding UTF-8 -proc:none -d "$SNAPSHOT_TMP/shim" "$SNAPSHOT_TMP/shim-source/android/database/sqlite/SQLiteDatabase.java" tools/r29/schema_owner_host/shim/android/content/ContentValues.java tools/r29/schema_owner_host/shim/android/database/Cursor.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$SNAPSHOT_TMP/product" app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchCutoverR29.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$SNAPSHOT_TMP/shim:$SNAPSHOT_TMP/product:$CLASSES:$ANDROID_JAR" -d "$SNAPSHOT_TMP/test" tools/r29/qual_host/CutoverSnapshotR29HostTest.java
java -cp "$SNAPSHOT_TMP/shim:$SNAPSHOT_TMP/test:$SNAPSHOT_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.CutoverSnapshotR29HostTest | tee "$OUT/r29_cutover_snapshot_host.log"
mkdir -p "$SNAPSHOT_TMP/old-source" "$SNAPSHOT_TMP/old-product"
git show 'e3dd104302dc988e1acb0c03996c05fb229ae3e6:app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchCutoverR29.java' > "$SNAPSHOT_TMP/old-source/ResearchCutoverR29.java"
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$SNAPSHOT_TMP/old-product" "$SNAPSHOT_TMP/old-source/ResearchCutoverR29.java"
for mode in rowid-replace stable-update; do
  if [ "$mode" = rowid-replace ]; then arg=--historical-unpinned-control; expected=CUTOVER_MUTABLE_APPSTATE_KEY_REAPPEARED_WITHOUT_SNAPSHOT; else arg=--historical-unpinned-update-control; expected=CUTOVER_UNPINNED_REALTIME_UPDATE_MIXED_TYPED_ROOT; fi
  set +e
  java -cp "$SNAPSHOT_TMP/shim:$SNAPSHOT_TMP/test:$SNAPSHOT_TMP/old-product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.CutoverSnapshotR29HostTest "$arg" > "$OUT/old-e3-unpinned-$mode-control.log" 2>&1
  SNAPSHOT_CONTROL_STATUS=$?
  set -e
  if [ "$SNAPSHOT_CONTROL_STATUS" -eq 0 ] || ! grep -Fq "$expected" "$OUT/old-e3-unpinned-$mode-control.log"; then cat "$OUT/old-e3-unpinned-$mode-control.log"; exit 1; fi
  echo "R29_CUTOVER_SNAPSHOT|OLD_UNPINNED_${mode}_REJECTED|exit=$SNAPSHOT_CONTROL_STATUS"
done
