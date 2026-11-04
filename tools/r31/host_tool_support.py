"""Portable, source-explicit HOST compilation and measured platform-boundary adapters."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess


def dependencies(root, require_resources=False):
    root = Path(root)
    classes = Path(os.environ.get("R31_HOST_CLASSES", root / "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes"))
    home = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    sdk = Path(os.environ["R31_HOST_ANDROID_JAR"]) if "R31_HOST_ANDROID_JAR" in os.environ else Path(home or "") / "platforms/android-36/android.jar"
    resources = Path(os.environ.get("R31_HOST_RJAR", root / "app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/debug/processDebugResources/R.jar"))
    jdbc = Path(os.environ.get("SQLITE_JDBC", "/tmp/r31-baseline-deps/sqlite-jdbc.jar"))
    for name, path in [("actual compiled application classes", classes), ("Android SDK", sdk), ("SQLite JDBC", jdbc)]:
        if not path.exists():
            raise FileNotFoundError(name + " required: " + str(path))
    if require_resources and not resources.is_file():
        raise FileNotFoundError("actual generated R.jar required; set R31_HOST_RJAR: " + str(resources))
    evidence = {}
    for name, path in [("classes", classes), ("androidSdk", sdk), ("generatedResources", resources), ("sqliteJdbc", jdbc)]:
        if path.is_file():
            evidence[name] = dict(path=str(path), sha256=hashlib.sha256(path.read_bytes()).hexdigest())
        elif path.is_dir():
            digest = hashlib.sha256()
            for child in sorted(path.rglob("*.class")):
                digest.update(str(child.relative_to(path)).encode()); digest.update(b"\0"); digest.update(hashlib.sha256(child.read_bytes()).digest())
            evidence[name] = dict(path=str(path), classTreeSha256=digest.hexdigest())
    print("R31_HOST_DEPENDENCY_PROVENANCE " + json.dumps(evidence, sort_keys=True), flush=True)
    return classes, sdk, resources, jdbc


def run(root, args):
    argv = list(map(str, args))
    if argv[0] in {"java", "javac"}:
        name = argv[0]
        candidate = Path(os.environ["JAVA_HOME"]) / "bin" / name if os.environ.get("JAVA_HOME") else None
        argv[0] = str(candidate) if candidate and candidate.is_file() else shutil.which(name) or name
        if name == "javac":
            # Explicit current producers compile against real existing class dependencies.
            # A fresh checkout must never silently recompile MainActivity or the whole app.
            argv[1:1] = ["-sourcepath", "", "-implicit:none"]
    subprocess.run(argv, cwd=root, check=True)


def pid_adapter(output):
    target = Path(output) / "Process.java"
    target.write_text('package android.os;public final class Process{public static int myPid(){long pid=java.lang.ProcessHandle.current().pid();if(pid<=0||pid>Integer.MAX_VALUE)throw new IllegalStateException("HOST_PID_OUT_OF_ANDROID_INT_RANGE");return(int)pid;}}\n')
    return target


def cursor_column_metadata(text):
    if re.search(r"\bint\s+getColumnCount\s*\(", text):
        return text
    anchor = "    int getCount();"
    if anchor not in text:
        raise ValueError("unsupported HOST Cursor metadata shape")
    return text.replace(anchor, anchor + "\n    int getColumnCount();", 1)


def database_column_metadata(text):
    if re.search(r"\bint\s+getColumnCount\s*\(", text):
        return text
    replacements = {
        "List<Object[]> rows = new ArrayList<>();": "List<Object[]> rows = new ArrayList<>();int columns=0;",
        "int n = r.getMetaData().getColumnCount();": "int n = r.getMetaData().getColumnCount();columns=n;",
        "return new Rows(rows);": "return new Rows(rows,columns);",
        "Rows(List<Object[]> rows) { this.rows = rows; }": "final int columns;Rows(List<Object[]> rows,int columns){this.rows=rows;this.columns=columns;} public int getColumnCount(){return columns;}",
    }
    for old, new in replacements.items():
        if old not in text:
            raise ValueError("unsupported HOST SQLite metadata shape: " + old)
        text = text.replace(old, new, 1)
    return text
