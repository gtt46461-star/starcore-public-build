#!/usr/bin/env python3
"""Run Batch-3/4 manifest and diagnostic contracts against real host SQLite.

The ordinary Java host gate compiles production against android.jar. Database contract harnesses
need executable Android adapters, so this script overlays only test-side ContentValues,
SQLiteDatabase and DbStore/DatabaseCoordinator classes. Production sources remain unchanged.
"""
from __future__ import annotations

import os
import pathlib
import shutil
import subprocess
import sys
from typing import Iterable, List

ROOT = pathlib.Path(__file__).resolve().parents[1]
TOOLCHAIN = pathlib.Path(os.environ.get("STARCORE_TOOLCHAIN_ROOT", ROOT.parent / "08_TOOLCHAIN"))
ANDROID = pathlib.Path(os.environ.get(
    "ANDROID_PLATFORM_JAR",
    TOOLCHAIN / "android-platform-36" / "android.jar",
))
HOST = ROOT / "build" / "r925-host" / "compiled-classes"
HOST_SUMMARY = ROOT / "build" / "r925-host" / "R925_COMPILE_HOST_SUMMARY.txt"
OUT = ROOT / "build" / "r930-batch34-sql"
EVIDENCE = ROOT / "build" / "r925-host"
BRIDGE = ROOT / "verification" / "r925" / "sqlite_bridge.py"
SQLITE_STUB = ROOT / "verification" / "r925" / "stubs" / "android" / "database" / "sqlite" / "SQLiteDatabase.java"
DBSTORE_STUB = ROOT / "verification" / "r925" / "stubs" / "com" / "ethk3" / "novaonepro" / "DbStore.java"
COORDINATOR_STUB = ROOT / "verification" / "r925" / "stubs" / "com" / "ethk3" / "novaonepro" / "DatabaseCoordinator.java"
MANIFEST_HARNESS = ROOT / "verification" / "r930" / "Batch34ManifestHarnessR930.java"
DIAGNOSTIC_HARNESS = ROOT / "verification" / "r930" / "Batch34DiagnosticCardsHarnessR930.java"


def fail(code: str, detail: str = "") -> "None":
    suffix = f"|{detail}" if detail else ""
    raise SystemExit(f"R930_BATCH34_SQL_FAIL|{code}{suffix}")


def require_file(path: pathlib.Path, code: str) -> None:
    if not path.is_file():
        fail(code, str(path.relative_to(ROOT) if path.is_relative_to(ROOT) else path))


def write(path: pathlib.Path, content: str) -> pathlib.Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    return path


def run(command: List[str], log: pathlib.Path) -> None:
    process = subprocess.run(
        command,
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        check=False,
    )
    log.parent.mkdir(parents=True, exist_ok=True)
    log.write_text(process.stdout, encoding="utf-8")
    if process.returncode != 0:
        sys.stderr.write(process.stdout)
        fail("COMMAND_FAILED", f"exit={process.returncode}|command={' '.join(command)}")


def require_marker(path: pathlib.Path, marker: str) -> None:
    require_file(path, "EVIDENCE_MISSING")
    text = path.read_text(encoding="utf-8")
    if marker not in text.splitlines() and marker not in text:
        sys.stderr.write(text)
        fail("MARKER_MISSING", f"{path.name}|{marker}")


