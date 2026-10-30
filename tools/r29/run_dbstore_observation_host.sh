#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/dbstore-observation-host}
mkdir -p "$OUT"
DBSTORE_TMP=$(mktemp -d)
trap 'rm -rf "$DBSTORE_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
mkdir -p "$DBSTORE_TMP/framework/android/database/sqlite"
python3 - "$DBSTORE_TMP/framework/android/database/sqlite/SQLiteDatabase.java" <<'PY'
import pathlib,sys
text=pathlib.Path('tools/r29/scoped_schema_host/shim/android/database/sqlite/SQLiteDatabase.java').read_text()
query='''    // HOST framework query builder; executes the actual DbStore-supplied selection and order.
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
mapfile -t DBSTORE_SHIMS < <(find tools/r29/scoped_schema_host/shim -name '*.java' ! -path '*/android/database/sqlite/SQLiteDatabase.java')
javac -nowarn -encoding UTF-8 -proc:none -d "$DBSTORE_TMP/shim" "${DBSTORE_SHIMS[@]}" "$DBSTORE_TMP/framework/android/database/sqlite/SQLiteDatabase.java" tools/r29/schema_owner_host/shim/android/content/ContentValues.java tools/r29/schema_owner_host/shim/android/database/Cursor.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$DBSTORE_TMP/product" app/src/main/java/com/ethk3/novaonepro/DbStore.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$DBSTORE_TMP/shim:$DBSTORE_TMP/product:$CLASSES:$ANDROID_JAR" -d "$DBSTORE_TMP/test" tools/r29/qual_host/EventYieldR29HostTest.java tools/r29/qual_host/DbStoreObservationR29HostTest.java
java -Xmx768m -cp "$DBSTORE_TMP/shim:$DBSTORE_TMP/test:$DBSTORE_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.DbStoreObservationR29HostTest | tee "$OUT/dbstore-observation-host.log"
mkdir -p "$DBSTORE_TMP/old-source" "$DBSTORE_TMP/old-product"
git show '0c639ccb:app/src/main/java/com/ethk3/novaonepro/DbStore.java' > "$DBSTORE_TMP/old-source/DbStore.java"
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$DBSTORE_TMP/old-product" "$DBSTORE_TMP/old-source/DbStore.java"
set +e
java -Xmx768m -cp "$DBSTORE_TMP/shim:$DBSTORE_TMP/test:$DBSTORE_TMP/old-product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.DbStoreObservationR29HostTest > "$OUT/old-0c639-shared-pool-control.log" 2>&1
DBSTORE_OLD_STATUS=$?
set -e
if [ "$DBSTORE_OLD_STATUS" -eq 0 ] || ! grep -Fq DBSTORE_GETTERS_BORROWED_BLOCKED_ROOT_RW_POOL "$OUT/old-0c639-shared-pool-control.log"; then
  cat "$OUT/old-0c639-shared-pool-control.log"
  exit 1
fi
echo "R29_DBSTORE_OBSERVATION|OLD_SHARED_RW_GETTERS_REJECTED|exit=$DBSTORE_OLD_STATUS"
