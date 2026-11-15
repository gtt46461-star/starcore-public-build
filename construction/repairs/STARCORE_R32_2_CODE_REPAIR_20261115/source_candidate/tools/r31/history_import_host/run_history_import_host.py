#!/usr/bin/env python3
"""Isolated actual importer/native SQLite negative controls and later prepared publication contracts."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import time

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools/r31"))
from host_runtime_fixture import compile_owned_platform
from host_tool_support import dependencies, run

BASE = "241a93848851440fdfd21155ae400ff2354eceb2"
OUT = ROOT / "verification/r31/p01/history-import"
OUT.mkdir(parents=True, exist_ok=True)
CLASSES, SDK, RJAR, JDBC = dependencies(ROOT)

with tempfile.TemporaryDirectory(prefix="r31-history-native-") as directory:
    temporary = Path(directory)
    platform = compile_owned_platform(ROOT, temporary / "owned", CLASSES, SDK)
    # Observe real JDBC calls/commit boundaries. No SQL outcome or production guard is replaced.
    db_source = temporary / "owned/sources/android/database/sqlite/SQLiteDatabase.java"
    text = db_source.read_text()
    text = text.replace("public class SQLiteDatabase {", "public class SQLiteDatabase {public static volatile boolean traceHistoryR31;public static volatile Runnable afterCommitR31;public static long historyDdlR31,maxHistoryOpsR31,maxHistoryNanosR31;private long historyOpsR31,historyStartedR31;")
    text = text.replace("public void beginTransaction(){", "public void beginTransaction(){historyOpsR31=0;historyStartedR31=System.nanoTime();")
    text = text.replace("    public void execSQL(String sql, Object[] args) {", '    public void execSQL(String sql, Object[] args) {if(traceHistoryR31){historyOpsR31++;String normalized=sql.trim().toUpperCase(java.util.Locale.ROOT);if(normalized.startsWith("CREATE ")||normalized.startsWith("ALTER ")||normalized.startsWith("DROP "))historyDdlR31++;}')
    text = text.replace("if(successful)jdbc.commit();else jdbc.rollback();", "if(successful){jdbc.commit();if(traceHistoryR31){maxHistoryOpsR31=Math.max(maxHistoryOpsR31,historyOpsR31);maxHistoryNanosR31=Math.max(maxHistoryNanosR31,System.nanoTime()-historyStartedR31);if(afterCommitR31!=null)afterCommitR31.run();}}else jdbc.rollback();")
    db_source.write_text(text)
    run(ROOT, ["javac", "-cp", os.pathsep.join(map(str, [platform, CLASSES, SDK])), "-d", platform, db_source])
    old_source = temporary / "baseline/HistoryImportR27.java"
    old_source.parent.mkdir()
    old_source.write_bytes(subprocess.check_output(["git", "show", BASE + ":app/src/main/java/com/ethk3/novaonepro/strategyv2/HistoryImportR27.java"], cwd=ROOT))
    baseline = temporary / "old-classes"
    baseline.mkdir()
    run(ROOT, ["javac", "-cp", os.pathsep.join(map(str, [platform, CLASSES, SDK])), "-d", baseline, old_source, ROOT / "tools/r31/host/R31OwnedRuntimeFixture.java", ROOT / "tools/r31/history_import_host/HistoryImportBaselineR31Host.java"])
    cp = os.pathsep.join(map(str, [platform, baseline, CLASSES, JDBC, SDK]))
    records = []
    for name, mode, rows, database, expected in [
        ("old-large-repeat", "repeat", 4000, "large.db", 0),
        ("old-commit-then-kill", "crash-after-commit", 64, "crash.db", 73),
        ("old-reentry-after-kill", "once", 64, "crash.db", 0),
    ]:
        command = ["java", "-Xmx512m", "-cp", cp, "com.ethk3.novaonepro.strategyv2.HistoryImportBaselineR31Host", mode, str(temporary / "databases" / database), str(rows)]
        started = time.monotonic()
        completed = subprocess.run(command, cwd=ROOT, text=True, capture_output=True)
        (OUT / (name + ".log")).write_text(completed.stdout + completed.stderr)
        records.append(dict(name=name, command=command, exit=completed.returncode, expectedExit=expected, elapsedSeconds=time.monotonic() - started))
        print(json.dumps(records[-1]), flush=True)
        print(completed.stdout, flush=True)
        if completed.returncode != expected:
            print(completed.stderr)
            raise RuntimeError("unexpected original-importer result " + name)
    (OUT / "BASELINE_EXECUTION.json").write_text(json.dumps(dict(scope="EXACT_OLD_IMPORTER_NATIVE_JDBC_NEGATIVE_CONTROL_NOT_DEVICE_OR_QUALITY", baseline=BASE, sourceSha256=hashlib.sha256(old_source.read_bytes()).hexdigest(), records=records), indent=2) + "\n")
