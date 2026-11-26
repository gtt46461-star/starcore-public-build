#!/usr/bin/env python3
"""Host regression: actual SQLite TEMP catalog + compiled pure Java policy.

This does not claim Android WAL/service/device timing validation.
"""
import json
import argparse
import pathlib
import sqlite3
import subprocess
import tempfile

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parent
JAVA = ROOT / "after/app/src/main/java/com/ethk3/novaonepro/strategyv2"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--baseline", type=pathlib.Path, default=HERE.parents[2] / "source-installed")
    args = parser.parse_args()
    classifier = args.baseline / "app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureDispositionR31.java"
    if not classifier.is_file():
        parser.error("pass --baseline with the source checkout containing FailureDispositionR31.java")
    with tempfile.TemporaryDirectory(prefix="r343-runtime-") as temp:
        classes = pathlib.Path(temp)
        subprocess.run(["java", "com.sun.tools.javac.Main", "-d", str(classes),
                        str(JAVA / "StartupTempSchemaR343.java"),
                        str(JAVA / "StartupProbePolicyR343.java"),
                        str(HERE / "RuntimePolicyHarness.java"),
                        str(HERE / "ParseChangedJava.java"),
                        str(HERE / "FailureCategoryHarness.java"), str(classifier),
                        str(args.baseline / "app/src/main/java/com/ethk3/novaonepro/strategyv2/JsonR27.java"),
                        str(args.baseline / "app/src/main/java/com/ethk3/novaonepro/strategyv2/StatsR27.java"),
                        str(JAVA.parent / "WriterFaultSnapshotViewR343.java"),
                        str(HERE / "WriterFaultSnapshotHarness.java")], check=True)
        parse_result = subprocess.check_output(["java", "-cp", str(classes), "ParseChangedJava", str(ROOT / "after")], text=True).strip()
        category_result = subprocess.check_output(["java", "-cp", str(classes), "FailureCategoryHarness"], text=True).strip()
        snapshot_result = subprocess.check_output(["java", "-cp", str(classes), "com.ethk3.novaonepro.WriterFaultSnapshotHarness"], text=True).strip()
        summary = subprocess.check_output(["java", "-cp", str(classes), "RuntimePolicyHarness"], text=True).strip()
        db = sqlite3.connect(":memory:")
        ddls = [
            "CREATE TEMP TABLE IF NOT EXISTS r31q_ddl_cache_flush(x INTEGER)",
            "CREATE TEMP TABLE IF NOT EXISTS r335_extension_catalog(original_id INTEGER PRIMARY KEY,type,name,tbl_name,sql)",
            "CREATE TEMP TABLE IF NOT EXISTS r335_extension_seal(domain TEXT,revision INTEGER,contract_hash TEXT,fingerprint TEXT,state TEXT)",
            "CREATE TEMP TABLE IF NOT EXISTS r335_extension_journal(step_id TEXT,status TEXT,domain TEXT,revision INTEGER)",
        ]
        for ddl in ddls:
            db.execute(ddl)
        # Exact original predicate, with the legitimate producer-created table.
        old = db.execute("SELECT 1 FROM sqlite_temp_master WHERE type <> 'table' OR name NOT IN "
                         "('r335_extension_catalog','r335_extension_seal','r335_extension_journal') LIMIT 1").fetchone()
        assert old == (1,), "The baseline regression did not reproduce"
        rows = db.execute("SELECT type,name,tbl_name,sql FROM sqlite_temp_master ORDER BY name").fetchall()
        for row in rows:
            result = subprocess.check_output(["java", "-cp", str(classes), "RuntimePolicyHarness", *row], text=True).strip()
            assert result == "true", row
        db.execute("CREATE TEMP TABLE r29_model_checkpoint(owner TEXT)")
        row = db.execute("SELECT type,name,tbl_name,sql FROM sqlite_temp_master WHERE name='r29_model_checkpoint'").fetchone()
        result = subprocess.check_output(["java", "-cp", str(classes), "RuntimePolicyHarness", *row], text=True).strip()
        assert result == "false", "Shadowing remains forbidden"
        print(json.dumps({"status": "PASS", "pure_java": summary, "sqlite_version": sqlite3.sqlite_version,
                          "baseline_temp_schema_failure_reproduced": True,
                          "actual_owned_temp_contracts_accepted": len(rows),
                          "unexpected_checkpoint_shadow_rejected": True,
                          "snapshot_observation": snapshot_result, "syntax_parse": parse_result, "actual_classifier": category_result,
                          "android_device_tests": "NOT_RUN", "apk_build": "NOT_RUN"}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
