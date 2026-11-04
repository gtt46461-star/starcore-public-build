"""Run actual R31 task/lease/receipt methods against isolated native SQLite/JDBC, never a device receipt."""
import os
import hashlib
import json
import re
from datetime import datetime, timezone
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/ethk3/novaonepro"
CLASSES = ROOT / "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes"
SDK = Path(os.environ["ANDROID_HOME"]) / "platforms/android-36/android.jar"
JDBC = Path(os.environ.get("SQLITE_JDBC", "/tmp/r31-baseline-deps/sqlite-jdbc.jar"))
HOST_CLASS=os.environ.get("R31_RUNTIME_HOST_CLASS","R31TaskRuntimeHost")
if HOST_CLASS not in {"R31TaskRuntimeHost","R31RuntimeHealthHost"}:raise ValueError("unsupported host harness")
OUTPUT=ROOT/os.environ.get("R31_RUNTIME_OUTPUT","verification/r31/p02/runtime-health" if HOST_CLASS=="R31RuntimeHealthHost" else "verification/r31/p01/task-runtime")


def run(args):
    subprocess.run(list(map(str, args)), cwd=ROOT, check=True)


with tempfile.TemporaryDirectory(prefix="r31-production-runtime-") as temporary:
    tmp = Path(temporary)
    platform, sources, production, test = [tmp / name for name in ["platform", "sources", "production", "test"]]
    for directory in [platform, sources, production, test]:
        directory.mkdir()
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
    private boolean successful;
    public static final int OPEN_READONLY=1;
    public interface CursorFactory {}
    public static SQLiteDatabase openDatabase(String path,CursorFactory factory,int flags){try{return new SQLiteDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite:"+path));}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
    public void beginTransaction(){try{jdbc.setAutoCommit(false);successful=false;}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
    public void setTransactionSuccessful(){successful=true;}
    public void endTransaction(){try{if(successful)jdbc.commit();else jdbc.rollback();}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}finally{successful=false;try{jdbc.setAutoCommit(true);}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}}
    public void close(){try{jdbc.close();}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
''' + text[at:]
        target.write_text(text)
        shim_files.append(target)
    adapters = {
        "android/content/Context.java": 'package android.content;public class Context{public java.io.File getDatabasePath(String name){throw new IllegalStateException("HOST_PATH_REQUIRED");}public Context getApplicationContext(){return this;}}',
        "android/app/Application.java": 'package android.app;public class Application extends android.content.Context{private final java.io.File path;public Application(java.io.File path){this.path=path;}@Override public java.io.File getDatabasePath(String name){return path;}}',
        "android/os/SystemClock.java": 'package android.os;public final class SystemClock{public static long offset;public static long elapsedRealtime(){return System.nanoTime()/1000000+offset;}public static long uptimeMillis(){return elapsedRealtime();}public static long currentThreadTimeMillis(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()/1000000L;}}',
        "android/os/Debug.java": 'package android.os;public final class Debug{public static long threadCpuTimeNanos(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();}}',
        "android/os/Process.java": 'package android.os;public final class Process{public static final int THREAD_PRIORITY_BACKGROUND=10;public static int myPid(){return(int)java.lang.ProcessHandle.current().pid();}public static void setThreadPriority(int priority){}}',
        "android/os/Looper.java": 'package android.os;public final class Looper{public static Looper myLooper(){return null;}public static Looper getMainLooper(){return null;}}',
        "android/util/Log.java": 'package android.util;public final class Log{public static int w(String t,String m){return 0;}public static int w(String t,String m,Throwable e){return 0;}public static int e(String t,String m){return 0;}public static int e(String t,String m,Throwable e){return 0;}public static int d(String t,String m){return 0;}public static int i(String t,String m){return 0;}}',
        "com/ethk3/novaonepro/DbHelper.java": 'package com.ethk3.novaonepro;import android.database.sqlite.SQLiteDatabase;public final class DbHelper{private final SQLiteDatabase db;public DbHelper(SQLiteDatabase db){this.db=db;}public SQLiteDatabase getReadableDatabase(){return db;}public SQLiteDatabase getWritableDatabase(){return db;}public void close(){}}',
        "com/ethk3/novaonepro/DbStore.java": 'package com.ethk3.novaonepro;import android.database.sqlite.SQLiteDatabase;public final class DbStore{private final DatabaseCoordinator coordinator;private final SQLiteDatabase db;private final android.content.Context context;public DbStore(DatabaseCoordinator coordinator,SQLiteDatabase db,android.content.Context context){this.coordinator=coordinator;this.db=db;this.context=context;}public DatabaseCoordinator coordinatorForResearchR952(){return coordinator;}public SQLiteDatabase readable(){return db;}public android.content.Context contextForR26(){return context;}}',
    }
    for name, text in adapters.items():
        target = sources / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text)
        shim_files.append(target)
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", str(SDK) + os.pathsep + str(CLASSES), "-d", platform, *shim_files])
    production_names = ["DatabaseCoordinator", "TransactionSchemaCacheR941", "SchemaDomainCoordinatorR926"]
    production_names += ["strategyv2/" + name for name in ["FailureDispositionR31", "RuntimeFaultEvidenceR31", "WaitKindR27", "SchemaR27", "RuntimeCoordinatorR26", "ResearchPoolR27", "PreparedTaskR30R1", "CooperativeContinuationR26", "ResearchWriteQueueR27","MemoryBudgetR31","BoundedResearchExecutorR31","SharedFeatureStoreR31","FeatureCacheR27","PolicyAstR27","RuntimeSchemaR26","TaskContinuationR26","LegacyGenerationR26","LegacyReplayContinuationR26"]]
    compiled_hashes={str((MAIN/(name+".java")).relative_to(ROOT)):hashlib.sha256((MAIN/(name+".java")).read_bytes()).hexdigest() for name in production_names}
    compile_commit=subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip()
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", str(SDK) + os.pathsep + str(CLASSES), "-sourcepath", ROOT / "app/src/main/java", "-d", production, *[MAIN / (name + ".java") for name in production_names]])
    if compiled_hashes!={str((MAIN/(name+".java")).relative_to(ROOT)):hashlib.sha256((MAIN/(name+".java")).read_bytes()).hexdigest() for name in production_names}:
        raise RuntimeError("production source changed during compilation; rerun with owners frozen")
    classpath = os.pathsep.join(map(str, [platform, production, test, CLASSES, JDBC, SDK]))
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", classpath, "-d", test, ROOT / ("tools/r31/host/"+HOST_CLASS+".java")])
    command=["java", "-Xmx512m", "-cp", classpath, "com.ethk3.novaonepro.strategyv2."+HOST_CLASS, str(tmp / "runtime.db")]
    completed=subprocess.run(command,cwd=ROOT,text=True,capture_output=True)
    print(completed.stdout,end="");print(completed.stderr,end="")
    completed.check_returncode()
    checks=int(re.search(r"(?:R31_TASK_RUNTIME_HOST_PASS|R31_RUNTIME_HEALTH_HOST_PASS) assertions=(\d+)",completed.stdout).group(1))
    classifier_tests=0
    if HOST_CLASS=="R31RuntimeHealthHost":
        gradle=Path(os.environ.get("GRADLE_USER_HOME","/workspace/starcore-environment/gradle-home"))
        junit=next(gradle.glob("caches/modules-2/files-2.1/junit/junit/4.13.2/*/junit-4.13.2.jar"))
        hamcrest=next(gradle.glob("caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/*/hamcrest-core-1.3.jar"))
        junit_cp=classpath+os.pathsep+str(junit)+os.pathsep+str(hamcrest)
        run(["javac","-encoding","UTF-8","-cp",junit_cp,"-d",test,ROOT/"app/src/test/java/com/ethk3/novaonepro/strategyv2/R31TaskFailureDispositionTest.java"])
        classified=subprocess.run(["java","-cp",junit_cp,"org.junit.runner.JUnitCore","com.ethk3.novaonepro.strategyv2.R31TaskFailureDispositionTest"],cwd=ROOT,text=True,capture_output=True)
        print(classified.stdout,end="");print(classified.stderr,end="");classified.check_returncode()
        classifier_tests=int(re.search(r"OK \((\d+) tests\)",classified.stdout).group(1))
    receipt={"status":"PASS","scope":"ACTUAL_PRODUCTION_TASK_PROTOCOL_WITH_NATIVE_SQLITE_JDBC_HOST_PLATFORM_AND_CLOCK_ADAPTERS","capturedAt":datetime.now(timezone.utc).isoformat(),"assertions":checks,"command":"source /workspace/starcore-environment/activate.sh; python3 tools/r31/run_task_runtime_host.py","sourceCommitAtCompile":compile_commit,"dirtyAtRun":bool(subprocess.check_output(["git","status","--porcelain"],cwd=ROOT,text=True).strip()),"productionSources":compiled_hashes,"scenarios":["late owner success and failure receipts","boot/elapsed lease and wall-deadline perturbation","100 cancellations without fault budget","six same-root half-open fault rounds","one recovery probe per second","13 parallel actual claims under writer lock held 3 real seconds","actual pool loop retains parent/helper ownership","manual retry waits for actual helper exit","cancelled Future is not actual helper exit","close is bounded and idempotent"],"androidRuntime":"NOT_RUN","physicalDevice":"NOT_RUN","continuous24h":"NOT_RUN","continuous72h":"NOT_RUN","naturalQuality":"NOT_PROVEN","remaining":["Android SQLite/thread integration and device lifecycle matrix","real OS wall-clock/reboot test","device resource/latency/capacity acceptance","full model process-kill recovery matrix"]}
    receipt["harness"]=HOST_CLASS
    receipt["actualFailureClassifierJUnitTests"]=classifier_tests
    if HOST_CLASS=="R31RuntimeHealthHost":receipt["scenarios"]=["single bounded production SQL snapshot", "13 quarantines immediately degraded", "unknown pending/source/ABI not zero", "per-task stalled detection despite other fresh artifacts", "resource/dependency separation", "actual cached getter and asynchronous bounded control", "same-owner named cancellation and real drain before restart", "immutable DTO and future clock rejection"];receipt["command"]="R31_RUNTIME_HOST_CLASS=R31RuntimeHealthHost python3 tools/r31/run_task_runtime_host.py"
    output=OUTPUT/"receipt.json";output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+"\n")
print("R31_TASK_RUNTIME_NATIVE_SQLITE_HOST_PASS_NOT_ANDROID_NOT_DEVICE_NOT_SOAK")
