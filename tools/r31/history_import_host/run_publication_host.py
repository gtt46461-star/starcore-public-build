#!/usr/bin/env python3
"""Actual HistoryImport prepared/complete protocol on native SQLite and separately labelled HOST POSIX stat.

Uses a frozen real Android class jar; replaces platform boundaries only. It does not run Gradle,
change default budget limits/probes/VM properties, or claim Android/device/natural-quality acceptance.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import time

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools/r31"))
from host_runtime_fixture import compile_owned_platform
from host_tool_support import dependencies, run

parser = argparse.ArgumentParser()
parser.add_argument("--modes", default="stock-unknown,suite,rollback-cancel,io-debt,source-members,source-race,streaming-format,crash-chunk,lease-pending,resume-chunk,crash-members,member-lease-pending,resume-members,crash-publish,resume-publish")
parser.add_argument("--out", type=Path, default=ROOT / "verification/r31/p01/history-import/current")
args = parser.parse_args()
OUT = args.out.resolve()
OUT.mkdir(parents=True, exist_ok=True)
CLASSES, SDK, RJAR, JDBC = dependencies(ROOT, require_resources=True)
JAVA = Path(os.environ["JAVA_HOME"]) / "bin/java" if os.environ.get("JAVA_HOME") else Path(shutil.which("java"))
JAVAC = JAVA.with_name("javac")
JAVA_HOME = JAVA.parent.parent
journal = Path(os.environ.get("R31_SOURCE_JOURNAL_SOURCE", ROOT / "app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchSourceJournalR31.java"))
if not journal.is_file():
    raise FileNotFoundError("actual same-version ResearchSourceJournalR31 producer required: " + str(journal))
temporary = Path(tempfile.mkdtemp(prefix="r31-history-publication-"))
sources = [ROOT / "app/src/main/java/com/ethk3/novaonepro/strategyv2/HistoryImportR27.java", journal]
joint_manifest = os.environ.get("R31_HISTORY_JOINT_SOURCES")
if joint_manifest:
    # Before integration, explicit actual owners may live in separate authorized worktrees.
    # Snapshot them together; no old Pin/Schema body is silently retained for process reentry.
    sources += [Path(name) for name in json.loads(Path(joint_manifest).read_text())]
sources += sorted((ROOT / "tools/r31/history_import_host").glob("*.java"))
sources += [ROOT / "tools/r31/host/R31OwnedRuntimeFixture.java"]
snapshot = temporary / "source-snapshot"
snapshot.mkdir()
compiled_sources = []
producer_sources = []
host_sources = []
hashes = {}
for index, source in enumerate(sources):
    if source.name == "HistoryImportBaselineR31Host.java":
        continue
    target = snapshot / str(index) / source.name
    target.parent.mkdir()
    shutil.copyfile(source, target)
    hashes[str(source)] = hashlib.sha256(target.read_bytes()).hexdigest()
    compiled_sources.append(target)
    (producer_sources if "/app/src/main/java/" in str(source) else host_sources).append(target)
ui_origin = ROOT / "app/src/main/java/com/ethk3/novaonepro/ChartsActivityR83.java"
ui_snapshot = snapshot / "ui" / ui_origin.name
ui_snapshot.parent.mkdir()
shutil.copyfile(ui_origin, ui_snapshot)
hashes[str(ui_origin)] = hashlib.sha256(ui_snapshot.read_bytes()).hexdigest()
native_origin = ROOT / "tools/r31/history_import_host/history_posix_stat_r31.c"
native_snapshot = snapshot / native_origin.name
shutil.copyfile(native_origin, native_snapshot)
hashes[str(native_origin)] = hashlib.sha256(native_snapshot.read_bytes()).hexdigest()
receipt = dict(scope="ACTUAL_PRODUCER_NATIVE_SQLITE_DEFAULT_BUDGET_HOST_POSIX_MEASUREMENT_BOUNDARY_NOT_ANDROID_OR_NATURAL_QUALITY", worktree=str(ROOT), head=subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(), temporary=str(temporary), sourceSha256=hashes, records=[], status="IMPLEMENTING")
(OUT / "EXECUTION.json").write_text(json.dumps(receipt, indent=2) + "\n")
try:
    platform = compile_owned_platform(ROOT, temporary / "owned", CLASSES, SDK)
    adapter_sources = temporary / "owned/sources"
    context = adapter_sources / "android/content/Context.java"
    context.write_text(context.read_text().replace("public Context getApplicationContext()", 'public java.io.File getFilesDir(){throw new IllegalStateException("HOST_FILES_PATH_REQUIRED");}public Context getApplicationContext()'))
    application = adapter_sources / "android/app/Application.java"
    application.write_text(application.read_text().replace("@Override public java.io.File getDatabasePath", '@Override public java.io.File getFilesDir(){java.io.File files=new java.io.File(path.getParentFile().getParentFile(),"files");if(!files.isDirectory()&&!files.mkdirs())throw new IllegalStateException("HOST_FILES_DIRECTORY");return files;}@Override public java.io.File getDatabasePath'))
    process = adapter_sources / "android/os/Process.java"
    process.write_text(process.read_text().replace("public static final int", "public static int myPid(){long pid=java.lang.ProcessHandle.current().pid();if(pid<1||pid>Integer.MAX_VALUE)throw new IllegalStateException(\"HOST_PID_RANGE\");return(int)pid;}public static final int"))
    db_source = adapter_sources / "android/database/sqlite/SQLiteDatabase.java"
    text = db_source.read_text()
    text = text.replace("public class SQLiteDatabase {", "public class SQLiteDatabase {public static volatile boolean traceHistoryR31;public static volatile Runnable afterCommitR31;public static long historyDdlR31,maxHistoryOpsR31,maxHistoryNanosR31;private long historyOpsR31,historyStartedR31;")
    text = text.replace("public void beginTransaction(){", "public void beginTransaction(){historyOpsR31=0;historyStartedR31=System.nanoTime();")
    text = text.replace("    public void execSQL(String sql, Object[] args) {", '    public void execSQL(String sql, Object[] args) {if(traceHistoryR31){historyOpsR31++;String normalized=sql.trim().toUpperCase(java.util.Locale.ROOT);if(normalized.startsWith("CREATE ")||normalized.startsWith("ALTER ")||normalized.startsWith("DROP "))historyDdlR31++;}')
    text = text.replace("if(successful)jdbc.commit();else jdbc.rollback();", "if(successful){jdbc.commit();if(traceHistoryR31){maxHistoryOpsR31=Math.max(maxHistoryOpsR31,historyOpsR31);maxHistoryNanosR31=Math.max(maxHistoryNanosR31,System.nanoTime()-historyStartedR31);if(afterCommitR31!=null)afterCommitR31.run();}}else jdbc.rollback();")
    db_source.write_text(text)
    run(ROOT, ["javac", "-cp", os.pathsep.join(map(str, [platform, CLASSES, SDK])), "-d", platform, context, application, process, db_source])
    native_source = native_snapshot
    library = temporary / "libhistory_posix_stat_r31.so"
    run(ROOT, ["cc", "-std=c11", "-O2", "-Wall", "-Wextra", "-Werror", "-fPIC", "-shared", "-I" + str(JAVA_HOME / "include"), "-I" + str(JAVA_HOME / "include/linux"), native_source, "-o", library])
    receipt["hostMeasurementBoundary"] = dict(source=str(native_source), sourceSha256=hashlib.sha256(native_source.read_bytes()).hexdigest(), library=str(library), librarySha256=hashlib.sha256(library.read_bytes()).hexdigest(), method="REAL_POSIX_STAT_ST_BLOCKS_TIMES_512; only private measurement fields bound; no VM property/config/traversal changes", androidStatus="NOT_RUN")
    current = temporary / "current-classes"
    current.mkdir()
    # Production must see the real DbHelper constants/Android Cursor contract. HOST adapters
    # are deliberately selected only when compiling the native test arrangement.
    run(ROOT, ["javac", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, [CLASSES, SDK])), "-d", current, *producer_sources])
    run(ROOT, ["javac", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, [platform, current, CLASSES, SDK])), "-d", current, *host_sources])
    # Compile the actual UI against Android SDK/resources, never the HOST platform adapters.
    run(ROOT, ["javac", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, [current, CLASSES, RJAR, SDK])), "-d", temporary / "ui-classes", ui_snapshot])
    receipt["uiSourceSha256"] = hashes[str(ui_origin)]
    cp = os.pathsep.join(map(str, [platform, current, CLASSES, JDBC, SDK]))
    modes = args.modes.split(",")
    for mode in modes:
        family = "chunk-process" if mode in {"crash-chunk", "lease-pending", "resume-chunk"} else "member-process" if mode in {"crash-members", "member-lease-pending", "resume-members"} else "publish-process" if mode in {"crash-publish", "resume-publish"} else mode
        database = temporary / family / "databases/main.db"
        if mode in {"resume-chunk", "resume-members"}:
            # The real elapsed-time lease is allowed to expire; no DB clock/lease field is edited.
            import sqlite3
            with sqlite3.connect(database) as connection:
                until = connection.execute("SELECT lease_elapsed_ms FROM r31_history_import_run").fetchone()[0]
            remaining = max(0, until / 1000.0 - time.monotonic() + 0.1)
            print("WAIT_REAL_PRODUCTION_OWNER_LEASE_SECONDS " + str(remaining), flush=True)
            if remaining > 31:
                raise AssertionError("unexpected lease duration " + str(remaining))
            if remaining:
                time.sleep(remaining)
        command = [str(JAVA), "-Xmx512m", "-cp", cp, "com.ethk3.novaonepro.strategyv2.HistoryImportPublicationR31Host", mode, str(database), str(library)]
        start = time.monotonic()
        completed = subprocess.run(command, cwd=ROOT, text=True, capture_output=True)
        log = OUT / (mode + ".log")
        log.write_text(completed.stdout + completed.stderr)
        expected = 73 if mode in {"crash-chunk", "crash-members", "crash-publish"} else 0
        item = dict(mode=mode, command=command, expectedExit=expected, exit=completed.returncode, elapsedSeconds=time.monotonic() - start, log=str(log))
        receipt["records"].append(item)
        (OUT / "EXECUTION.json").write_text(json.dumps(receipt, indent=2) + "\n")
        print(json.dumps(item), flush=True)
        print(completed.stdout, flush=True)
        if completed.returncode != expected:
            print(completed.stderr, flush=True)
            raise RuntimeError("actual history protocol failure " + mode)
    receipt["status"] = "PASS_SCOPED_HOST_CHECKS_ONLY"
except BaseException as failure:
    receipt["status"] = "FAILED"
    receipt["failure"] = repr(failure)
    raise
finally:
    receipt["originsChangedSinceSnapshot"] = [name for name, digest in hashes.items() if hashlib.sha256(Path(name).read_bytes()).hexdigest() != digest]
    archived = OUT / "source-snapshot"
    shutil.copytree(snapshot, archived, dirs_exist_ok=True)
    receipt["archivedSourceSnapshot"] = str(archived)
    receipt["finishedAtUnixSeconds"] = time.time()
    (OUT / "EXECUTION.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print("EVIDENCE " + str(OUT / "EXECUTION.json"), flush=True)