def main() -> int:
    for path, code in (
        (ANDROID, "ANDROID_JAR_MISSING"),
        (HOST_SUMMARY, "HOST_SUMMARY_MISSING"),
        (BRIDGE, "SQLITE_BRIDGE_MISSING"),
        (SQLITE_STUB, "SQLITE_STUB_MISSING"),
        (DBSTORE_STUB, "DBSTORE_STUB_MISSING"),
        (COORDINATOR_STUB, "COORDINATOR_STUB_MISSING"),
        (MANIFEST_HARNESS, "MANIFEST_HARNESS_MISSING"),
        (DIAGNOSTIC_HARNESS, "DIAGNOSTIC_HARNESS_MISSING"),
    ):
        require_file(path, code)
    if not HOST.is_dir():
        fail("HOST_CLASSES_MISSING", str(HOST.relative_to(ROOT)))
    host_summary = HOST_SUMMARY.read_text(encoding="utf-8")
    if "R925_COMPILE_HOST=PASS" not in host_summary:
        fail("HOST_GATE_NOT_PASS")
    if "BATCH34_SQL=SEPARATE_REQUIRED" not in host_summary:
        fail("HOST_GATE_SEPARATION_MARKER_MISSING")

    shutil.rmtree(OUT, ignore_errors=True)
    OUT.mkdir(parents=True, exist_ok=True)
    EVIDENCE.mkdir(parents=True, exist_ok=True)

    content_values = write(
        OUT / "adapters" / "android" / "content" / "ContentValues.java",
        """package android.content;
import java.util.LinkedHashMap;
import java.util.Map;
public class ContentValues {
    private final LinkedHashMap<String,Object> values=new LinkedHashMap<String,Object>();
    public ContentValues(){}
    public ContentValues(int size){}
    public void put(String key,String value){values.put(key,value);}
    public void put(String key,Integer value){values.put(key,value);}
    public void put(String key,Long value){values.put(key,value);}
    public void put(String key,Double value){values.put(key,value);}
    public void put(String key,Float value){values.put(key,value);}
    public void put(String key,Boolean value){values.put(key,value);}
    public void put(String key,byte[] value){values.put(key,value);}
    public void putNull(String key){values.put(key,null);}
    public Object get(String key){return values.get(key);}
    public int size(){return values.size();}
    public java.util.Set<Map.Entry<String,Object>> valueSet(){return values.entrySet();}
}""",
    )
    cursor_wrapper = write(
        OUT / "adapters" / "android" / "database" / "CursorWrapper.java",
        """package android.database;
public class CursorWrapper implements Cursor {
    private final Cursor cursor;
    public CursorWrapper(Cursor cursor){this.cursor=cursor;}
    public Cursor getWrappedCursor(){return cursor;}
    public int getCount(){return cursor.getCount();}
    public int getPosition(){return cursor.getPosition();}
    public boolean move(int offset){return cursor.move(offset);}
    public boolean moveToPosition(int position){return cursor.moveToPosition(position);}
    public boolean moveToFirst(){return cursor.moveToFirst();}
    public boolean moveToLast(){return cursor.moveToLast();}
    public boolean moveToNext(){return cursor.moveToNext();}
    public boolean moveToPrevious(){return cursor.moveToPrevious();}
    public boolean isFirst(){return cursor.isFirst();}
    public boolean isLast(){return cursor.isLast();}
    public boolean isBeforeFirst(){return cursor.isBeforeFirst();}
    public boolean isAfterLast(){return cursor.isAfterLast();}
    public int getColumnIndex(String name){return cursor.getColumnIndex(name);}
    public int getColumnIndexOrThrow(String name){return cursor.getColumnIndexOrThrow(name);}
    public String getColumnName(int index){return cursor.getColumnName(index);}
    public String[] getColumnNames(){return cursor.getColumnNames();}
    public int getColumnCount(){return cursor.getColumnCount();}
    public byte[] getBlob(int column){return cursor.getBlob(column);}
    public String getString(int column){return cursor.getString(column);}
    public short getShort(int column){return cursor.getShort(column);}
    public int getInt(int column){return cursor.getInt(column);}
    public long getLong(int column){return cursor.getLong(column);}
    public float getFloat(int column){return cursor.getFloat(column);}
    public double getDouble(int column){return cursor.getDouble(column);}
    public int getType(int column){return cursor.getType(column);}
    public boolean isNull(int column){return cursor.isNull(column);}
    public void deactivate(){cursor.deactivate();}
    public boolean requery(){return cursor.requery();}
    public void close(){cursor.close();}
    public boolean isClosed(){return cursor.isClosed();}
    public void registerContentObserver(android.database.ContentObserver observer){cursor.registerContentObserver(observer);}
    public void unregisterContentObserver(android.database.ContentObserver observer){cursor.unregisterContentObserver(observer);}
    public void registerDataSetObserver(android.database.DataSetObserver observer){cursor.registerDataSetObserver(observer);}
    public void unregisterDataSetObserver(android.database.DataSetObserver observer){cursor.unregisterDataSetObserver(observer);}
    public void setNotificationUri(android.content.ContentResolver resolver,android.net.Uri uri){cursor.setNotificationUri(resolver,uri);}
    public android.net.Uri getNotificationUri(){return cursor.getNotificationUri();}
    public boolean getWantsAllOnMoveCalls(){return cursor.getWantsAllOnMoveCalls();}
    public void setExtras(android.os.Bundle extras){cursor.setExtras(extras);}
    public android.os.Bundle getExtras(){return cursor.getExtras();}
    public android.os.Bundle respond(android.os.Bundle extras){return cursor.respond(extras);}
    public void copyStringToBuffer(int column,android.database.CharArrayBuffer buffer){cursor.copyStringToBuffer(column,buffer);}
}""",
    )
    system_clock = write(
        OUT / "adapters" / "android" / "os" / "SystemClock.java",
        """package android.os;
public final class SystemClock {
    private SystemClock(){}
    public static long elapsedRealtime(){return java.lang.System.nanoTime()/1000000L;}
    public static long uptimeMillis(){return java.lang.System.nanoTime()/1000000L;}
}""",
    )
    debug = write(
        OUT / "adapters" / "android" / "os" / "Debug.java",
        """package android.os;
public final class Debug {
    private Debug(){}
    public static long getPss(){return Math.max(1L,Runtime.getRuntime().totalMemory()/1024L);}
    public static long getNativeHeapAllocatedSize(){return Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();}
    public static void getMemoryInfo(MemoryInfo info){}
    public static final class MemoryInfo { public int getTotalPss(){return (int)Math.min(Integer.MAX_VALUE,Debug.getPss());} }
}""",
    )

    compile_log = EVIDENCE / "R930_BATCH34_SQL_COMPILE.log"
    sources: Iterable[pathlib.Path] = (
        content_values,
        cursor_wrapper,
        system_clock,
        debug,
        SQLITE_STUB,
        DBSTORE_STUB,
        COORDINATOR_STUB,
        MANIFEST_HARNESS,
        DIAGNOSTIC_HARNESS,
    )
    classpath = os.pathsep.join((str(OUT), str(HOST), str(ANDROID)))
    run([
        "java", "-m", "jdk.compiler/com.sun.tools.javac.Main",
        "-encoding", "UTF-8", "-source", "17", "-target", "17",
        "-cp", classpath, "-d", str(OUT), *[str(path) for path in sources],
    ], compile_log)

    runtime_classpath = os.pathsep.join((str(OUT), str(HOST), str(ANDROID)))
    manifest_output = EVIDENCE / "Batch34ManifestHarnessR930.txt"
    diagnostic_output = EVIDENCE / "Batch34DiagnosticCardsHarnessR930.txt"
    run([
        "java", "-cp", runtime_classpath,
        "com.ethk3.novaonepro.Batch34ManifestHarnessR930", str(BRIDGE),
    ], manifest_output)
    run([
        "java", "-cp", runtime_classpath,
        "com.ethk3.novaonepro.Batch34DiagnosticCardsHarnessR930", str(BRIDGE),
    ], diagnostic_output)

    require_marker(manifest_output, "R930_BATCH34_MANIFEST_PASS")
    require_marker(manifest_output, "SUMMARY|batch3=12/12|batch4=10/10|tasks=22/22|")
    require_marker(manifest_output, "effect=UNPROVEN_FAIL_CLOSED")
    require_marker(diagnostic_output, "ENHANCEMENT|E05|PASS")
    require_marker(diagnostic_output, "R930_E05_DIAGNOSTIC_PASS")

    summary = (
        "R930_BATCH34_SQL=PASS"
        "|batch3=12/12|batch4=10/10|tasks=22/22"
        "|sqlite=REAL_PYTHON_BRIDGE"
        "|transactions=HOST_ADAPTER"
        "|effect=UNPROVEN_FAIL_CLOSED"
        "|e05=PASS\n"
    )
    (EVIDENCE / "R930_BATCH34_SQL_SUMMARY.txt").write_text(summary, encoding="utf-8")
    print(summary, end="")
    return 0


if __name__ == "__main__":
    sys.exit(main())
