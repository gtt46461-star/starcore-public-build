"""Real telemetry owners + sole coordinator over native HOST SQLite. Never device evidence."""
import argparse
import datetime
import hashlib
import json
import os
import pathlib
import subprocess
import tempfile
import shutil

ROOT = pathlib.Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/ethk3/novaonepro"
BASE = "3246ccf8eb6f7b2f7b9d0fe77fc18a70ab958056"
parser = argparse.ArgumentParser()
parser.add_argument("--output", type=pathlib.Path, required=True)
parser.add_argument("--support-jar", type=pathlib.Path, default=pathlib.Path("/tmp/r31-p02-012-actual-android-support/classes.jar"))
options = parser.parse_args()
output = options.output.resolve()
output.mkdir(parents=True, exist_ok=False)
sdk = pathlib.Path(os.environ["ANDROID_HOME"]) / "platforms/android-36/android.jar"
jdbc = pathlib.Path(os.environ.get("SQLITE_JDBC", "/tmp/r31-baseline-deps/sqlite-jdbc.jar"))
sources = [MAIN / "DatabaseCoordinator.java", MAIN / "TransactionSchemaCacheR941.java",
           MAIN / "strategyv2/FailureDispositionR31.java", MAIN / "strategyv2/IssueTimelineR30.java",
           MAIN / "strategyv2/WriterHoldLedgerR30.java", MAIN / "strategyv2/IssueTimelineWriterR30.java",
           MAIN / "strategyv2/HoldLedgerFlushR30.java"]
hashes = {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}
receipt = {"sourceCapturedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
           "base": BASE, "sourceSha256": hashes,
           "supportJar": str(options.support_jar), "supportJarSha256": hashlib.sha256(options.support_jar.read_bytes()).hexdigest(),
           "jdbcSha256": hashlib.sha256(jdbc.read_bytes()).hexdigest(),
           "androidJarSha256": hashlib.sha256(sdk.read_bytes()).hexdigest(),
           "scope": "Actual production telemetry/coordinator sources; native JDBC SQLite HOST adapters, engineering fixtures; NOT_ANDROID_NOT_DEVICE_NOT_24H_NOT_72H_NOT_NATURAL_QUALITY",
           "status": "IN_PROGRESS", "runs": []}
