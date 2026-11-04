"""Compile actual R31 writer sources; native SQLite/JDBC, explicitly not Android/device evidence."""
import os
import hashlib
import argparse
import contextlib
import json
import shutil
import pathlib
import subprocess
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/ethk3/novaonepro"
CLASSES = ROOT / "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes"
SDK = pathlib.Path(os.environ["ANDROID_HOME"]) / "platforms/android-36/android.jar"
JDBC = pathlib.Path(os.environ.get("SQLITE_JDBC", "/tmp/r31-baseline-deps/sqlite-jdbc.jar"))
PRODUCTION_SOURCES = [MAIN / "DatabaseCoordinator.java", MAIN / "TransactionSchemaCacheR941.java", MAIN / "SchemaDomainCoordinatorR926.java",
                      MAIN / "strategyv2/FailureDispositionR31.java", MAIN / "strategyv2/SchemaR27.java", MAIN / "strategyv2/ResearchWriteQueueR27.java",
                      MAIN / "strategyv2/WriterHoldFaultsR31.java"]
SOURCE_HASHES = {str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest() for path in PRODUCTION_SOURCES}
for path, digest in SOURCE_HASHES.items():
    print("R31_HOST_SOURCE_SHA256", path, digest, flush=True)
parser = argparse.ArgumentParser()
parser.add_argument("--prepare-contention", type=pathlib.Path, help="retain a fixed compiled HOST workload snapshot; does not run or pass the soak")
options = parser.parse_args()
if options.prepare_contention:
    options.prepare_contention.mkdir(parents=True, exist_ok=False)


def run(args):
    subprocess.run(list(map(str, args)), cwd=ROOT, check=True)


