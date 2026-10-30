#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/cutover-resume-host}
mkdir -p "$OUT"
RESUME_TMP=$(mktemp -d)
trap 'rm -rf "$RESUME_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
javac -nowarn -encoding UTF-8 -proc:none -d "$RESUME_TMP/shim" $(find tools/r29/scoped_schema_host/shim -name '*.java') tools/r29/schema_owner_host/shim/android/content/ContentValues.java tools/r29/schema_owner_host/shim/android/database/Cursor.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$RESUME_TMP/product" app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchCutoverR29.java app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRouteR29.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$RESUME_TMP/shim:$RESUME_TMP/product:$CLASSES:$ANDROID_JAR" -d "$RESUME_TMP/test" tools/r29/qual_host/CutoverResumeR29HostTest.java
java -cp "$RESUME_TMP/shim:$RESUME_TMP/test:$RESUME_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.CutoverResumeR29HostTest | tee "$OUT/r29_cutover_resume_host.log"
# Extract the exact historical retry cleanup segment and execute it with the immutable old production helper.
mkdir -p "$RESUME_TMP/old-source" "$RESUME_TMP/old-product"
git show '0c639ccb37025f6d573f0293abccff061d41826e:app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchCutoverR29.java' > "$RESUME_TMP/old-source/ResearchCutoverR29.java"
python3 - "$RESUME_TMP/old-source/ResearchCutoverR29.java" "$RESUME_TMP/old-source/HistoricalCutoverRetryR29.java" <<'PY'
from pathlib import Path
import re,sys
old=Path(sys.argv[1]).read_text()
cleanup=re.search(r'(removeUnpublished\(candidate\);removeUnpublished\(published\);)',old)
assert cleanup, 'HISTORICAL_CLEANUP_SEGMENT_MISSING'
body=cleanup.group(1).replace('removeUnpublished(', 'ResearchCutoverR29.removeUnpublished(')
Path(sys.argv[2]).write_text('package com.ethk3.novaonepro.strategyv2; import java.io.File; public final class HistoricalCutoverRetryR29 { public static void cleanup(File candidate,File published)throws Exception{'+body+'}}\n')
PY
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$RESUME_TMP/old-product" "$RESUME_TMP/old-source/ResearchCutoverR29.java" "$RESUME_TMP/old-source/HistoricalCutoverRetryR29.java"
set +e
java -cp "$RESUME_TMP/shim:$RESUME_TMP/test:$RESUME_TMP/old-product:$RESUME_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.CutoverResumeR29HostTest --historical-delete-control > "$OUT/old-cp10-published-delete-control.log" 2>&1
RESUME_OLD_STATUS=$?
set -e
if [ "$RESUME_OLD_STATUS" -eq 0 ] || ! grep -Fq OLD_RETRY_DELETED_VERIFIED_PUBLISHED "$OUT/old-cp10-published-delete-control.log"; then cat "$OUT/old-cp10-published-delete-control.log"; exit 1; fi
echo "R29_CUTOVER_RESUME|OLD_CP10_RETRY_DELETION_REJECTED|exit=$RESUME_OLD_STATUS"
