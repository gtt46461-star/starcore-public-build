"""Actual production REAL/OOS routines through native SQLite; never an Android/quality receipt."""
import ast
import hashlib
import json
import os
import pathlib
import re
import subprocess
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[4]
MAIN = ROOT / "app/src/main/java/com/ethk3/novaonepro"
SDK = pathlib.Path(os.environ["ANDROID_HOME"]) / "platforms/android-36/android.jar"
CLASSES = ROOT / "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes"
RJAR = ROOT / "app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/debug/processDebugResources/R.jar"
JDBC = pathlib.Path("/tmp/r31-baseline-deps/sqlite-jdbc.jar")
SOURCES = [MAIN / name for name in ["SettlementEngine.java", "OosAfterDrawWriterR36.java", "AppConfig.java", "DecisionGateEngine.java", "DrawHashR36.java", "DbHelper.java"]]
before = {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in SOURCES}
for name, digest in before.items():
    print("R31_H15_NATIVE_SOURCE_SHA256", name, digest, flush=True)


def run(args):
    subprocess.run(list(map(str, args)), cwd=ROOT, check=True)


writer = ast.parse((ROOT / "tools/r31/run_write_protocol_host.py").read_text())
adapters = next(ast.literal_eval(node.value) for node in ast.walk(writer) if isinstance(node, ast.Assign) and any(isinstance(t, ast.Name) and t.id == "adapters" for t in node.targets))
transactions = next(node.value for node in ast.walk(writer) if isinstance(node, ast.Constant) and isinstance(node.value, str) and "public void beginTransaction()" in node.value and "OPEN_READONLY" in node.value)
service = ast.parse((ROOT / "tools/r31/run_service_protocol_host.py").read_text())
dml = next(ast.literal_eval(node.value) for node in ast.walk(service) if isinstance(node, ast.Assign) and any(isinstance(t, ast.Name) and t.id == "dml" for t in node.targets))
with tempfile.TemporaryDirectory(prefix="r31-h15-native-") as temporary:
    tmp = pathlib.Path(temporary)
    src, platform, actual, tests = [tmp / name for name in ["src", "platform", "actual", "tests"]]
    for directory in [src, platform, actual, tests]:
        directory.mkdir()
    files = []
    for source in sorted((ROOT / "tools/r29/qual_host/shim").rglob("*.java")):
        target = src / source.relative_to(ROOT / "tools/r29/qual_host/shim")
        target.parent.mkdir(parents=True, exist_ok=True)
        content = source.read_text()
        if source.name == "SQLiteDatabase.java":
            at = content.index("    static final class Rows")
            content = content[:at] + transactions + dml + content[at:]
        target.write_text(content)
        files.append(target)
    constants = "\n".join("public static final String " + name + "=" + json.dumps(value) + ";" for name, value in re.findall(r'public\s+static\s+final\s+String\s+(\w+)\s*=\s*"([^"\\]*)"\s*;', (MAIN / "DbHelper.java").read_text()))
    adapters["com/ethk3/novaonepro/DbHelper.java"] = "package com.ethk3.novaonepro;import android.database.sqlite.SQLiteDatabase;public final class DbHelper{" + constants + "private final SQLiteDatabase db;public DbHelper(SQLiteDatabase db){this.db=db;}public SQLiteDatabase getReadableDatabase(){return db;}public SQLiteDatabase getWritableDatabase(){return db;}public void close(){}}"
    adapters["android/content/ContentValues.java"] = "package android.content;public class ContentValues{public final java.util.Map<String,Object> data=new java.util.LinkedHashMap<>();" + "".join("public void put(String key," + kind + " value){data.put(key,value);}" for kind in ["String", "Integer", "Long", "Double", "Float", "Boolean", "byte[]"]) + "public void putNull(String key){data.put(key,null);}}"
    for name, content in adapters.items():
        target = src / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content)
        files.append(target)
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", SDK, "-d", platform, *files])
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, [SDK, CLASSES, RJAR])), "-d", actual, MAIN / "SettlementEngine.java", MAIN / "OosAfterDrawWriterR36.java"])
    cp = os.pathsep.join(map(str, [platform, actual, tests, CLASSES, JDBC, SDK]))
    run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", cp, "-d", tests, pathlib.Path(__file__).with_name("R31SettlementRedLineHost.java")])
    run(["java", "-Xmx512m", "-cp", cp, "com.ethk3.novaonepro.R31SettlementRedLineHost", tmp / "settlement.db"])
for name, digest in before.items():
    if hashlib.sha256((ROOT / name).read_bytes()).hexdigest() != digest:
        raise RuntimeError("Production source changed during the test: " + name)
print("R31_H15_NATIVE_SOURCE_STABLE_PASS_NOT_FULL_COORDINATOR_NOT_ANDROID_NOT_QUALITY")
