"""Retention startup/reopen tests with actual source and native SQLite; HOST_NOT_DEVICE."""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import signal
import sqlite3
import subprocess
import tempfile
import time

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument("--out", type=Path, required=True)
parser.add_argument("--classes", type=Path, required=True)
parser.add_argument("--production-root", type=Path, default=ROOT)
parser.add_argument("--baseline-only", action="store_true")
args = parser.parse_args()
out = args.out.resolve()
out.mkdir(parents=True, exist_ok=False)
sdk = Path(os.environ["ANDROID_HOME"]) / "platforms/android-36/android.jar"
jdbc = Path(os.environ.get("SQLITE_JDBC", "/tmp/r31-baseline-deps/sqlite-jdbc.jar"))
env = dict(os.environ)
env.pop("JAVA_TOOL_OPTIONS", None)


def sha(path):
    if path.is_file():
        return hashlib.sha256(path.read_bytes()).hexdigest()
    result = hashlib.sha256()
    for child in sorted(path.rglob("*.class")):
        result.update(str(child.relative_to(path)).encode() + b"\0")
        result.update(hashlib.sha256(child.read_bytes()).digest())
    return result.hexdigest()


receipt = {
    "scope": "ACTUAL_RETENTION_DOMAIN_REVOCATION_NATIVE_SQLITE_HOST_NOT_ANDROID_NOT_DEVICE",
    "sourceCapturedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "head": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
    "originalTestSourceCommit": "2f91a41c2904df4d465158254c090f85be7f16ad",
    "supportClasses": str(args.classes), "supportClassesSha256": sha(args.classes),
    "androidJarSha256": sha(sdk), "sqliteJdbcSha256": sha(jdbc),
    "records": [], "status": "IN_PROGRESS",
    "criticalStamp": "MUST_REMAIN_MISSING_IN_THIS_MINIMAL_FIXTURE",
    "androidDevice": "NOT_RUN", "naturalQuality": "NOT_EVALUATED",
}


def save():
    (out / "RECEIPT.json").write_text(json.dumps(receipt, indent=2) + "\n")


def run(command, label, expected_failure=False):
    command = list(map(str, command))
    start = time.monotonic_ns()
    process = subprocess.Popen(command, cwd=ROOT, env=env, text=True,
                               stdout=subprocess.PIPE, stderr=subprocess.STDOUT, start_new_session=True)
    try:
        stdout, _ = process.communicate(timeout=90)
    except subprocess.TimeoutExpired:
        os.killpg(process.pid, signal.SIGKILL)
        stdout, _ = process.communicate(timeout=10)
        (out / (label + ".log")).write_text(stdout)
        receipt["status"] = "FAILED_TIMEOUT"
        save()
        raise RuntimeError(label + " exceeded 90s; whole HOST process group terminated")
    (out / (label + ".log")).write_text(stdout)
    receipt["records"].append({"label": label, "command": command,
                               "exitCode": process.returncode,
                               "monotonicDurationNs": time.monotonic_ns() - start})
    save()
    print(label, "exit=" + str(process.returncode), flush=True)
    if expected_failure:
        if not process.returncode or not all(value in stdout for value in (
                "CHECKPOINT_STARTUP_BASELINE_RETAINED_WITHOUT_PER_BATCH_ARCHIVE_CHURN",
                "DatabaseCoordinator$SchemaPendingR31", "UNKNOWN_ARCHIVE_ATTACHMENT",
                "host_startup_archive_failure", "REAL_REOPEN_FAILED")):
            raise RuntimeError("Original test did not fail at the expected strict archive guard: " + stdout[-10000:])
    elif process.returncode:
        receipt["status"] = "FAILED"
        save()
        raise RuntimeError(label + " failed; original log preserved: " + stdout[-12000:])
    return stdout


def observe(databases, label):
    records = []
    for file in sorted(databases.glob("*.db")):
        with sqlite3.connect("file:" + str(file) + "?mode=ro", uri=True) as db:
            table_names = {row[0] for row in db.execute("SELECT name FROM sqlite_master WHERE type='table'")}
            records.append({
                "databaseName": file.name,
                "checkpointRows": list(db.execute("SELECT owner,state_hash,typeof(artifact),length(artifact) FROM r29_model_checkpoint")),
                "archiveRows": list(db.execute("SELECT state_hash,typeof(artifact),length(artifact),__r30_reason FROM r30_keep_r29_model_checkpoint ORDER BY __r30_id")),
                "actualBlobHashes": [{"stateHash": row[0], "actualArtifactSha256": hashlib.sha256(row[1]).hexdigest()}
                                     for row in db.execute("SELECT state_hash,artifact FROM r29_model_checkpoint UNION ALL SELECT state_hash,artifact FROM r30_keep_r29_model_checkpoint")],
                "startupMarkers": db.execute("SELECT count(*) FROM r30_retention_boot_checkpoint").fetchone()[0],
                "criticalStamps": list(db.execute("SELECT domain,state FROM v57_schema_domain_r926 WHERE domain='CRITICAL'")) if "v57_schema_domain_r926" in table_names else "INFRASTRUCTURE_NAME_CHECK_REQUIRED",
                "syntheticFailureTriggers": list(db.execute("SELECT name FROM sqlite_master WHERE type='trigger' AND name='host_startup_archive_failure'")),
                "rollbackProbeRows": db.execute("SELECT count(*) FROM host_startup_tx_probe").fetchone()[0] if "host_startup_tx_probe" in table_names else None,
            })
    (out / (label + "-readonly.json")).write_text(json.dumps(records, indent=2) + "\n")