native_env = dict(os.environ)
native_env.pop("JAVA_TOOL_OPTIONS", None)  # Local JNI/JDK tests need no proxy JVM properties in public logs.
receipt["javaHome"] = os.environ.get("JAVA_HOME")
receipt["javaExecutable"] = shutil.which("java")
receipt["javacExecutable"] = shutil.which("javac")
(output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")


def run(args, name):
    args = list(map(str, args))
    try:
        result = subprocess.run(args, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, env=native_env, timeout=60)
    except subprocess.TimeoutExpired as failure:
        raw = failure.stdout or b""
        (output / (name + ".log")).write_text(raw.decode(errors="replace") if isinstance(raw, bytes) else raw)
        receipt["status"] = "FAILED_TIMEOUT"
        receipt["runs"].append({"name": name, "command": args, "exitCode": None, "timeoutSeconds": 60})
        (output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
        raise
    (output / (name + ".log")).write_text(result.stdout)
    receipt["runs"].append({"name": name, "command": args, "exitCode": result.returncode})
    (output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(name, "exit=" + str(result.returncode), flush=True)
    if result.returncode:
        receipt["status"] = "FAILED"
        receipt["failedCommand"] = name
        (output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
        print(result.stdout[-14000:], flush=True)
        raise RuntimeError(name + " failed; original log retained")


with tempfile.TemporaryDirectory(prefix="r31-telemetry-budgets-") as temporary:
    tmp = pathlib.Path(temporary)
    platform, prepared, tests, old = (tmp / name for name in ["platform", "production", "tests", "baseline"])
    generated = tmp / "shim-source"
    for d in [platform, prepared, tests, old, generated]:
        d.mkdir()
    shims = []
    for source in sorted((ROOT / "tools/r29/qual_host/shim").rglob("*.java")):
        target = generated / source.relative_to(ROOT / "tools/r29/qual_host/shim")
        target.parent.mkdir(parents=True, exist_ok=True)
        text = source.read_text()
        if source.name == "SQLiteDatabase.java":
            text = text.replace("public class SQLiteDatabase {", "public class SQLiteDatabase implements AutoCloseable {")
            text = text.replace("    public void execSQL(String sql, Object[] args) {", "    public void execSQL(String sql, Object[] args) {\n        if(observer!=null)observer.sql(this,sql,args,inTransaction());")
            text = text.replace("    public Cursor rawQuery(String sql, String[] args) {", "    public Cursor rawQuery(String sql, String[] args) {\n        if(observer!=null)observer.sql(this,sql,args,inTransaction());")
            text = text.replace("            return new Rows(rows, columns);", '''            if(sql.startsWith("SELECT panel_id,CASE"))for(Object[] row:rows)if(row[1] instanceof String)maxPreparedPayloadBytes=Math.max(maxPreparedPayloadBytes,((String)row[1]).getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
            return new Rows(rows, columns);''')
            at = text.index("    static final class Rows")
            text = text[:at] + '''
    public interface Observer {
        void sql(SQLiteDatabase db,String sql,Object[] args,boolean inTransaction);
        void begin(SQLiteDatabase db);
        void end(SQLiteDatabase db,boolean committed,long elapsedNanos);
    }
    public static Observer observer;
    public static boolean rejectReadback;
    public static int independentOpens;
    public static long maxPreparedPayloadBytes;
    public boolean failBeforeCommitOnce,failAfterCommitOnce;
    private boolean successful;
    private long transactionStart;
    public interface CursorFactory {}
    public static final int OPEN_READONLY=1,NO_LOCALIZED_COLLATORS=16;
    public static SQLiteDatabase openDatabase(String path,CursorFactory factory,int flags){
        independentOpens++;
        if(rejectReadback)throw new SQLiteException("HOST_READBACK_UNAVAILABLE");
        try{return new SQLiteDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite:file:"+path+"?mode=ro"));}
        catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}
    }
    public void beginTransaction(){try{jdbc.setAutoCommit(false);successful=false;transactionStart=System.nanoTime();if(observer!=null)observer.begin(this);}catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}}
    public void setTransactionSuccessful(){successful=true;}
    public void endTransaction(){
        boolean before=successful&&failBeforeCommitOnce,after=successful&&failAfterCommitOnce,committed=false;
        failBeforeCommitOnce=false;failAfterCommitOnce=false;
        try{
            if(before){jdbc.rollback();throw new SQLiteException("HOST_END_ROLLED_BACK_BEFORE_ACK");}
            if(successful){jdbc.commit();committed=true;}else jdbc.rollback();
        }catch(java.sql.SQLException failure){try{jdbc.rollback();}catch(java.sql.SQLException ignored){}throw new SQLiteException(failure.getMessage());}
        finally{successful=false;try{jdbc.setAutoCommit(true);}catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}finally{if(observer!=null)observer.end(this,committed,System.nanoTime()-transactionStart);}}
        if(after)throw new SQLiteException("HOST_END_COMMITTED_BEFORE_ACK_FAILURE");
    }
    public void close(){try{jdbc.close();}catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}}
''' + text[at:]
        target.write_text(text)
        shims.append(target)
    extra = {
        "android/content/Context.java": 'package android.content;public class Context{public java.io.File getDatabasePath(String n){throw new IllegalStateException("HOST_PATH_REQUIRED");}public Context getApplicationContext(){return this;}}',
        "android/app/Application.java": 'package android.app;public class Application extends android.content.Context{private final java.io.File path;public Application(java.io.File p){path=p;}@Override public java.io.File getDatabasePath(String n){return path;}}',
        "android/os/SystemClock.java": 'package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000;}public static long uptimeMillis(){return elapsedRealtime();}public static long currentThreadTimeMillis(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()/1000000;}}',
        "android/os/Process.java": 'package android.os;public final class Process{public static int myPid(){return Math.toIntExact(ProcessHandle.current().pid());}}',
        "android/os/Looper.java": 'package android.os;public final class Looper{public static Looper myLooper(){return null;}public static Looper getMainLooper(){return null;}}',
        "android/util/Log.java": 'package android.util;public final class Log{public static int w(String t,String m){System.err.println(t+":"+m);return 0;}public static int w(String t,String m,Throwable e){System.err.println(t+":"+m+":"+e);return 0;}public static int e(String t,String m){return w(t,m);}public static int e(String t,String m,Throwable e){return w(t,m,e);}public static int d(String t,String m){return 0;}public static int i(String t,String m){return 0;}}',
        "com/ethk3/novaonepro/DbHelper.java": 'package com.ethk3.novaonepro;public final class DbHelper{private final android.database.sqlite.SQLiteDatabase db;public DbHelper(android.database.sqlite.SQLiteDatabase d){db=d;}public android.database.sqlite.SQLiteDatabase getReadableDatabase(){return db;}public android.database.sqlite.SQLiteDatabase getWritableDatabase(){return db;}public void close(){}}',
        "com/ethk3/novaonepro/DbStore.java": 'package com.ethk3.novaonepro;public final class DbStore{private final android.database.sqlite.SQLiteDatabase db;private final DatabaseCoordinator coordinator;public DbStore(android.database.sqlite.SQLiteDatabase d,DatabaseCoordinator c){db=d;coordinator=c;}public android.database.sqlite.SQLiteDatabase readable(){return db;}public DatabaseCoordinator coordinatorForResearchR952(){return coordinator;}}',
    }
    for name, text in extra.items():
        target = generated / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text)
        shims.append(target)
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, [sdk, options.support_jar])), "-d", platform, *shims], "compile-host-platform")
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, [sdk, options.support_jar])), "-d", prepared, *sources], "compile-production")
    receipt["compiledProductionClassSha256"] = {str(p.relative_to(prepared)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(prepared.rglob("*.class"))}
    receipt["hostShimSha256"] = {str(p.relative_to(generated)): hashlib.sha256(p.read_bytes()).hexdigest() for p in shims}
    baseline_sources = []
    for name in ["IssueTimelineWriterR30.java", "HoldLedgerFlushR30.java"]:
        relative = (MAIN / "strategyv2" / name).relative_to(ROOT)
        path = tmp / "baseline-source" / name
        path.parent.mkdir(exist_ok=True)
        path.write_bytes(subprocess.check_output(["git", "show", BASE + ":" + str(relative)], cwd=ROOT))
        baseline_sources.append(path)
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, [sdk, prepared, options.support_jar])), "-d", old, *baseline_sources], "compile-baseline-owners")
    receipt["baselineOwnerSourceSha256"] = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in baseline_sources}
    cp = os.pathsep.join(map(str, [platform, prepared, tests, options.support_jar, jdbc, sdk]))
    host_sources = sorted((ROOT / "tools/r31/telemetry_budget_host").glob("*.java"))
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", cp, "-d", tests, *host_sources,
         ROOT / "tools/r30-repair/qual_host/RepairTimelineR30HostTest.java"], "compile-native-tests")
    receipt["compiledHostTestClassSha256"] = {str(p.relative_to(tests)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(tests.rglob("*.class"))}
    receipt["hostTestSourceSha256"] = {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in host_sources}
    baseline_cp = os.pathsep.join(map(str, [platform, old, prepared, tests, options.support_jar, jdbc, sdk]))
    for case in ["baseline-timeline64", "baseline-hold65"]:
        run(["java", "-Xmx512m", "-cp", baseline_cp, "com.ethk3.novaonepro.strategyv2.R31TelemetryBudgetHost", case, output], case)
    cases = ["timeline64", "hold65", "timeline-first-sql-failure", "timeline-middle-sql-failure",
             "timeline-before-commit-failure", "timeline-after-commit-ack-failure", "timeline-unknown-then-receipt",
             "timeline-postcommit-diagnostic", "timeline-concurrent-source", "timeline-concurrent-new", "timeline-existing-durable",
             "hold-first-sql-failure", "hold-middle-sql-failure", "hold-before-commit-failure", "hold-after-commit-ack-failure",
             "hold-unknown-then-receipt", "hold-postcommit-diagnostic", "hold-concurrent-new", "missing-schema", "timeline-byte-budget", "timeline-byte-budget-64m", "hold-prune"]
    for case in cases:
        run(["java", "-Xmx64m" if case.endswith("-64m") else "-Xmx512m", "-cp", cp, "com.ethk3.novaonepro.strategyv2.R31TelemetryBudgetHost", case, output], case)
    run(["java", "-Xmx512m", "-cp", cp, "com.ethk3.novaonepro.strategyv2.RepairTimelineR30HostTest"], "original-timeline-regression")
    receipt["compiledProductionClassSha256"] = {str(p.relative_to(prepared)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(prepared.rglob("*.class"))}
    receipt["baselineOwnerSourceSha256"] = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in baseline_sources}
    receipt["hostShimSha256"] = {str(p.relative_to(generated)): hashlib.sha256(p.read_bytes()).hexdigest() for p in shims}
    receipt["productionSourcesUnchangedDuringRun"] = all(hashlib.sha256((ROOT / p).read_bytes()).hexdigest() == h for p, h in hashes.items())
    if not receipt["productionSourcesUnchangedDuringRun"]:
        raise RuntimeError("Production source changed during run; no coherent source receipt")
receipt["status"] = "HOST_NATIVE_TESTS_COMPLETED"
receipt["completedAt"] = datetime.datetime.now(datetime.timezone.utc).isoformat()
(output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
print("R31_TELEMETRY_BUDGET_HOST_COMPLETED_NOT_DEVICE", output, flush=True)
