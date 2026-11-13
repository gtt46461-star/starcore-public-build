"""Actual P02 production classes + native SQLite/HTTP loopback. HOST adapters, never device acceptance."""
import ast
import hashlib
import json
import os
import pathlib
import re
import subprocess
import tempfile
from host_tool_support import dependencies, run as host_run, cursor_column_metadata, database_column_metadata

ROOT = pathlib.Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/ethk3/novaonepro"
CLASSES, SDK, RJAR, JDBC = dependencies(ROOT, require_resources=True)
source_names = ["AutoFetchService", "DatabaseCoordinator", "SchemaDomainCoordinatorR926", "ApiClient", "FetchLedgerR925", "SettlementTaskStoreR36", "AnalysisSingleFlightR36", "AnalysisRequestStoreR36", "R36AnalysisLeasePolicyR365", "ServiceHeartbeatStore", "BootReceiver", "OosAfterDrawWriterR36"]
strategy_names = ["RuntimeSchemaR26", "RuntimeCoordinatorR26", "ResearchPoolR27", "SchemaR27", "FailureDispositionR31", "CooperativeContinuationR26", "MemoryBudgetR31", "BoundedResearchExecutorR31", "SharedFeatureStoreR31", "FeatureCacheR27", "PreparedTaskR30R1", "TaskContinuationR26", "ExecutionBudgetR26"]
production_sources = [MAIN / (name + ".java") for name in source_names] + [MAIN / "strategyv2" / (name + ".java") for name in strategy_names]
before = {str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest() for path in production_sources}
for path, digest in before.items():
    print("R31_P02_HOST_SOURCE_SHA256", path, digest, flush=True)


def run(args):
    host_run(ROOT, args)


# Reuse only the labelled Android/JDBC adapters. No business algorithm is extracted or replaced.
adapter_tree = ast.parse((ROOT / "tools/r31/run_write_protocol_host.py").read_text())
adapters = next(ast.literal_eval(node.value) for node in ast.walk(adapter_tree)
                if isinstance(node, ast.Assign) and any(isinstance(t, ast.Name) and t.id == "adapters" for t in node.targets))
transaction_adapter = next(node.value for node in ast.walk(adapter_tree) if isinstance(node, ast.Constant)
                           and isinstance(node.value, str) and "public void beginTransaction()" in node.value and "OPEN_READONLY" in node.value)
