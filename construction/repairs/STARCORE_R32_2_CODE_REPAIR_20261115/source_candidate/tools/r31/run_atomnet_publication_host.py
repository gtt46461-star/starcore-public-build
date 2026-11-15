#!/usr/bin/env python3
"""Run the original AtomNet native/JUnit contracts against an actual whole-app jar.

Usage:
  python3 tools/r31/run_atomnet_publication_host.py --classes ACTUAL_WHOLE_JAR --out NEW_DIRECTORY

Requires JAVA_HOME, ANDROID_HOME (or ANDROID_SDK_ROOT), and SQLITE_JDBC. JUnit and
Hamcrest come from JUNIT_JAR/HAMCREST_JAR or the normal GRADLE_USER_HOME cache.
Only HOST Android boundaries and original tests are compiled. Business classes,
including DbHelper/DbStore, come from --classes; no production source overrides.
The original HOST fixture's obsolete DbHelper(db) convenience call is adapted in
an output-only copy to actual DbHelper(Context), with an explicitly bound native
SQLiteOpenHelper boundary. Every original assertion remains byte-for-byte intact.
These are HOST/native SQLite results, never Android/device/soak/quality results.
"""
from __future__ import annotations

import argparse
import ast
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
import zipfile

ROOT = Path(__file__).resolve().parents[2]
FIXTURES = ROOT / "verification/r31/p01/atomnet-publication"
PACKAGE = "com.ethk3.novaonepro.strategyv2."
HOST_NAMES = ["AtomNativeFaultsR31", "AtomPublicationNativeHost", "AtomPublicationConsumerHost",
              "AtomPublicationProcessHost", "MaterializedCursorNativeHost", "AtomRuntimeProducerHost"]
JUNIT_NAMES = ["R29ModelArtifactTest", "R30ResumeTrainingTest", "R30TenPositionTrainingTest",
               "RepairNeuralR30Test", "R30NumberRankerTest", "RepairModelReplayR30Test", "R30ModelSurfaceTest"]
BUSINESS_NAMES = ["com.ethk3.novaonepro." + n for n in ("DbHelper", "DbStore", "DatabaseCoordinator")]
BUSINESS_NAMES += [PACKAGE + n for n in (
    "AtomNetPublicationR31", "AtomNetStoreR30", "AtomNetSessionR30", "AtomNetRuntimeR30", "ModelArtifactR29",
    "ModelSurfaceStoreR30", "ModelSurfaceProviderR30", "CheckpointArtifactsR26", "CheckpointReferencesR26",
    "ModelReplayStudyR30R1", "CapabilityRegistryStoreR30", "ResearchProvenanceGraphR30R1", "RuntimeR27",
    "RuntimeCoordinatorR26", "ResearchPoolR27", "ResearchWriteQueueR27", "MemoryBudgetR31", "FeatureCacheR27", "PolicyAstR27")]


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def java_tool(name: str) -> Path:
    home = os.environ.get("JAVA_HOME")
    if not home:
        raise ValueError("JAVA_HOME must identify the JDK used for this run")
    path = Path(home) / "bin" / name
    if not path.is_file():
        raise FileNotFoundError(f"{name} missing under JAVA_HOME: {path}")
    return path.resolve()


def dependency_jar(env_name: str, cache_pattern: str) -> Path:
    if os.environ.get(env_name):
        path = Path(os.environ[env_name]).expanduser()
    else:
        gradle = Path(os.environ.get("GRADLE_USER_HOME", str(Path.home() / ".gradle")))
        matches = sorted(gradle.glob(cache_pattern))
        if not matches:
            raise FileNotFoundError(f"set {env_name}, or populate the JUnit/Hamcrest Gradle cache")
        path = matches[0]
    if not path.is_file():
        raise FileNotFoundError(f"{env_name} jar missing: {path}")
    return path.resolve()


