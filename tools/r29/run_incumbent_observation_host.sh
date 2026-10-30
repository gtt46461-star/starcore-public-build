#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/incumbent-observation-host}
mkdir -p "$OUT"
INCUMBENT_OBSERVATION_TMP=$(mktemp -d)
trap 'rm -rf "$INCUMBENT_OBSERVATION_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
mkdir -p "$INCUMBENT_OBSERVATION_TMP/framework/android/database/sqlite"
python3 - "$INCUMBENT_OBSERVATION_TMP/framework/android/database/sqlite/SQLiteDatabase.java" <<'PY'
import pathlib,sys
text=pathlib.Path('tools/r29/scoped_schema_host/shim/android/database/sqlite/SQLiteDatabase.java').read_text()
query='''    // HOST query builder executes the actual DbStore-supplied selection and ordering.
    public Cursor query(String table,String[] columns,String selection,String[] args,String groupBy,String having,String orderBy,String limit){
        String sql="SELECT "+(columns==null?"*":String.join(",",columns))+" FROM "+table;
        if(selection!=null&&!selection.isEmpty())sql+=" WHERE "+selection;
        if(groupBy!=null&&!groupBy.isEmpty())sql+=" GROUP BY "+groupBy;
        if(having!=null&&!having.isEmpty())sql+=" HAVING "+having;
        if(orderBy!=null&&!orderBy.isEmpty())sql+=" ORDER BY "+orderBy;
        if(limit!=null&&!limit.isEmpty())sql+=" LIMIT "+limit;
        return rawQuery(sql,args);
    }
    public Cursor query(String table,String[] columns,String selection,String[] args,String groupBy,String having,String orderBy){return query(table,columns,selection,args,groupBy,having,orderBy,null);}
'''
assert text.count('    public Cursor rawQuery(String sql, String[] args) {')==1
pathlib.Path(sys.argv[1]).write_text(text.replace('    public Cursor rawQuery(String sql, String[] args) {',query+'    public Cursor rawQuery(String sql, String[] args) {'))
PY
mapfile -t INCUMBENT_OBSERVATION_SHIMS < <(find tools/r29/scoped_schema_host/shim -name '*.java' ! -path '*/android/database/sqlite/SQLiteDatabase.java')
javac -nowarn -encoding UTF-8 -proc:none -d "$INCUMBENT_OBSERVATION_TMP/shim" "${INCUMBENT_OBSERVATION_SHIMS[@]}" "$INCUMBENT_OBSERVATION_TMP/framework/android/database/sqlite/SQLiteDatabase.java" tools/r29/schema_owner_host/shim/android/content/ContentValues.java tools/r29/schema_owner_host/shim/android/database/Cursor.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$INCUMBENT_OBSERVATION_TMP/product" app/src/main/java/com/ethk3/novaonepro/strategyv2/IncumbentLoopSU2.java app/src/main/java/com/ethk3/novaonepro/strategyv2/EventFrameBuilderSU1.java app/src/main/java/com/ethk3/novaonepro/strategyv2/LegacyResearchScopeR29.java app/src/main/java/com/ethk3/novaonepro/strategyv2/ExpertWorkshopR26.java app/src/main/java/com/ethk3/novaonepro/r952/PITSnapshotContractR952.java app/src/main/java/com/ethk3/novaonepro/strategyv2/EventFrameSU1.java app/src/main/java/com/ethk3/novaonepro/strategyv2/CooperativeContinuationR26.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$INCUMBENT_OBSERVATION_TMP/shim:$INCUMBENT_OBSERVATION_TMP/product:$CLASSES:$ANDROID_JAR" -d "$INCUMBENT_OBSERVATION_TMP/test" tools/r29/qual_host/EventYieldR29HostTest.java tools/r29/qual_host/IncumbentObservationR29HostTest.java tools/r29/qual_host/PredrawCaptureQuantumR29HostTest.java
java -Xmx768m -cp "$INCUMBENT_OBSERVATION_TMP/shim:$INCUMBENT_OBSERVATION_TMP/test:$INCUMBENT_OBSERVATION_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.IncumbentObservationR29HostTest | tee "$OUT/r29_incumbent_observation_host.log"
mkdir -p "$INCUMBENT_OBSERVATION_TMP/old-source" "$INCUMBENT_OBSERVATION_TMP/old-product"
for name in IncumbentLoopSU2 EventFrameBuilderSU1; do git show "116888fe:app/src/main/java/com/ethk3/novaonepro/strategyv2/$name.java" > "$INCUMBENT_OBSERVATION_TMP/old-source/$name.java"; done
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$INCUMBENT_OBSERVATION_TMP/old-product" "$INCUMBENT_OBSERVATION_TMP/old-source/IncumbentLoopSU2.java" "$INCUMBENT_OBSERVATION_TMP/old-source/EventFrameBuilderSU1.java"
set +e
java -Xmx768m -cp "$INCUMBENT_OBSERVATION_TMP/shim:$INCUMBENT_OBSERVATION_TMP/test:$INCUMBENT_OBSERVATION_TMP/old-product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.IncumbentObservationR29HostTest > "$OUT/old-116888fe-incumbent-rw-control.log" 2>&1
INCUMBENT_OBSERVATION_CONTROL=$?
set -e
if [ "$INCUMBENT_OBSERVATION_CONTROL" -eq 0 ] || ! grep -q INCUMBENT_BORROWED_BLOCKED_ROOT_RW_POOL "$OUT/old-116888fe-incumbent-rw-control.log"; then
  cat "$OUT/old-116888fe-incumbent-rw-control.log"
  exit 1