with tempfile.TemporaryDirectory(prefix="r31-service-native-") as temporary:
    tmp = pathlib.Path(temporary)
    platform, src, actual, tests = [tmp / name for name in ["platform", "sources", "actual", "tests"]]
    for directory in [platform, src, actual, tests]:
        directory.mkdir()
    files = []
    for source in sorted((ROOT / "tools/r29/qual_host/shim").rglob("*.java")):
        target = src / source.relative_to(ROOT / "tools/r29/qual_host/shim")
        target.parent.mkdir(parents=True, exist_ok=True)
        text = source.read_text()
        if source.name == "Cursor.java":
            text = cursor_column_metadata(text)
        if source.name == "SQLiteDatabase.java":
            text = text.replace("public class SQLiteDatabase {", "public class SQLiteDatabase implements java.lang.AutoCloseable {")
            text = database_column_metadata(text)
            at = text.index("    static final class Rows")
            dml = '''
    public static final int CONFLICT_IGNORE=4,CONFLICT_REPLACE=5;
    public long insertOrThrow(String table,String empty,android.content.ContentValues values){return insertWithOnConflict(table,empty,values,0);}
    public long insertWithOnConflict(String table,String empty,android.content.ContentValues values,int conflict){java.util.StringJoiner names=new java.util.StringJoiner(","),slots=new java.util.StringJoiner(",");java.util.List<Object> args=new java.util.ArrayList<>();for(java.util.Map.Entry<String,Object> e:values.data.entrySet()){names.add(e.getKey());slots.add("?");args.add(e.getValue());}execSQL("INSERT "+(conflict==4?"OR IGNORE ":conflict==5?"OR REPLACE ":"")+"INTO "+table+"("+names+") VALUES("+slots+")",args.toArray());try(Cursor c=rawQuery("SELECT changes(),last_insert_rowid()",null)){return c.moveToFirst()&&c.getLong(0)>0?c.getLong(1):-1;}}
    public int update(String table,android.content.ContentValues values,String where,String[] whereArgs){java.util.StringJoiner setters=new java.util.StringJoiner(",");java.util.List<Object> args=new java.util.ArrayList<>();for(java.util.Map.Entry<String,Object> e:values.data.entrySet()){setters.add(e.getKey()+"=?");args.add(e.getValue());}if(whereArgs!=null)java.util.Collections.addAll(args,(Object[])whereArgs);execSQL("UPDATE "+table+" SET "+setters+" WHERE "+where,args.toArray());try(Cursor c=rawQuery("SELECT changes()",null)){return c.moveToFirst()?c.getInt(0):0;}}
'''
            text = text[:at] + transaction_adapter + dml + text[at:]
        target.write_text(text)
        files.append(target)
    constants = "\n".join("public static final String " + name + "=" + json.dumps(value) + ";"
                          for name, value in re.findall(r'public\s+static\s+final\s+String\s+(\w+)\s*=\s*"([^"\\]*)"\s*;', (MAIN / "DbHelper.java").read_text()))
    adapters["com/ethk3/novaonepro/DbHelper.java"] = "package com.ethk3.novaonepro;import android.database.sqlite.SQLiteDatabase;public final class DbHelper{" + constants + "private final SQLiteDatabase db;public DbHelper(SQLiteDatabase db){this.db=db;}public SQLiteDatabase getReadableDatabase(){return db;}public SQLiteDatabase getWritableDatabase(){return db;}public void close(){}}"
    adapters["android/os/Process.java"] = "package android.os;public final class Process{public static int myPid(){return (int)java.lang.ProcessHandle.current().pid();}public static final int THREAD_PRIORITY_BACKGROUND=10;public static void setThreadPriority(int priority){}}"
    adapters["android/os/Build.java"] = "package android.os;public final class Build{public static final String MANUFACTURER=\"HOST\",MODEL=\"HOST\";public static final class VERSION{public static int SDK_INT=23;public static final String RELEASE=\"HOST\";}}"
    adapters["android/content/Context.java"] = "package android.content;public class Context{public java.io.File getDatabasePath(String name){throw new IllegalStateException(\"HOST_PATH_REQUIRED\");}public Context getApplicationContext(){return this;}public SharedPreferences getSharedPreferences(String name,int mode){throw new IllegalStateException(\"HOST_PREFS_REQUIRED\");}public Context createDeviceProtectedStorageContext(){return this;}}"
    adapters["android/content/ContentValues.java"] = "package android.content;public class ContentValues{public final java.util.Map<String,Object> data=new java.util.LinkedHashMap<>();" + "".join("public void put(String key," + kind + " value){data.put(key,value);}" for kind in ["String", "Integer", "Long", "Double", "Float", "Boolean", "byte[]"]) + "public void putNull(String key){data.put(key,null);}}"
    adapters["com/ethk3/novaonepro/DbStore.java"] = '''package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
public final class DbStore{private final SQLiteDatabase db;private final DatabaseCoordinator owner;public DbStore(SQLiteDatabase db,DatabaseCoordinator owner){this.db=db;this.owner=owner;}public SQLiteDatabase readable(){return db;}public DatabaseCoordinator coordinatorR36(){return owner;}public DatabaseCoordinator coordinatorForResearchR952(){return owner;}
public Models.Draw getDrawByIssue(String issue){try(Cursor c=db.rawQuery("SELECT issue,open_time,fetch_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM draws WHERE issue=?",new String[]{issue})){if(!c.moveToFirst())return null;Models.Draw draw=new Models.Draw();draw.issue=c.getString(0);draw.openTime=c.getLong(1);draw.fetchTime=c.getLong(2);draw.sourceLine="HOST";for(int i=0;i<10;i++)draw.numbers[i]=c.getInt(i+3);return draw;}}
public Models.Draw getLatestDraw(){try(Cursor c=db.rawQuery("SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){return c.moveToFirst()?getDrawByIssue(c.getString(0)):null;}}
public String getState(String key,String fallback){return fallback;}public void setState(String key,String value){} }
'''
    for name, text in adapters.items():
        target = src / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text)
        files.append(target)
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, [SDK, CLASSES])), "-d", platform, *files])
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, [SDK, CLASSES, RJAR])), "-d", actual, *production_sources])
    cp = os.pathsep.join(map(str, [platform, actual, tests, CLASSES, JDBC, SDK]))
    drivers = [ROOT / "tools/r31/host/R31ServiceProtocolHost.java", ROOT / "tools/r31/host/R31FeedbackHost.java", ROOT / "tools/r31/host/R31ServiceHeartbeatHost.java", ROOT / "tools/r31/host/R31SqliteHostBoundaryContract.java"]
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", cp, "-d", tests, *drivers])
    run(["java", "-Xmx768m", "-cp", cp, "com.ethk3.novaonepro.R31SqliteHostBoundaryContract"])
    run(["java", "-Xmx768m", "-cp", cp, "com.ethk3.novaonepro.R31ServiceProtocolHost", tmp / "service.db"])
    gradle = pathlib.Path(os.environ.get("GRADLE_USER_HOME",str(pathlib.Path.home()/".gradle")))
    junit = next(gradle.glob("caches/modules-2/files-2.1/junit/junit/4.13.2/*/*.jar"))
    hamcrest = next(gradle.glob("caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/*/*.jar"))
    unit_cp = os.pathsep.join(map(str, [cp, junit, hamcrest]))
    test_source = ROOT / "app/src/test/java/com/ethk3/novaonepro/R31AnalysisPhaseBudgetTest.java"
    run(["javac", "-encoding", "UTF-8", "-cp", unit_cp, "-d", tests, test_source])
    run(["java", "-cp", unit_cp, "org.junit.runner.JUnitCore", "com.ethk3.novaonepro.R31AnalysisPhaseBudgetTest"])
for path, expected in before.items():
    if hashlib.sha256((ROOT / path).read_bytes()).hexdigest() != expected:
        raise RuntimeError("production source changed during tests; rerun before accepting: " + path)
print("R31_P02_SERVICE_NATIVE_HTTP_HOST_PASS_NOT_ANDROID_NOT_DEVICE_NOT_QUALITY")