def platform_sources() -> dict[str, str]:
    # Read literal boundary definitions without importing/executing the old runner.
    tree = ast.parse((ROOT / "tools/r31/run_task_runtime_host.py").read_text())
    definitions = next(ast.literal_eval(n.value) for n in ast.walk(tree)
                       if isinstance(n, ast.Assign)
                       and any(isinstance(t, ast.Name) and t.id == "adapters" for t in n.targets))
    definitions = {name: value for name, value in definitions.items() if name.startswith("android/")}
    definitions.update({
        "android/content/Context.java": '''package android.content;public class Context{private final java.io.File database;public Context(java.io.File database){this.database=database;}public java.io.File getDatabasePath(String name){return database;}public java.io.File getFilesDir(){java.io.File f=new java.io.File(database.getParentFile().getParentFile(),"files");f.mkdirs();return f;}public android.content.res.AssetManager getAssets(){return new android.content.res.AssetManager();}public Context getApplicationContext(){return this;}}''',
        "android/app/Application.java": '''package android.app;public class Application extends android.content.Context{public Application(java.io.File database){super(database);}}''',
        "android/content/res/AssetManager.java": '''package android.content.res;public class AssetManager{public java.io.InputStream open(String path)throws java.io.IOException{return new java.io.FileInputStream(new java.io.File(System.getProperty("r31.assets"),path));}}''',
        "android/os/SystemClock.java": '''package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000L;}public static long uptimeMillis(){return elapsedRealtime();}public static long currentThreadTimeMillis(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()/1000000L;}}''',
        "android/os/Build.java": '''package android.os;public final class Build{public static final class VERSION{public static final int SDK_INT=0;}}''',
        "android/database/sqlite/SQLiteOpenHelper.java": '''package android.database.sqlite;public abstract class SQLiteOpenHelper{private static final java.util.Map<String,SQLiteDatabase> connections=new java.util.concurrent.ConcurrentHashMap<>();private final SQLiteDatabase database;public static void bindExistingForHostR31(java.io.File path,SQLiteDatabase db){SQLiteDatabase previous=connections.putIfAbsent(path.getAbsolutePath(),db);if(previous!=null&&previous!=db)throw new IllegalStateException("HOST_CONNECTION_ALREADY_BOUND");}public SQLiteOpenHelper(android.content.Context context,String name,SQLiteDatabase.CursorFactory factory,int version){database=connections.get(context.getDatabasePath(name).getAbsolutePath());if(database==null)throw new IllegalStateException("HOST_EXPLICIT_NATIVE_CONNECTION_REQUIRED");}public SQLiteDatabase getReadableDatabase(){return database;}public SQLiteDatabase getWritableDatabase(){return database;}public void setWriteAheadLoggingEnabled(boolean enabled){}public void close(){database.close();}public abstract void onCreate(SQLiteDatabase db);public abstract void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion);public void onConfigure(SQLiteDatabase db){}public void onOpen(SQLiteDatabase db){}public void onDowngrade(SQLiteDatabase db,int oldVersion,int newVersion){throw new UnsupportedOperationException("HOST_DOWNGRADE_NOT_TESTED");}}''',
        "android/system/ErrnoException.java": '''package android.system;public class ErrnoException extends Exception{public ErrnoException(String op,int code){super(op+":"+code);}}''',
        "android/system/OsConstants.java": '''package android.system;public final class OsConstants{public static final int O_RDONLY=0;}''',
        "android/system/Os.java": '''package android.system;public final class Os{private static final java.util.Map<java.io.FileDescriptor,java.nio.channels.FileChannel> files=new java.util.IdentityHashMap<>();public static synchronized java.io.FileDescriptor open(String path,int flags,int mode)throws ErrnoException{try{java.io.FileDescriptor d=new java.io.FileDescriptor();files.put(d,java.nio.channels.FileChannel.open(java.nio.file.Paths.get(path),java.nio.file.StandardOpenOption.READ));return d;}catch(java.io.IOException e){throw new ErrnoException(e.toString(),1);}}public static synchronized void fsync(java.io.FileDescriptor d)throws ErrnoException{try{files.get(d).force(true);}catch(java.io.IOException e){throw new ErrnoException(e.toString(),1);}}public static synchronized void close(java.io.FileDescriptor d)throws ErrnoException{try{java.nio.channels.FileChannel c=files.remove(d);if(c!=null)c.close();}catch(java.io.IOException e){throw new ErrnoException(e.toString(),1);}}}''',
    })
    for path in sorted((ROOT / "tools/r29/qual_host/shim").rglob("*.java")):
        name = path.relative_to(ROOT / "tools/r29/qual_host/shim").as_posix()
        content = path.read_text()
        if path.name == "SQLiteDatabase.java":
            content = content.replace("public class SQLiteDatabase {", "public class SQLiteDatabase implements java.lang.AutoCloseable {")
            at = content.index("    static final class Rows")
            content = content[:at] + '''public static final int OPEN_READONLY=1,NO_LOCALIZED_COLLATORS=16;public interface CursorFactory{}private boolean successful;public String getPath(){try{return jdbc.getMetaData().getURL().substring("jdbc:sqlite:".length());}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}public static SQLiteDatabase openDatabase(String path,CursorFactory factory,int flags){try{return new SQLiteDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite:"+path));}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}public void beginTransaction(){try{jdbc.setAutoCommit(false);successful=false;}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}public void setTransactionSuccessful(){successful=true;}public void endTransaction(){try{if(successful)jdbc.commit();else jdbc.rollback();}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}finally{successful=false;try{jdbc.setAutoCommit(true);}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}}public void close(){try{jdbc.close();}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}''' + content[at:]
        definitions[name] = content
    if any(not name.startswith("android/") for name in definitions):
        raise ValueError("HOST platform must never override application business classes")
    return definitions


