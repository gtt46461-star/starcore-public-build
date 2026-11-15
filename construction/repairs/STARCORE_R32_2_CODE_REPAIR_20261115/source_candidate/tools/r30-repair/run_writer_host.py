"""Original rotation/retry contracts on actual telemetry + coordinator/native HOST SQLite."""
import argparse
import datetime
import hashlib
import json
import os
import pathlib
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument("--output", type=pathlib.Path)
parser.add_argument("--support-jar", type=pathlib.Path, default=pathlib.Path(os.environ["R31_TELEMETRY_SUPPORT_JAR"]) if "R31_TELEMETRY_SUPPORT_JAR" in os.environ else None)
options = parser.parse_args()
# Persistent caller-selected evidence, or a fresh temporary directory for the legacy zero-argument entry point.
output = options.output.resolve() if options.output else pathlib.Path(tempfile.mkdtemp(prefix="r31-original-writer-contract-"))
output.mkdir(parents=True, exist_ok=not bool(options.output))
env = dict(os.environ)
env.pop("JAVA_TOOL_OPTIONS", None)
source = ROOT / "app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldLedgerR30.java"
original = ROOT / "construction/plans/STARCORE_R30_FINAL_REPAIR_20261101/verification/WriterRotateTest.java"
receipt = {"scope": "Original160000 rotation and old12/new3 assertions; real production coordinator + native HOST SQLite; NOT_ANDROID_NOT_PROCESS_DEATH_NOT_SLO", "sourceCapturedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(), "sources": {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in [source, original]}, "runs": [], "status": "IN_PROGRESS"}


def run(args, name):
    args = list(map(str, args))
    result = subprocess.run(args, cwd=ROOT, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=60)
    (output / (name + ".log")).write_text(result.stdout)
    receipt["runs"].append({"name": name, "command": args, "exitCode": result.returncode})
    receipt["status"] = "FAILED" if result.returncode else "IN_PROGRESS"
    (output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    if result.returncode:
        print(result.stdout, flush=True)
        raise RuntimeError(name + " failed; original output preserved")
    return result.stdout


with tempfile.TemporaryDirectory(prefix="repair-writer-rotation-") as temporary:
    print(run(["javac", "-encoding", "UTF-8", "-d", temporary, source, original], "compile-original-rotation"), end="")
    print(run(["java", "-cp", temporary, "com.ethk3.novaonepro.strategyv2.WriterRotateTest"], "original-rotation160000"), end="")
command = [sys.executable, ROOT / "tools/r31/run_telemetry_budget_host.py", "--legacy-retry-only", "--output", output / "original-retry-native"]
if options.support_jar is not None:
    command.extend(["--support-jar", options.support_jar])
run(command, "original-retry-native")
for line in (output / "original-retry-native/legacy-retry.log").read_text().splitlines():
    if line.startswith("WRITER_RETRY_NATIVE_SQLITE_HOST_PASS"):
        print(line, flush=True)
receipt["status"] = "ORIGINAL_WRITER_CONTRACTS_COMPLETED_REAL_COORDINATOR_HOST"
receipt["completedAt"] = datetime.datetime.now(datetime.timezone.utc).isoformat()
(output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
print("FR_B02_WRITER_SOURCE_AND_NATIVE_SQLITE_HOST_SCOPE_PASS_NOT_ANDROID_OR_PROCESS_DEATH", flush=True)
print("R31_ORIGINAL_WRITER_EVIDENCE", output, flush=True)
