"""Original atom/molecule producers + complete publication on real coordinator/native HOST SQLite."""
import argparse
import datetime
import hashlib
import json
import os
import pathlib
import subprocess
import tempfile
import shutil
import gzip

ROOT = pathlib.Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/ethk3/novaonepro"
BASE = "241a93848851440fdfd21155ae400ff2354eceb2"
def support_digest(path):
    if path.is_file():
        return hashlib.sha256(path.read_bytes()).hexdigest()
    if path.is_dir():
        h = hashlib.sha256()
        for entry in sorted(path.rglob("*.class")):
            h.update(str(entry.relative_to(path)).encode())
            h.update(b"\0")
            h.update(hashlib.sha256(entry.read_bytes()).digest())
        return h.hexdigest()
    raise RuntimeError("Complete Android support classes are unavailable: supply --support-jar with an actual built classes directory or jar")
current_classes = ROOT / "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes"
default_support = os.environ.get("R31_ATOM_SUPPORT_CLASSES") or (str(current_classes) if (current_classes / "com/ethk3/novaonepro/DatabaseCoordinator.class").is_file() else "/tmp/r31-ba35-host-integration-mifthdj3/frozen-debug-classes.jar")
parser = argparse.ArgumentParser()
parser.add_argument("--output", type=pathlib.Path, required=True)
parser.add_argument("--support-jar", type=pathlib.Path, default=pathlib.Path(default_support), help="actual complete Android classes jar or directory")
parser.add_argument("--cases", default="brier-original", help="comma separated explicit native cases")
options = parser.parse_args()
output = options.output.resolve()
output.mkdir(parents=True, exist_ok=False)
sdk = pathlib.Path(os.environ["ANDROID_HOME"]) / "platforms/android-36/android.jar"
jdbc = pathlib.Path(os.environ.get("SQLITE_JDBC", "/tmp/r31-baseline-deps/sqlite-jdbc.jar"))
sources = [MAIN / "DatabaseCoordinator.java", MAIN / "TransactionSchemaCacheR941.java"] + [MAIN / "strategyv2" / name for name in [
    "FailureDispositionR31.java", "IssueTimelineR30.java", "WriterHoldLedgerR30.java", "IssueTimelineWriterR30.java", "HoldLedgerFlushR30.java",
    "AtomWorldR27.java", "AtomPublicationStoreR31.java", "ResearchWriteQueueR27.java", "ResearchStoreR30R1.java",
    "RuntimeCoordinatorR26.java", "ResearchPoolR27.java", "ResearchInputPinR30R1.java", "SchemaR27.java", "RuntimeR27.java",
    "ChartCacheR27.java", "ReportDataR27.java", "ReportR29.java", "ResearchTableAdaptersR30R1.java", "ReportSnapshotR30.java"]]

hashes = {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}
receipt = {"sourceCapturedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
           "base": BASE, "sourceSha256": hashes,
           "supportJar": str(options.support_jar), "supportJarSha256": support_digest(options.support_jar),
           "jdbcSha256": hashlib.sha256(jdbc.read_bytes()).hexdigest(),
           "androidJarSha256": hashlib.sha256(sdk.read_bytes()).hexdigest(),
           "scope": "Actual production atom/molecule/coordinator sources; native JDBC SQLite HOST adapters. Synthetic3000/601 fixture retains original opportunity sets and poisoned CONFIRM labels; NOT_ANDROID_NOT_DEVICE_NOT_24H_NOT_72H_NOT_NATURAL_QUALITY",
           "status": "IN_PROGRESS", "runs": []}