fi
echo "R29_INCUMBENT_OBSERVATION|OLD_SHARED_ROOT_RW_REJECTED|exit=$INCUMBENT_OBSERVATION_CONTROL"
mkdir -p "$INCUMBENT_OBSERVATION_TMP/old-audit-source" "$INCUMBENT_OBSERVATION_TMP/old-audit-product"
git show "116888fe:app/src/main/java/com/ethk3/novaonepro/strategyv2/ExpertWorkshopR26.java" > "$INCUMBENT_OBSERVATION_TMP/old-audit-source/ExpertWorkshopR26.java"
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$INCUMBENT_OBSERVATION_TMP/old-audit-product" "$INCUMBENT_OBSERVATION_TMP/old-audit-source/ExpertWorkshopR26.java"
set +e
java -Xmx768m -cp "$INCUMBENT_OBSERVATION_TMP/shim:$INCUMBENT_OBSERVATION_TMP/test:$INCUMBENT_OBSERVATION_TMP/old-audit-product:$INCUMBENT_OBSERVATION_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.IncumbentObservationR29HostTest > "$OUT/old-116888fe-expert-audit-rw-control.log" 2>&1
EXPERT_AUDIT_OBSERVATION_CONTROL=$?
set -e
if [ "$EXPERT_AUDIT_OBSERVATION_CONTROL" -eq 0 ] || ! grep -q AUDIT_BORROWED_BLOCKED_ROOT_RW_POOL "$OUT/old-116888fe-expert-audit-rw-control.log"; then
  cat "$OUT/old-116888fe-expert-audit-rw-control.log"
  exit 1
fi
echo "R29_INCUMBENT_OBSERVATION|OLD_SHARED_EXPERT_AUDIT_RW_REJECTED|exit=$EXPERT_AUDIT_OBSERVATION_CONTROL"
java -Xmx768m -cp "$INCUMBENT_OBSERVATION_TMP/shim:$INCUMBENT_OBSERVATION_TMP/test:$INCUMBENT_OBSERVATION_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.PredrawCaptureQuantumR29HostTest | tee "$OUT/r29_predraw_capture_quantum_host.log"
mkdir -p "$INCUMBENT_OBSERVATION_TMP/old-capture-source" "$INCUMBENT_OBSERVATION_TMP/old-capture-product"
for path in r952/PITSnapshotContractR952 strategyv2/EventFrameBuilderSU1 strategyv2/EventFrameSU1; do
  git show "84c360e0:app/src/main/java/com/ethk3/novaonepro/$path.java" > "$INCUMBENT_OBSERVATION_TMP/old-capture-source/$(basename "$path").java"
done
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$INCUMBENT_OBSERVATION_TMP/old-capture-product" "$INCUMBENT_OBSERVATION_TMP/old-capture-source/"*.java
set +e
java -Xmx768m -cp "$INCUMBENT_OBSERVATION_TMP/shim:$INCUMBENT_OBSERVATION_TMP/test:$INCUMBENT_OBSERVATION_TMP/old-capture-product:$INCUMBENT_OBSERVATION_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.PredrawCaptureQuantumR29HostTest old-control > "$OUT/old-84c360e0-full-predraw-capture-control.log" 2>&1
PREDRAW_CAPTURE_CONTROL=$?
set -e
if [ "$PREDRAW_CAPTURE_CONTROL" -eq 0 ] || ! grep -q PREDRAW_CAPTURE_MISSING_POLL_BETWEEN_BOUNDED_READS "$OUT/old-84c360e0-full-predraw-capture-control.log"; then
  cat "$OUT/old-84c360e0-full-predraw-capture-control.log"
  exit 1
fi
echo "R29_PREDRAW_CAPTURE|OLD_UNSLICED_CAPTURE_REJECTED|exit=$PREDRAW_CAPTURE_CONTROL"
set +e
java -Xmx768m -cp "$INCUMBENT_OBSERVATION_TMP/shim:$INCUMBENT_OBSERVATION_TMP/test:$INCUMBENT_OBSERVATION_TMP/old-capture-product:$INCUMBENT_OBSERVATION_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.PredrawCaptureQuantumR29HostTest old-task-control > "$OUT/old-84c360e0-full-incumbent-task-control.log" 2>&1
PREDRAW_TASK_CONTROL=$?
set -e
if [ "$PREDRAW_TASK_CONTROL" -eq 0 ] || ! grep -q PREDRAW_TASK_MISSING_CAPTURE_POLL "$OUT/old-84c360e0-full-incumbent-task-control.log"; then
  cat "$OUT/old-84c360e0-full-incumbent-task-control.log"
  exit 1
fi
echo "R29_PREDRAW_CAPTURE|OLD_UNSLICED_ACTUAL_INCUMBENT_TASK_REJECTED|exit=$PREDRAW_TASK_CONTROL"
