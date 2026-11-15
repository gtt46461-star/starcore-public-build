#!/usr/bin/env python3
"""42 real-report boundary + 18 native SQLite source-reader assertions.

Requires whole-app compiled dependency classes, never substitutes business code.
Fixture trial outcomes are NOT executed factory studies, natural quality or REAL
qualification. Generated files are only written under the new --out directory.
"""
from __future__ import annotations
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys

HERE = Path(__file__).resolve().parent
BASE = Path("app/src/main/java/com/ethk3/novaonepro/strategyv2")
PACKAGE = "com.ethk3.novaonepro.strategyv2."
PRODUCTION = ["MetricEvaluatorR29", "MetricDefinitionsR29", "RepairDiagnosisR32",
              "ReportSnapshotR30", "Q08ReportProjectionR325", "Q08EvidenceReaderR325"]


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def process(command: list[str], logfile: Path, timeout: int) -> dict:
    try:
        r = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                           text=True, encoding="utf-8", errors="replace",
                           timeout=timeout, check=False)
        code, output = r.returncode, r.stdout
    except subprocess.TimeoutExpired as e:
        code, output = 124, e.stdout or ""
        if isinstance(output, bytes):
            output = output.decode("utf-8", "replace")
        output += "\nTIMEOUT\n"
    logfile.write_text(output, encoding="utf-8")
    return {"command": command, "exitCode": code, "log": str(logfile),
            "logSha256": sha(output.encode("utf-8")), "output": output}


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--repo", required=True, type=Path)
    p.add_argument("--overlay", type=Path)
    p.add_argument("--dependency-classes", required=True, type=Path)
    p.add_argument("--android-jar", required=True, type=Path)
    p.add_argument("--sqlite-jdbc", required=True, type=Path)
    p.add_argument("--slf4j-jar", required=True, type=Path)
    p.add_argument("--out", required=True, type=Path)
    p.add_argument("--baseline-only", action="store_true",
                   help="Compile original report sources and run the same 42 consumer assertions; failures are expected evidence.")
    p.add_argument("--java", default="java")
    p.add_argument("--timeout-seconds", type=int, default=60)
    a = p.parse_args()
    java = shutil.which(a.java)
    if java is None:
        p.error("--java executable is unavailable")
    repo, out, dep = a.repo.resolve(), a.out.resolve(), a.dependency_classes.resolve()
    overlay = a.overlay.resolve() if a.overlay else None
    if out.exists() and (not out.is_dir() or any(out.iterdir())):
        p.error("--out must be new or empty; existing evidence is never deleted")
    if not dep.is_dir() or not (repo / BASE).is_dir():
        p.error("source root or whole-app dependency classes are absent")
    jars = [a.android_jar.resolve(), a.sqlite_jdbc.resolve(), a.slf4j_jar.resolve()]
    if any(not f.is_file() for f in jars):
        p.error("a required jar is absent")
    if a.timeout_seconds < 1 or a.timeout_seconds > 60:
        p.error("--timeout-seconds must be in 1..60")
    out.mkdir(parents=True, exist_ok=True)
    classes, hostclasses = out / "classes", out / "hostclasses"
    classes.mkdir();hostclasses.mkdir()
    receipt = {
        "format": "R32_5_Q08_REPORT_SOURCE_BOUNDARY_1",
        "scope": "ACTUAL_PRODUCTION_REPORT_CONSUMERS_AND_NATIVE_SQLITE_READS; FIXTURE_TRIAL_OUTCOMES_ONLY; NOT_120_EXECUTED_RESEARCH_EXPERIMENTS",
        "naturalQuality": "NOT_PROVEN", "realAuthorizationGranted": False,
        "baselineOnly": a.baseline_only, "businessStubs": [],
        "expectedAssertions": 42 if a.baseline_only else 60,
        "sourceSha256": {}, "dependencyClassSha256": {},
        "inputsUnmodifiedByRunner": True, "suites": [],
        "startedAtUtc": datetime.now(timezone.utc).isoformat(),
    }
    for f in sorted(dep.rglob("*.class")):
        receipt["dependencyClassSha256"][str(f.relative_to(dep))] = sha(f.read_bytes())
    receipt["dependencyClassTreeSha256"] = sha(json.dumps(receipt["dependencyClassSha256"], sort_keys=True, separators=(",", ":")).encode())
    receipt["jars"] = {str(f): sha(f.read_bytes()) for f in jars}
    receipt["javaVersion"] = process([java, "-version"], out / "java_version.log", a.timeout_seconds)
    selected, snapshots = [], []

    def snapshot(src: Path, relative: Path) -> str:
        data = src.read_bytes()
        dest = out / "sources" / relative
        dest.parent.mkdir(parents=True, exist_ok=True);dest.write_bytes(data)
        receipt["sourceSha256"][str(src)] = sha(data)
        snapshots.append((src, sha(data)))
        return str(dest)

    for name in PRODUCTION:
        if a.baseline_only and name.startswith("Q08"):
            continue
        relative = BASE / (name + ".java")
        src = overlay / relative if overlay and (overlay / relative).is_file() else repo / relative
        selected.append(snapshot(src, relative))
    test = snapshot(HERE / "Q08ReportBoundaryProbe.java", Path("tests/Q08ReportBoundaryProbe.java"))
    cp = str(dep) + ":" + str(jars[0])
    compile_command = [java, "-m", "jdk.compiler/com.sun.tools.javac.Main", "-source", "17", "-target", "17",
                       "-encoding", "UTF-8", "-proc:none", "-cp", cp, "-d", str(classes)] + selected + [test]
    compiled = process(compile_command, out / "compile.log", a.timeout_seconds)
    receipt["compile"] = compiled
    fullcp = str(classes) + ":" + cp

    def run_suite(name: str, main: str, classpath: str, expected: int) -> None:
        result = process([java, "-XX:-UsePerfData", "-Xmx256m", "-cp", classpath, PACKAGE + main], out / (name + ".log"), a.timeout_seconds)
        matches = re.findall(r"^RESULT checks=(\d+) failures=(\d+)$", result["output"], re.MULTILINE)
        observed, failed = (map(int, matches[0]) if len(matches) == 1 else (None, None))
        receipt["suites"].append({"name": name, "expected": expected, "observed": observed, "failures": failed,
            "passed": result["exitCode"] == 0 and observed == expected and failed == 0, "run": result})

    if compiled["exitCode"] == 0:
        run_suite("report_boundaries", "Q08ReportBoundaryProbe", fullcp, 42)
        if not a.baseline_only:
            platform = [snapshot(f, Path("host") / f.relative_to(HERE / "host"))
                        for f in sorted((HERE / "host").rglob("*.java"))]
            readtest = snapshot(HERE / "Q08OriginalReadProbe.java", Path("tests/Q08OriginalReadProbe.java"))
            receipt["hostPlatformAdapters"] = [str(f) for f in sorted((HERE / "host").rglob("*.java"))]
            hostcompile = process([java, "-m", "jdk.compiler/com.sun.tools.javac.Main", "-source", "17", "-target", "17",
                "-encoding", "UTF-8", "-proc:none", "-cp", fullcp, "-d", str(hostclasses)] + platform + [readtest],
                out / "host_compile.log", a.timeout_seconds)
            receipt["hostCompile"] = hostcompile
            if hostcompile["exitCode"] == 0:
                run_suite("native_original_reads", "Q08OriginalReadProbe", str(hostclasses) + ":" + fullcp + ":" + str(jars[1]) + ":" + str(jars[2]), 18)
    changed = [str(src) for src, digest in snapshots if not src.is_file() or sha(src.read_bytes()) != digest]
    receipt["inputChangesObserved"] = changed
    receipt["actualAssertions"] = sum(s["observed"] or 0 for s in receipt["suites"])
    receipt["failedAssertions"] = sum(s["failures"] or 0 for s in receipt["suites"])
    expected_suites = 1 if a.baseline_only else 2
    ok = (len(receipt["suites"]) == expected_suites and all(s["passed"] for s in receipt["suites"])
          and not changed and receipt["actualAssertions"] == receipt["expectedAssertions"] and receipt["javaVersion"]["exitCode"] == 0)
    receipt["status"] = "HOST_BOUNDARIES_PASSED" if ok else "HOST_BOUNDARIES_FAILED_OR_NOT_RUN"
    receipt["finishedAtUtc"] = datetime.now(timezone.utc).isoformat()
    receipt["runnerSha256"] = sha(Path(__file__).read_bytes())
    (out / "RESULTS.json").write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"receipt": str(out / "RESULTS.json"), "status": receipt["status"],
                      "assertions": receipt["actualAssertions"], "failures": receipt["failedAssertions"],
                      "naturalQuality": "NOT_PROVEN"}, ensure_ascii=False))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