def run(args: argparse.Namespace) -> int:
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=False)
    receipt: dict = {
        "status": "IN_PROGRESS", "scope": "ACTUAL_WHOLE_JAR_NATIVE_SQLITE_HOST_ORIGINAL_CONTRACTS_NOT_ANDROID",
        "startedAtUtc": datetime.now(timezone.utc).isoformat(), "commands": [], "productionSourceOverrides": [],
        "device": "NOT_RUN", "soak24h": "NOT_RUN", "soak72h": "NOT_RUN", "naturalQuality": "NOT_EVALUATED",
        "explicitGc": False, "heapArgument": "-Xmx512m", "hostFrameworkApiLevel": "NOT_ANDROID",
    }
    def save():
        (out / "RECEIPT.json").write_text(json.dumps(receipt, indent=2, ensure_ascii=False) + "\n")
    def command(argv, log, expected=0, timeout=240):
        argv = list(map(str, argv))
        started = time.monotonic_ns()
        print("ATOMNET_HOST_STAGE", log, flush=True)
        try:
            result = subprocess.run(argv, cwd=ROOT, text=True, capture_output=True, timeout=timeout)
        except subprocess.TimeoutExpired as expired:
            def decode(value):return value.decode(errors="replace") if isinstance(value, bytes) else value or ""
            (out / log).write_text(decode(expired.stdout) + decode(expired.stderr))
            receipt["commands"].append({"argv": argv, "log": log, "timeoutSeconds": timeout, "timedOut": True})
            save()
            raise
        (out / log).write_text(result.stdout + result.stderr)
        receipt["commands"].append({"argv": argv, "log": log, "exitCode": result.returncode,
                                    "expectedExitCode": expected, "elapsedNanos": time.monotonic_ns() - started})
        save()
        if result.returncode != expected:
            raise RuntimeError(f"{log}: exit {result.returncode}, expected {expected}: {(result.stdout + result.stderr)[-2400:]}")
        return result.stdout
    try:
        java, javac = java_tool("java"), java_tool("javac")
        sdk_home = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
        if not sdk_home:
            raise ValueError("ANDROID_HOME or ANDROID_SDK_ROOT must identify the Android SDK")
        sdk = Path(sdk_home) / "platforms/android-36/android.jar"
        jdbc_env = os.environ.get("SQLITE_JDBC")
        if not jdbc_env:
            raise ValueError("SQLITE_JDBC must identify the real sqlite-jdbc jar")
        jdbc = Path(jdbc_env).expanduser().resolve()
        junit = dependency_jar("JUNIT_JAR", "caches/modules-2/files-2.1/junit/junit/4.13.2/*/*.jar")
        hamcrest = dependency_jar("HAMCREST_JAR", "caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/*/*.jar")
        artifact = args.classes.expanduser().resolve()
        for path in [sdk, jdbc, artifact]:
            if not path.is_file():raise FileNotFoundError(path)
        with zipfile.ZipFile(artifact) as jar:
            entries = {name: hashlib.sha256(jar.read(name.replace(".", "/") + ".class")).hexdigest() for name in BUSINESS_NAMES}
            class_count = sum(n.endswith(".class") for n in jar.namelist())
        support, platform, tests, sources = [out / name for name in ("support", "platform", "tests", "sources")]
        for path in [support, platform, tests, sources]:path.mkdir()
        dependencies = {"actualWholeJar": artifact, "androidSdk": sdk, "sqliteJdbc": jdbc, "junit": junit, "hamcrest": hamcrest}
        receipt["dependencyInputs"] = {name: {"path": str(path), "sha256": sha(path)} for name, path in dependencies.items()}
        receipt["actualWholeClassCount"] = class_count
        receipt["businessClassEntryHashes"] = entries
        artifact_copy = support / "actual-whole.jar"
        shutil.copy2(artifact, artifact_copy)
        command([java, "-version"], "java-version.log")
        receipt["jdk"] = {"java": str(java), "javac": str(javac), "version": (out / "java-version.log").read_text()}
        input_paths = [Path(__file__).resolve(), ROOT / "tools/r31/run_task_runtime_host.py", FIXTURES / "FINAL_NATIVE_RECEIPT.json"]
        input_paths += sorted((ROOT / "tools/r29/qual_host/shim").rglob("*.java"))
        input_paths += [FIXTURES / (name + ".java") for name in HOST_NAMES]
        test_root = ROOT / "app/src/test/java/com/ethk3/novaonepro/strategyv2"
        original_tests = [test_root / (name + ".java") for name in JUNIT_NAMES]
        input_paths += original_tests + [test_root / "R27TestSupport.java"]
        input_paths += [ROOT / "research_data/ethpk10_draws_20260316_201059.csv"]
        input_paths += sorted(p for d in [ROOT / "app/src/main/assets", ROOT / "app/src/test/resources"] for p in d.rglob("*") if p.is_file())
        before = {str(path.relative_to(ROOT)): sha(path) for path in input_paths}
        receipt["repositoryInputsBefore"] = before
        original_manifest = json.loads((FIXTURES / "FINAL_NATIVE_RECEIPT.json").read_text())
        declared_original = {Path(name).name: value for name, value in original_manifest["originalTestSha256"].items()}
        for path in original_tests:
            if sha(path) != declared_original[path.name]:
                raise ValueError(f"original numerical assertions changed: {path.relative_to(ROOT)}")
        receipt["originalTestSha256"] = {str(path.relative_to(ROOT)): sha(path) for path in original_tests}
        platforms = []
        for name, content in platform_sources().items():
            path = sources / "platform" / name
            path.parent.mkdir(parents=True, exist_ok=True);path.write_text(content);platforms.append(path)
        receipt["platformSourceHashes"] = {str(p.relative_to(out)): sha(p) for p in platforms}
        compile_args = [javac, "-sourcepath", "", "-implicit:none", "-encoding", "UTF-8"]
        command([*compile_args, "-cp", os.pathsep.join(map(str, [artifact_copy, sdk])), "-d", platform, *platforms], "platform-compile.log")
        if (platform / "com").exists() and any((platform / "com").rglob("*.class")):
            raise AssertionError("platform output contains a business-class override")
        host_paths = []
        old = 'set(coordinator,"helper",new DbHelper(db));'
        new = 'android.database.sqlite.SQLiteOpenHelper.bindExistingForHostR31(path,db);set(coordinator,"helper",new DbHelper(new android.app.Application(path)));'
        for name in HOST_NAMES:
            original = FIXTURES / (name + ".java")
            content = original.read_text()
            if name == "AtomPublicationNativeHost":
                if content.count(old) != 1:raise ValueError("unsupported original fixture helper seam")
                content = content.replace(old, new, 1)
                receipt["hostConnectionAdaptation"] = {"source": str(original.relative_to(ROOT)), "originalSha256": sha(original),
                                                         "old": old, "new": new, "assertionsChanged": False}
            path = sources / "host" / original.name
            path.parent.mkdir(parents=True, exist_ok=True);path.write_text(content);host_paths.append(path)
        identity = sources / "host/AtomWholeArtifactIdentityHost.java"
        identity.write_text('''package com.ethk3.novaonepro.strategyv2;public final class AtomWholeArtifactIdentityHost{public static void main(String[] args)throws Exception{java.nio.file.Path wanted=java.nio.file.Paths.get(args[0]).toRealPath();java.util.Map<String,Object> entries=new java.util.LinkedHashMap<>();for(int i=1;i<args.length;i++){Class<?> c=Class.forName(args[i],false,AtomWholeArtifactIdentityHost.class.getClassLoader());java.nio.file.Path actual=java.nio.file.Paths.get(c.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();if(!wanted.equals(actual))throw new AssertionError("BUSINESS_CLASS_NOT_FROM_ACTUAL_ARTIFACT:"+c.getName()+":"+actual);entries.put(c.getName(),actual.toString());}java.util.Map<String,Object> out=new java.util.LinkedHashMap<>();out.put("pid",ProcessHandle.current().pid());out.put("loadedBusinessClasses",entries);out.put("status","ACTUAL_WHOLE_JAR_CLASS_SOURCES_CONFIRMED");System.out.println(JsonR27.write(out));}}''')
        host_paths.append(identity)
        receipt["effectiveHostSourceHashes"] = {str(path.relative_to(out)): sha(path) for path in host_paths}
        cp = os.pathsep.join(map(str, [platform, tests, artifact_copy, jdbc, sdk, ROOT / "app/src/test/resources"]))
        command([*compile_args, "-cp", cp, "-d", tests, test_root / "R27TestSupport.java", *host_paths], "host-compile.log")
        java_cmd = [java, "-Xmx512m", "-Dr31.assets=" + str(ROOT / "app/src/main/assets"), "-cp", cp]
        identity_result = json.loads(command([*java_cmd, PACKAGE + "AtomWholeArtifactIdentityHost", artifact_copy, *BUSINESS_NAMES], "artifact-identity.log"))
        receipt["artifactIdentity"] = identity_result
        producer = json.loads(command([*java_cmd, PACKAGE + "AtomRuntimeProducerHost", out / "data/producer/databases/main.db"], "actual-runtime-producer.log"))
        (out / "actual-runtime-producer.json").write_text(json.dumps(producer, indent=2) + "\n")
        cursor = json.loads(command([*java_cmd, PACKAGE + "MaterializedCursorNativeHost", out / "data/cursor/databases/main.db", "fixed"], "cursor-fixed.log"))
        if not cursor["rejected"] or cursor["actualStoredMaterialized"] != 0 or cursor["actualStoredSamples"] != 0:
            raise AssertionError("lost sample operations were not atomically refused")
        (out / "cursor-fixed.json").write_text(json.dumps(cursor, indent=2) + "\n")
        consumer = json.loads(command([*java_cmd, PACKAGE + "AtomPublicationConsumerHost", out / "data/consumer/databases/main.db"], "native-consumer.log"))
        if consumer["assertions"] < 543:raise AssertionError("the original consumer assertion coverage was reduced")
        (out / "native-consumer.json").write_text(json.dumps(consumer, indent=2) + "\n")
        processes = {}
        for branch, halt, code in [("before", "halt-before-final", 71), ("after", "halt-after-final", 73)]:
            db = out / ("data/process-" + branch) / "databases/main.db"
            event = json.loads(command([*java_cmd, PACKAGE + "AtomPublicationProcessHost", db, halt], "process-" + branch + "-halt.log", code))
            recovery = json.loads(command([*java_cmd, PACKAGE + "AtomPublicationProcessHost", db, "recover-" + branch], "process-" + branch + "-recovery.log"))
            if event["pid"] == recovery["pid"]:raise AssertionError("recovery did not execute in a fresh JVM")
            processes[branch] = {"haltExit": code, "haltEvent": event, "recovery": recovery}
        (out / "CROSS_JVM.json").write_text(json.dumps(processes, indent=2) + "\n")
        junit_cp = cp + os.pathsep + str(junit) + os.pathsep + str(hamcrest)
        command([*compile_args, "-cp", junit_cp, "-d", tests, *original_tests], "original-junit-compile.log")
        unit = command([java, "-Xmx512m", "-cp", junit_cp, "org.junit.runner.JUnitCore", *[PACKAGE + name for name in JUNIT_NAMES]], "original-junit.log")
        found = re.search(r"OK \((\d+) tests\)", unit)
        if not found or int(found.group(1)) != 29:raise AssertionError("all 29 original JUnit tests must pass in one JVM")
        after = {str(path.relative_to(ROOT)): sha(path) for path in input_paths}
        if before != after:raise AssertionError("repository test/boundary/resource inputs changed during the run")
        if sha(artifact) != sha(artifact_copy):raise AssertionError("actual whole artifact changed during the run")
        receipt.update({"status": "ACTUAL_WHOLE_JAR_NATIVE_CONTRACTS_PASS", "originalJUnit": 29,
                        "consumerAssertions": consumer["assertions"], "actualRuntimeProducer": producer, "cursorFault": cursor,
                        "fullOriginalSamplePlan": [800, 400, 160], "crossJvm": processes, "repositoryInputsAfter": after,
                        "repositoryInputsStable": True, "actualWholeJarStable": True, "businessClassOverrides": [],
                        "finishedAtUtc": datetime.now(timezone.utc).isoformat()})
        save()
        print(json.dumps({"status": receipt["status"], "consumerAssertions": consumer["assertions"], "originalJUnit": 29,
                          "crossJvm": True, "receipt": str(out / "RECEIPT.json")}))
        return 0
    except Exception as failed:
        receipt.update({"status": "FAILED_OR_BLOCKED", "failureType": type(failed).__name__, "failure": str(failed),
                        "finishedAtUtc": datetime.now(timezone.utc).isoformat()})
        save()
        print(f"AtomNet HOST run failed; raw evidence retained at {out}: {failed}", file=sys.stderr)
        return 1


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--classes", type=Path, required=True, help="Actual immutable whole Android compiled classes jar")
    parser.add_argument("--out", type=Path, required=True, help="New output directory; existing evidence is never overwritten")
    return run(parser.parse_args())


if __name__ == "__main__":
    sys.exit(main())