receipt["mode"] = "ATOM_COMPLETE_PUBLICATION_PROTOCOL"
receipt["supportKind"] = "CLASS_DIRECTORY" if options.support_jar.is_dir() else "JAR"
receipt["sourceGitHeadAtRun"] = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
receipt["baselineOwnerSourceCommit"] = BASE
native_env = dict(os.environ)
native_env.pop("JAVA_TOOL_OPTIONS", None)  # Local JNI/JDK tests need no proxy JVM properties in public logs.
receipt["javaHome"] = os.environ.get("JAVA_HOME")
receipt["javaExecutable"] = shutil.which("java")
receipt["javacExecutable"] = shutil.which("javac")
(output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")

def archive_raw_sql():
    archives=receipt.setdefault("losslessRawSqlArchives",{})
    for raw in sorted(output.glob("*-sql.jsonl")):
        payload=raw.read_bytes()
        packed=raw.with_suffix(raw.suffix+".gz")
        with packed.open("wb") as sink:
            with gzip.GzipFile(filename="",fileobj=sink,mode="wb",mtime=0) as compressor:
                compressor.write(payload)
        recovered=gzip.decompress(packed.read_bytes())
        if recovered!=payload:
            raise RuntimeError("Lossless SQL archive verification failed")
        archives[raw.name]={"file":packed.name,"rawBytes":len(payload),"rawSha256":hashlib.sha256(payload).hexdigest(),"gzipBytes":packed.stat().st_size,"gzipSha256":hashlib.sha256(packed.read_bytes()).hexdigest(),"roundTripVerified":True}
        raw.unlink()


def run(args, name, expected_exit=0):
    args = list(map(str, args))
    try:
        result = subprocess.run(args, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, env=native_env, timeout=180)
    except subprocess.TimeoutExpired as failure:
        raw = failure.stdout or b""
        (output / (name + ".log")).write_text(raw.decode(errors="replace") if isinstance(raw, bytes) else raw)
        receipt["status"] = "FAILED_TIMEOUT"
        receipt["runs"].append({"name": name, "command": args, "exitCode": None, "timeoutSeconds": 180})
        archive_raw_sql()
        (output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
        raise
    (output / (name + ".log")).write_text(result.stdout)
    receipt["runs"].append({"name": name, "command": args, "exitCode": result.returncode, "expectedExitCode": expected_exit})
    (output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(name, "exit=" + str(result.returncode), flush=True)
    if result.returncode != expected_exit:
        receipt["status"] = "FAILED"
        receipt["failedCommand"] = name
        archive_raw_sql()
        (output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
        print(result.stdout[-14000:], flush=True)
        raise RuntimeError(name + " failed; original log retained")


with tempfile.TemporaryDirectory(prefix="r31-atom-publication-") as temporary:
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
        if source.name == "SQLiteDatabase.java":
            text = text.replace("            p.execute();", "            p.execute();\n            if(observer!=null)observer.executed(this,sql,args,((org.sqlite.SQLiteConnection)jdbc).getDatabase().changes(),((org.sqlite.SQLiteConnection)jdbc).getDatabase().total_changes());")
            text = text.replace("        void begin(SQLiteDatabase db);", "        default void executed(SQLiteDatabase db,String sql,Object[] args,long changed,long totalChanges){}\n        void begin(SQLiteDatabase db);")
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
        if source.name == "SQLiteDatabase.java":
            text = text.replace("            p.execute();", "            p.execute();\n            if(observer!=null)observer.executed(this,sql,args,((org.sqlite.SQLiteConnection)jdbc).getDatabase().changes(),((org.sqlite.SQLiteConnection)jdbc).getDatabase().total_changes());")
            text = text.replace("        void begin(SQLiteDatabase db);", "        default void executed(SQLiteDatabase db,String sql,Object[] args,long changed,long totalChanges){}\n        void begin(SQLiteDatabase db);")
        target.write_text(text)
        shims.append(target)
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, [sdk, options.support_jar, jdbc])), "-d", platform, *shims], "compile-host-platform")
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, [sdk, options.support_jar])), "-d", prepared, *sources], "compile-production")
    receipt["compiledProductionClassSha256"] = {str(p.relative_to(prepared)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(prepared.rglob("*.class"))}
    receipt["hostShimSha256"] = {str(p.relative_to(generated)): hashlib.sha256(p.read_bytes()).hexdigest() for p in shims}
    cp = os.pathsep.join(map(str, [platform, prepared, tests, options.support_jar, jdbc, sdk]))
    host_sources = [ROOT / "tools/r31/atom_publication_host/R31AtomPublicationHost.java", ROOT / "tools/r30/qual_host/BrierContributionR30HostTest.java"]
    receipt["hostTestSourceSha256"] = {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in host_sources}
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", cp, "-d", tests, *host_sources], "compile-native-tests")
    receipt["compiledHostTestClassSha256"] = {str(p.relative_to(tests)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(tests.rglob("*.class"))}
    baseline_source = tmp / "old-source/AtomWorldR27.java"
    baseline_source.parent.mkdir()
    baseline_source.write_bytes(subprocess.check_output(["git", "show", BASE+":app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomWorldR27.java"], cwd=ROOT))
    receipt["baselineSourceSha256"] = hashlib.sha256(baseline_source.read_bytes()).hexdigest()
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str,[sdk,prepared,options.support_jar])), "-d", old, baseline_source], "compile-baseline-atom-owner")
    cases=options.cases.split(",")
    if "brier-original" in cases and "baseline-molecules32" not in cases:
        cases.insert(0,"baseline-molecules32")  # Actual old producer output is the numerical reference, never a frozen example.
    for case in cases:
        if case in ["process-halt-resume", "process-inbody-halt-resume"]:
            body = case.startswith("process-inbody")
            haltcase = "process-inbody-halt" if body else "process-aftercommit-halt"
            resumecase = "process-inbody-resume-64m" if body else "process-aftercommit-resume-64m"
            run(["java","-Xmx768m","-cp",cp,"com.ethk3.novaonepro.strategyv2.R31AtomPublicationHost",haltcase,output],haltcase,74 if body else 73)
            run(["java","-Xmx64m","-cp",cp,"com.ethk3.novaonepro.strategyv2.R31AtomPublicationHost",resumecase,output],resumecase)
            fixture = output / ("process-inbody-fixture.db" if body else "process-aftercommit-fixture.db")
            receipt.setdefault("processFixtureFinalSha256",{})[str(fixture.name)] = hashlib.sha256(fixture.read_bytes()).hexdigest()
            for suffix in ["", "-wal", "-shm", "-journal"]:
                pathlib.Path(str(fixture)+suffix).unlink(missing_ok=True)
            continue
        usecp = os.pathsep.join(map(str,[platform,old,prepared,tests,options.support_jar,jdbc,sdk])) if case.startswith("baseline-") else cp
        target = "com.ethk3.novaonepro.strategyv2.BrierContributionR30HostTest" if case=="brier-original" else "com.ethk3.novaonepro.strategyv2.R31AtomPublicationHost"
        heap = "-Xmx64m" if case.endswith("-64m") else "-Xmx768m"
        run(["java",heap,"-cp",usecp,target,case,output],case)
    receipt["productionSourcesUnchangedDuringRun"] = all(hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==h for p,h in hashes.items())
    receipt["supportClassesUnchangedDuringRun"] = support_digest(options.support_jar)==receipt["supportJarSha256"]
    if not receipt["productionSourcesUnchangedDuringRun"] or not receipt["supportClassesUnchangedDuringRun"]:
        raise RuntimeError("Inputs changed during run; no coherent source receipt")
receipt["status"]="TARGETED_HOST_ASSERTIONS_COMPLETED_NOT_DEVICE"
receipt["completedAt"]=datetime.datetime.now(datetime.timezone.utc).isoformat()
archive_raw_sql()
(output/"receipt.json").write_text(json.dumps(receipt,indent=2)+"\n")
print("R31_ATOM_PUBLICATION_HOST_COMPLETED_NOT_DEVICE",output,flush=True)