with (contextlib.nullcontext(str(options.prepare_contention.resolve())) if options.prepare_contention
      else tempfile.TemporaryDirectory(prefix="r31-production-writer-")) as temporary:
    tmp = pathlib.Path(temporary)
    platform, sources, production, test = (tmp / name for name in ["platform", "sources", "production", "test"])
    for directory in [platform, sources, production, test]:
        directory.mkdir()
    support_classes = CLASSES
    sdk_runtime, jdbc_runtime = SDK, JDBC
    if options.prepare_contention:
        support_classes = tmp / "support-classes"
        shutil.copytree(CLASSES, support_classes)
        sdk_runtime, jdbc_runtime = tmp / "android.jar", tmp / "sqlite-jdbc.jar"
        shutil.copy2(SDK, sdk_runtime)
        shutil.copy2(JDBC, jdbc_runtime)
    shim_files = []
    shim_root = ROOT / "tools/r29/qual_host/shim"
    for source in sorted(shim_root.rglob("*.java")):
        target = sources / source.relative_to(shim_root)
        target.parent.mkdir(parents=True, exist_ok=True)
        text = source.read_text()
        if source.name == "SQLiteDatabase.java":
            text = text.replace("public class SQLiteDatabase {", "public class SQLiteDatabase implements java.lang.AutoCloseable {")
            at = text.index("    static final class Rows")
            text = text[:at] + '''
    public static final int OPEN_READONLY=1,NO_LOCALIZED_COLLATORS=16;
    public static int independentOpens;
    public static boolean rejectReadback;
    public boolean failAfterCommitOnce,failBeforeCommitOnce;
    private boolean successful;
    public interface CursorFactory {}
    public static SQLiteDatabase openDatabase(String path,CursorFactory factory,int flags){
        independentOpens++;
        if(rejectReadback)throw new SQLiteException("HOST_READBACK_UNAVAILABLE");
        try{return new SQLiteDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite:file:"+path+"?mode=ro"));}
        catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}
    }
    public void beginTransaction(){try{jdbc.setAutoCommit(false);successful=false;}catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}}
    public void setTransactionSuccessful(){successful=true;}
    public void endTransaction(){
        boolean after=successful&&failAfterCommitOnce;failAfterCommitOnce=false;
        boolean before=successful&&failBeforeCommitOnce;failBeforeCommitOnce=false;
        try{
            if(before){jdbc.rollback();throw new SQLiteException("HOST_END_ROLLED_BACK_BEFORE_ACK");}
            if(successful)jdbc.commit();else jdbc.rollback();
        }catch(java.sql.SQLException failure){try{jdbc.rollback();}catch(java.sql.SQLException ignored){}throw new SQLiteException(failure.getMessage());}
        finally{successful=false;try{jdbc.setAutoCommit(true);}catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}}
        if(after)throw new SQLiteException("HOST_END_COMMITTED_BEFORE_ACK_FAILURE");
    }
    public void close(){try{jdbc.close();}catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}}
''' + text[at:]
        target.write_text(text)
        shim_files.append(target)
    adapters = {
        "android/content/Context.java": '''package android.content;public class Context{public java.io.File getDatabasePath(String name){throw new IllegalStateException("HOST_PATH_REQUIRED");}public Context getApplicationContext(){return this;}}''',
        "android/app/Application.java": '''package android.app;public class Application extends android.content.Context{private final java.io.File path;public Application(java.io.File path){this.path=path;}@Override public java.io.File getDatabasePath(String name){return path;}}''',
        "android/os/SystemClock.java": '''package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000;}public static long uptimeMillis(){return elapsedRealtime();}public static long currentThreadTimeMillis(){return elapsedRealtime();}}''',
        "android/os/Looper.java": '''package android.os;public final class Looper{public static Looper myLooper(){return null;}public static Looper getMainLooper(){return null;}}''',
        "android/util/Log.java": '''package android.util;public final class Log{public static int w(String t,String m){return 0;}public static int w(String t,String m,Throwable e){return 0;}public static int e(String t,String m){return 0;}public static int e(String t,String m,Throwable e){return 0;}public static int d(String t,String m){return 0;}public static int i(String t,String m){return 0;}}''',
        "com/ethk3/novaonepro/DbHelper.java": '''package com.ethk3.novaonepro;import android.database.sqlite.SQLiteDatabase;public final class DbHelper{private final SQLiteDatabase db;public DbHelper(SQLiteDatabase db){this.db=db;}public SQLiteDatabase getReadableDatabase(){return db;}public SQLiteDatabase getWritableDatabase(){return db;}public void close(){}}''',
    }
    for name, text in adapters.items():
        target = sources / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text)
        shim_files.append(target)
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", SDK, "-d", platform, *shim_files])
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, [sdk_runtime, support_classes])), "-d", production, *PRODUCTION_SOURCES])
    classpath = os.pathsep.join(map(str, [platform, production, test, support_classes, jdbc_runtime, sdk_runtime]))
    driver_sources = [ROOT / "tools/r31/host/R31WriteProtocolHost.java", ROOT / "tools/r31/host/R31QueueCancellationHost.java"]
    contention_source = ROOT / "verification/r31/p01/soak/R31WriterContentionHost.java"
    if options.prepare_contention:
        driver_sources.append(contention_source)
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", classpath, "-d", test, *driver_sources])
    if options.prepare_contention:
        actual = {path: hashlib.sha256((ROOT / path).read_bytes()).hexdigest() for path in SOURCE_HASHES}
        if actual != SOURCE_HASHES:
            raise RuntimeError("production source changed during snapshot compilation; no soak may start")
        frozen = {str(path.relative_to(tmp)): hashlib.sha256(path.read_bytes()).hexdigest()
                  for directory in [platform, production, test, support_classes] for path in sorted(directory.rglob("*.class"))}
        for source in PRODUCTION_SOURCES + [contention_source]:
            target = tmp / "source-snapshot" / source.relative_to(ROOT)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, target)
        receipt = {"format": "R31_FIXED_HOST_CONTENTION_V1", "status": "NOT_STARTED", "source_sha256": SOURCE_HASHES,
                   "compiled_class_sha256": frozen, "android_jar_sha256": hashlib.sha256(sdk_runtime.read_bytes()).hexdigest(),
                   "sqlite_jdbc_sha256": hashlib.sha256(jdbc_runtime.read_bytes()).hexdigest(),
                   "command": ["java", "-Xmx512m", "-cp", classpath, "com.ethk3.novaonepro.R31WriterContentionHost"],
                   "scope": "HOST production coordinator/native SQLite fixture, never Android/24h/72h/natural quality evidence"}
        (tmp / "snapshot.json").write_text(json.dumps(receipt, indent=2) + "\n")
        print("R31_CONTENTION_SNAPSHOT_READY", tmp / "snapshot.json")
        raise SystemExit(0)
    run(["java", "-Xmx512m", "-cp", classpath, "com.ethk3.novaonepro.R31WriteProtocolHost", tmp / "writer.db"])
    killed = subprocess.run(["java", "-Xmx512m", "-cp", classpath, "com.ethk3.novaonepro.R31WriteProtocolHost", str(tmp / "process.db"), "halt-after-commit"], cwd=ROOT)
    if killed.returncode != 73:
        raise RuntimeError(f"post-COMMIT host halt did not execute: {killed.returncode}")
    run(["java", "-Xmx512m", "-cp", classpath, "com.ethk3.novaonepro.R31WriteProtocolHost", tmp / "process.db", "reopen-after-halt"])
    junit = next(pathlib.Path(os.environ["GRADLE_USER_HOME"]).glob("caches/modules-2/files-2.1/junit/junit/4.13.2/*/*.jar"))
    hamcrest = next(pathlib.Path(os.environ["GRADLE_USER_HOME"]).glob("caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/*/*.jar"))
    test_classpath = os.pathsep.join(map(str, [classpath, junit, hamcrest]))
    unit_root = ROOT / "app/src/test/java/com/ethk3/novaonepro/strategyv2"
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", test_classpath, "-d", test,
         unit_root / "R31PreparedWriteContractTest.java", unit_root / "R30WriteWaitTest.java"])
    run(["java", "-cp", test_classpath, "org.junit.runner.JUnitCore", "com.ethk3.novaonepro.strategyv2.R31PreparedWriteContractTest", "com.ethk3.novaonepro.strategyv2.R30WriteWaitTest"])
for path, expected in SOURCE_HASHES.items():
    if hashlib.sha256((ROOT / path).read_bytes()).hexdigest() != expected:
        raise RuntimeError("source changed during host test; rerun required: " + path)
print("R31_ACTUAL_WRITER_NATIVE_SQLITE_HOST_PASS_NOT_ANDROID_NOT_DEVICE_NOT_SOAK")