save()
with tempfile.TemporaryDirectory(prefix="r31-checkpoint-startup-host-") as temporary:
    tmp = Path(temporary)
    platform, production, original, fixed = [tmp / p for p in ("platform", "production", "original", "fixed")]
    for directory in (platform, production, original, fixed):
        directory.mkdir()
    shims = sorted((ROOT / "tools/r29/qual_host/shim").rglob("*.java"))
    receipt["platformSources"] = {str(p.relative_to(ROOT)): sha(p) for p in shims}
    run(["javac", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, [args.classes, sdk, jdbc])),
         "-d", platform, *shims], "compile-native-jdbc-platform")
    paths = ["com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java",
             "com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java",
             "com/ethk3/novaonepro/strategyv2/CertificateRevocationR30.java"]
    snapshot = out / "source-snapshot"
    snapshot.mkdir()
    captured = []
    receipt["sourceSha256"] = {}
    for relative in paths:
        source = args.production_root / "app/src/main/java" / relative
        destination = snapshot / source.name
        destination.write_bytes(source.read_bytes())
        captured.append(destination)
        receipt["sourceSha256"][str(source)] = sha(destination)
    run(["javac", "-encoding", "UTF-8", "-proc:none", "-sourcepath", "", "-implicit:none",
         "-cp", os.pathsep.join(map(str, [args.classes, sdk])), "-d", production, *captured],
        "compile-actual-three-production-owners")
    common = [platform, production, args.classes, jdbc, sdk]
    baseline = ROOT / "tools/r31/checkpoint_startup_host/baseline/CheckpointStartupR30HostTest.java"
    receipt["originalTestSha256"] = sha(baseline)
    run(["javac", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, common)), "-d", original, baseline],
        "compile-original-test-unchanged")
    original_db = tmp / "original-databases"
    original_db.mkdir()
    run(["java", "-Xmx768m", "-Djava.io.tmpdir=" + str(original_db), "-cp",
         os.pathsep.join(map(str, [original, *common])),
         "com.ethk3.novaonepro.strategyv2.CheckpointStartupR30HostTest"],
        "original-test-strict-owner-FAILED", expected_failure=True)
    observe(original_db, "original-test-strict-owner-FAILED")
    if not args.baseline_only:
        test_source = ROOT / "tools/r30/qual_host/CheckpointStartupR30HostTest.java"
        receipt["adaptedTestSha256"] = sha(test_source)
        run(["javac", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, common)), "-d", fixed, test_source],
            "compile-adapted-original-test")
        fixed_db = tmp / "adapted-databases"
        fixed_db.mkdir()
        output = run(["java", "-Xmx768m", "-Djava.io.tmpdir=" + str(fixed_db), "-cp",
                      os.pathsep.join(map(str, [fixed, *common])),
                      "com.ethk3.novaonepro.strategyv2.CheckpointStartupR30HostTest"],
                     "actual-native-reopen-readonly-rollback-recover")
        receipt["testOutput"] = output.splitlines()
        observe(fixed_db, "adapted-test")
    receipt["compiledOwnerClassSha256"] = {str(p.relative_to(production)): sha(p) for p in sorted(production.rglob("*.class"))}
    receipt["sourceOriginsUnchanged"] = all(sha(Path(p)) == value for p, value in receipt["sourceSha256"].items())
    if not receipt["sourceOriginsUnchanged"] or sha(args.classes) != receipt["supportClassesSha256"]:
        raise RuntimeError("Bound sources/support changed during execution; no coherent final receipt")
receipt["status"] = "ORIGINAL_TEST_FAILED_EXPECTED_AT_STRICT_GUARD" if args.baseline_only else "TARGETED_NATIVE_CHECKPOINT_STARTUP_COMPLETED_NOT_DEVICE"
receipt["finishedAt"] = datetime.datetime.now(datetime.timezone.utc).isoformat()
save()
print(receipt["status"], out, flush=True)
