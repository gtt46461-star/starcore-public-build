#!/usr/bin/env python3
"""Run the 20 + 31 actual-production-class diagnostic boundary assertions.

No Android or natural-quality claim. All generated files are under --out.
The metric suite uses explicitly listed compile-only dependency declarations.
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

BASE = Path("app/src/main/java/com/ethk3/novaonepro/strategyv2")
PACKAGE = "com.ethk3.novaonepro.strategyv2."
HERE = Path(__file__).resolve().parent
STUBS = [
    "CertificateGateR27.java", "DiagnosisRulesR30.java", "IssueTimelineR30.java",
    "MetricCardMapR29.java", "ReportEvidenceProjectionR33.java", "ReportR27.java",
    "ReportR29.java",
]
SUITES = [
    {"name": "page", "main": "PageAuditBoundaryProbe", "expected": 20,
     "production": ["ResearchTableAuditR32.java", "StatsR27.java", "JsonR27.java"],
     "stubs": []},
    {"name": "assessment", "main": "AssessmentBoundaryProbe", "expected": 31,
     "production": ["ReportAssessmentR33.java", "CheckMetricCrosswalkR31.java",
                    "MetricDefinitionsR29.java", "MetricEvaluatorR29.java", "Q08ReportProjectionR325.java",
                    "StatsR27.java", "JsonR27.java"],
     "stubs": STUBS},
]


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def command_path(value: str) -> str:
    found = shutil.which(value)
    if found:
        return str(Path(found).resolve())
    path = Path(value).expanduser().resolve()
    if path.is_file():
        return str(path)
    raise ValueError("Executable not found: " + value)


def run_process(command: list[str], log: Path, cwd: Path, timeout: int) -> dict:
    began = datetime.now(timezone.utc).isoformat()
    try:
        result = subprocess.run(command, cwd=str(cwd), stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, text=True,
                                encoding="utf-8", errors="replace", timeout=timeout,
                                check=False)
        output, exit_code, timed_out = result.stdout, result.returncode, False
    except subprocess.TimeoutExpired as exc:
        output = exc.stdout or ""
        if isinstance(output, bytes):
            output = output.decode("utf-8", "replace")
        output += "\nRUNNER_TIMEOUT\n"
        exit_code, timed_out = 124, True
    except OSError as exc:
        output, exit_code, timed_out = repr(exc) + "\n", 127, False
    log.write_text(output, encoding="utf-8")
    return {"command": command, "startedAtUtc": began, "exitCode": exit_code,
            "timedOut": timed_out, "log": str(log),
            "logSha256": digest(output.encode("utf-8")), "output": output}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", required=True, type=Path,
                        help="Candidate repository root containing app/src/main/java")
    parser.add_argument("--out", required=True, type=Path,
                        help="New or empty output directory; existing evidence is never deleted")
    parser.add_argument("--overlay", type=Path,
                        help="Optional replacement root; its exact relative files override --repo")
    parser.add_argument("--java", default="java", help="Java executable or PATH command (JDK 17+)")
    parser.add_argument("--javac", help="Optional compiler executable; default uses java jdk.compiler module")
    parser.add_argument("--timeout-seconds", default=60, type=int,
                        help="Per compiler/test process timeout, default 60 seconds")
    args = parser.parse_args()
    repo = args.repo.expanduser().resolve()
    out = args.out.expanduser().resolve()
    overlay = args.overlay.expanduser().resolve() if args.overlay else None
    if not (repo / BASE).is_dir():
        parser.error("--repo does not contain " + str(BASE))
    if overlay is not None and not overlay.is_dir():
        parser.error("--overlay is not a directory")
    if args.timeout_seconds <= 0:
        parser.error("--timeout-seconds must be positive")
    if out.exists() and (not out.is_dir() or any(out.iterdir())):
        parser.error("--out must be new or empty; choose a fresh directory to preserve evidence")
    try:
        java = command_path(args.java)
        compiler = ([command_path(args.javac)] if args.javac else
                    [java, "-m", "jdk.compiler/com.sun.tools.javac.Main"])
    except ValueError as exc:
        parser.error(str(exc))
    out.mkdir(parents=True, exist_ok=True)
    receipt = {
        "format": "R32_4_DIAGNOSTIC_HOST_RUN_V1",
        "repo": str(repo), "overlay": str(overlay) if overlay else None,
        "runner": str(Path(__file__).resolve()),
        "runnerSha256": digest(Path(__file__).read_bytes()),
        "scope": "HOST_BOUNDARY_ASSERTIONS_ON_LISTED_REAL_PRODUCTION_SOURCES; SOURCE_TARGET_17; NOT_ANDROID_OR_NATURAL_QUALITY",
        "expectedAssertions": 51, "suites": [],
        "inputRepositoryMutatedByRunner": False,
        "naturalQuality": "NOT_PROVEN", "realAuthorizationGranted": False,
        "compileOnlyDeclarations": STUBS,
        "compileOnlyDeclarationsBoundary": "Unrelated report, timeline and gate APIs are declarations/adapters only; their real implementation is not verified by this suite.",
    }
    receipt["javaVersion"] = run_process([java, "-version"], out / "java_version.log",
                                        out, args.timeout_seconds)
    snapshots = []
    try:
        for spec in SUITES:
            name = spec["name"]
            print("Running " + name + " production-source assertions", flush=True)
            suite_dir = out / name
            classes = suite_dir / "classes"
            empty_path = suite_dir / "empty_source_and_class_path"
            classes.mkdir(parents=True)
            empty_path.mkdir()
            selected, inputs = [], []

            def snapshot(original: Path, relative: Path, kind: str) -> None:
                if not original.is_file():
                    raise FileNotFoundError(str(original))
                data = original.read_bytes()
                target = suite_dir / "sources" / kind / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(data)
                info = {"kind": kind, "originalPath": str(original),
                        "relativePath": relative.as_posix(), "sha256": digest(data),
                        "compiledSnapshot": str(target), "bytes": len(data)}
                inputs.append(info)
                snapshots.append(info)
                selected.append(str(target))

            for filename in spec["production"]:
                relative = BASE / filename
                original = repo / relative
                if overlay is not None and (overlay / relative).is_file():
                    original = overlay / relative
                snapshot(original, relative, "production")
            snapshot(HERE / "tests" / (spec["main"] + ".java"),
                     Path(spec["main"] + ".java"), "test")
            for filename in spec["stubs"]:
                snapshot(HERE / "stubs" / filename, Path(filename), "compile_only")

            compile_command = compiler + ["-source", "17", "-target", "17",
                "-encoding", "UTF-8", "-classpath", str(empty_path),
                "-sourcepath", str(empty_path), "-d", str(classes)] + selected
            compile_result = run_process(compile_command, suite_dir / "compile.log",
                                         out, args.timeout_seconds)
            suite = {"name": name, "expectedAssertions": spec["expected"],
                     "inputs": inputs, "compile": compile_result,
                     "actualProductionMethodsUnderTest": {
                         "page": ["ResearchTableAuditR32.audit/inspect/yes/knownId"],
                         "assessment": ["ReportAssessmentR33.evaluate/verifiedPass/group",
                             "MetricDefinitionsR29.r31Definitions",
                             "CheckMetricCrosswalkR31.metric/uncoveredConditions",
                             "MetricEvaluatorR29.evaluateR31"]}[name]}
            if compile_result["exitCode"] == 0:
                result = run_process([java, "-cp", str(classes), PACKAGE + spec["main"]],
                                     suite_dir / "test.log", out, args.timeout_seconds)
                suite["test"] = result
                matches = re.findall(r"^RESULT checks=(\d+) failures=(\d+)$",
                                     result["output"], re.MULTILINE)
                suite["observedAssertions"] = int(matches[0][0]) if len(matches) == 1 else None
                suite["observedFailures"] = int(matches[0][1]) if len(matches) == 1 else None
                suite["passed"] = (result["exitCode"] == 0 and len(matches) == 1
                                    and suite["observedAssertions"] == spec["expected"]
                                    and suite["observedFailures"] == 0)
            else:
                suite.update({"test": None, "observedAssertions": None,
                              "observedFailures": None, "passed": False})
            receipt["suites"].append(suite)
    except (OSError, ValueError) as exc:
        receipt["runnerError"] = repr(exc)

    changed = []
    for info in snapshots:
        try:
            same = digest(Path(info["originalPath"]).read_bytes()) == info["sha256"]
        except OSError:
            same = False
        if not same:
            changed.append(info["originalPath"])
    receipt["inputChangesObservedDuringRun"] = sorted(set(changed))
    receipt["actualAssertions"] = sum(s.get("observedAssertions") or 0 for s in receipt["suites"])
    receipt["actualFailedAssertions"] = sum(s.get("observedFailures") or 0 for s in receipt["suites"])
    success = (len(receipt["suites"]) == 2 and all(s["passed"] for s in receipt["suites"])
               and not changed and "runnerError" not in receipt
               and receipt["javaVersion"]["exitCode"] == 0)
    receipt["verdict"] = "HOST_ASSERTIONS_PASSED" if success else "HOST_ASSERTIONS_FAILED_OR_NOT_RUN"
    receipt["exitCode"] = 0 if success else 1
    receipt["finishedAtUtc"] = datetime.now(timezone.utc).isoformat()
    receipt_path = out / "RESULTS.json"
    receipt_path.write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"receipt": str(receipt_path), "verdict": receipt["verdict"],
                      "assertions": receipt["actualAssertions"],
                      "failedAssertions": receipt["actualFailedAssertions"],
                      "naturalQuality": "NOT_PROVEN"}, ensure_ascii=False))
    return receipt["exitCode"]


if __name__ == "__main__":
    sys.exit(main())
