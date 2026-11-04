#!/usr/bin/env python3
"""B00 evidence: execute production pure Java and production DDL/SQL against isolated host SQLite."""
import hashlib
import json
from pathlib import Path
import re
import sqlite3
import subprocess
import tempfile
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "app/src/main/java/com/ethk3/novaonepro/strategyv2"
OUT = ROOT / "verification/r31/runtime_observation"
PLAN = ROOT / "construction/STARCORE_R31_FINAL_20261104"


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    java_sources = [SOURCE / (name + ".java") for name in ("JsonR27", "StatsR27", "RuntimeFaultEvidenceR31", "WaitKindR27")]
    harness = OUT / "RuntimeFaultEvidenceR31HostTest.java"
    with tempfile.TemporaryDirectory(prefix="r31-observation-") as directory:
        compile_result = subprocess.run(["javac", "-d", directory, *map(str, java_sources), str(harness)], text=True, capture_output=True)
        (OUT / "javac.log").write_text(compile_result.stdout + compile_result.stderr)
        if compile_result.returncode:
            raise RuntimeError("production helper compilation failed; see javac.log")
        run_result = subprocess.run(["java", "-cp", directory, "com.ethk3.novaonepro.strategyv2.RuntimeFaultEvidenceR31HostTest"], text=True, capture_output=True)
        (OUT / "java.log").write_text(run_result.stdout + run_result.stderr)
        if run_result.returncode:
            raise RuntimeError("production helper assertions failed; see java.log")
        java_receipt = json.loads(run_result.stdout.strip().splitlines()[-1])

    report_path = PLAN / "evidence/original_report/R30_REPORT.json"
    report = json.loads(report_path.read_text())
    tasks = report["chapters"][3]["data"]["tasks"]
    r27 = [task for task in tasks if task["task_id"].startswith("r27-")]
    facts = {
        "tasks": len(tasks),
        "quarantined": sum(task["status"] == "QUARANTINED" for task in tasks),
        "r27Tasks": len(r27),
        "r27Quarantined": sum(task["status"] == "QUARANTINED" for task in r27),
        "r27ClaimFailure": sum("R30_PREPARED_TASK_CLAIM" in task.get("wait_detail", "") for task in r27),
        "r27Cancelled": sum("R27_CANCELLED" in task.get("wait_detail", "") for task in r27),
        "r27StudentDependency": sum("MATRIX_ORIGINAL_STUDENT_UNAVAILABLE" in task.get("wait_detail", "") for task in r27),
    }
    assert facts == dict(tasks=60, quarantined=14, r27Tasks=13, r27Quarantined=13, r27ClaimFailure=11, r27Cancelled=1, r27StudentDependency=1), facts
    # The original report has only wrapper errors. Deliberately do not infer a busy root cause.
    facts["claimRootCause"] = "UNOBSERVED_IN_ORIGINAL_REPORT"
    facts["reportSha256"] = hashlib.sha256(report_path.read_bytes()).hexdigest()

    schema_source = (SOURCE / "SchemaR27.java").read_text()
    ddl_block = schema_source.split("public static final String[] R31_DDL = {", 1)[1].split("\n    };", 1)[0]
    ddls = [json.loads('"' + value + '"') for value in re.findall(r'"((?:\\.|[^"\\])*)"', ddl_block)]
    db = sqlite3.connect(":memory:")
    db.execute("CREATE TABLE existing_evidence(id INTEGER PRIMARY KEY,payload BLOB)")
    db.execute("INSERT INTO existing_evidence VALUES(1,?)", (bytes(range(256)),))
    before = db.execute("SELECT hex(payload) FROM existing_evidence").fetchone()[0]
    for _ in range(2):
        for ddl in ddls:
            db.execute(ddl)
    assert db.execute("SELECT hex(payload) FROM existing_evidence").fetchone()[0] == before
    assert len(db.execute("SELECT name FROM sqlite_master WHERE name LIKE 'r31_%'").fetchall()) == 6
    pool_source = (SOURCE / "ResearchPoolR27.java").read_text()
    insert_sql = json.loads('"' + re.search(r'db\.execSQL\("(INSERT INTO r31_runtime_fault[^"\n]+)"', pool_source).group(1) + '"')
    row = ["event", "r27-live", "epoch", 7, "OP", "CLAIM", "DISK_FULL", "fingerprint", 1, "[]", "redacted", "hash", "[]", "redacted", "hash", "UNKNOWN_NOT_INFERRED_FROM_EXCEPTION", "BOOT_TEST", 1, 1, 1, 1, 1, 1040]
    db.execute(insert_sql, row)
    try:
        db.execute(insert_sql, row)
        raise AssertionError("duplicate sample identity accepted")
    except sqlite3.IntegrityError:
        pass
    update_sql = json.loads('"' + re.search(r'db\.execSQL\("(UPDATE r31_runtime_fault SET occurrences[^"\n]+)"', pool_source).group(1) + '"')
    for i in range(100):
        db.execute(update_sql, (i + 2, i + 2, "[]", "new redacted", "hash2", 1044, "event"))
    aggregate = db.execute("SELECT occurrences,stack,last_stack,first_wall_ms,last_wall_ms FROM r31_runtime_fault").fetchone()
    assert aggregate == (101, "redacted", "new redacted", 1, 101), aggregate
    try:
        invalid = row.copy(); invalid[0] = "negative"; invalid[7] = "other"; invalid[-1] = -1
        db.execute(insert_sql, invalid)
        raise AssertionError("negative byte budget accepted")
    except sqlite3.IntegrityError:
        pass
    # No device SQLite or process-kill result is claimed by this host protocol check.
    result = {
        "scope": "P00_PRODUCTION_HELPER_AND_ADDITIVE_SQL_HOST_CHECKS",
        "status": "PASS",
        "capturedAt": datetime.now(timezone.utc).isoformat(),
        "java": java_receipt,
        "originalReportFacts": facts,
        "sqlite": {"version": sqlite3.sqlite_version, "ddlStatements": len(ddls), "idempotentApply": "PASS", "preservedUnrelatedBlob": "PASS", "aggregateRetainsFirstAndLast": "PASS", "duplicateIdentityRejected": "PASS", "negativeByteCountRejected": "PASS"},
        "productionSources": {str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest() for path in [*java_sources, SOURCE / "SchemaR27.java", SOURCE / "ResearchPoolR27.java", SOURCE / "RuntimeCoordinatorR26.java"]},
        "androidBuild": "NOT_RUN_BY_THIS_SCRIPT",
        "device": "NOT_RUN",
        "naturalQuality": "NOT_PROVEN",
        "remaining": ["Android SQLite adapter fault injection", "runtime observation failure after real business commit", "device report evidence", "P01 disposition/recovery/fence contracts"],
    }
    (OUT / "receipt.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
