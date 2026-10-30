#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/continuation-observation-host}
mkdir -p "$OUT"
CONT_OBS_TMP=$(mktemp -d)
trap 'rm -rf "$CONT_OBS_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
mkdir -p "$CONT_OBS_TMP/framework"
python3 - "$CONT_OBS_TMP/framework/SQLiteDatabase.java" <<'PY'
import pathlib,sys
s=pathlib.Path('tools/r29/scoped_schema_host/shim/android/database/sqlite/SQLiteDatabase.java').read_text()
query='''public Cursor query(String table,String[] columns,String selection,String[] args,String groupBy,String having,String orderBy,String limit){
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
target='    public Cursor rawQuery(String sql, String[] args) {'
assert s.count(target)==1
pathlib.Path(sys.argv[1]).write_text(s.replace(target,query+target))
PY
javac -nowarn -encoding UTF-8 -proc:none -d "$CONT_OBS_TMP/shim" $(find tools/r29/scoped_schema_host/shim -name '*.java' ! -path '*/android/database/sqlite/SQLiteDatabase.java') "$CONT_OBS_TMP/framework/SQLiteDatabase.java" tools/r29/schema_owner_host/shim/android/content/ContentValues.java tools/r29/schema_owner_host/shim/android/database/Cursor.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$CONT_OBS_TMP/product" app/src/main/java/com/ethk3/novaonepro/strategyv2/LegacyResearchScopeR29.java app/src/main/java/com/ethk3/novaonepro/strategyv2/LegacyCapabilityAdapterR29.java app/src/main/java/com/ethk3/novaonepro/strategyv2/WarmCandidateR26.java app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$CONT_OBS_TMP/shim:$CONT_OBS_TMP/product:$CLASSES:$ANDROID_JAR" -d "$CONT_OBS_TMP/test" tools/r29/qual_host/EventYieldR29HostTest.java tools/r29/qual_host/DbStoreObservationR29HostTest.java tools/r29/qual_host/ContinuationObservationR29HostTest.java
java -Xmx768m -cp "$CONT_OBS_TMP/shim:$CONT_OBS_TMP/test:$CONT_OBS_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.ContinuationObservationR29HostTest | tee "$OUT/continuation-observation-host.log"
for control in warm foundry owner; do
  case "$control" in warm) CONT_CLASS=WarmCandidateR26; CONT_FAILURE=WARM_CONTINUATION_BORROWED_BLOCKED_ROOT_RW_POOL;; foundry) CONT_CLASS=FoundryCampaignR26; CONT_FAILURE=FOUNDRY_CONTINUATION_BORROWED_BLOCKED_ROOT_RW_POOL;; owner) CONT_CLASS=LegacyCapabilityAdapterR29; CONT_FAILURE=SCHEDULER_PROBE_BORROWED_BLOCKED_ROOT_RW_POOL;; esac
  mkdir -p "$CONT_OBS_TMP/old-$control-source" "$CONT_OBS_TMP/old-$control-product"
  git show "116888fe641aa0f67c4ef85659d9fa0f1303c4b7:app/src/main/java/com/ethk3/novaonepro/strategyv2/$CONT_CLASS.java" > "$CONT_OBS_TMP/old-$control-source/$CONT_CLASS.java"
  javac -nowarn -encoding UTF-8 -proc:none -cp "$CONT_OBS_TMP/product:$CLASSES:$ANDROID_JAR" -d "$CONT_OBS_TMP/old-$control-product" "$CONT_OBS_TMP/old-$control-source/$CONT_CLASS.java"
  set +e
  java -Xmx768m -cp "$CONT_OBS_TMP/shim:$CONT_OBS_TMP/test:$CONT_OBS_TMP/old-$control-product:$CONT_OBS_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.ContinuationObservationR29HostTest "--old-$control-control" > "$OUT/old-116888fe-$control-control.log" 2>&1
  CONT_OBS_STATUS=$?
  set -e
  if [ "$CONT_OBS_STATUS" -eq 0 ] || ! grep -Fq "$CONT_FAILURE" "$OUT/old-116888fe-$control-control.log"; then cat "$OUT/old-116888fe-$control-control.log"; exit 1; fi
  echo "R29_CONTINUATION_OBSERVATION|OLD_116888FE_${control}_REJECTED|exit=$CONT_OBS_STATUS"
done
