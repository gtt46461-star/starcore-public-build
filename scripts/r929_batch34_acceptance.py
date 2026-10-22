#!/usr/bin/env python3
"""Generate and enforce Batch-3/4 engineering acceptance from executable host evidence.

Synthetic fixtures may close implementation contracts, but they can never set the production
future-OOS/effect flags. This script deliberately keeps those two claims separate.
"""
from __future__ import annotations

import hashlib
import json
import os
import pathlib
import re
import sys
from typing import Dict, List, Tuple

ROOT = pathlib.Path(__file__).resolve().parents[1]
HARNESS = ROOT / "build/r925-host/Batch34ContractHarnessR929.txt"
OUT_DIR = ROOT / "reports/r929"
OUT = OUT_DIR / "BATCH34_ENGINEERING_ACCEPTANCE.json"
TASK3 = ROOT / "docs/5.4.2/01_TASKS/tasks_batch3_D01-D08_D10_C01_C03_C06.json"
TASK4 = ROOT / "docs/5.4.2/01_TASKS/tasks_batch4_C02_C04_C05_C07-C12_D09.json"
PRODUCTION = ROOT / "app/src/main/java/com/ethk3/novaonepro/Batch34ProductionEvidenceR929.java"
EXPECTED3 = ["D01", "D02", "D03", "D04", "D05", "D06", "D07", "D08", "D10", "C01", "C03", "C06"]
EXPECTED4 = ["C02", "C04", "C05", "C07", "C08", "C09", "C10", "C11", "C12", "D09"]


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def fail(code: str, detail: str = "") -> None:
    suffix = f"|{detail}" if detail else ""
    raise SystemExit(f"R929_BATCH34_ACCEPTANCE_FAIL|{code}{suffix}")


def load_task_ids(path: pathlib.Path) -> List[str]:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:  # pragma: no cover - CI reports the exact parse failure
        fail("TASK_JSON_INVALID", f"{path}:{exc}")
    if not isinstance(payload, list):
        fail("TASK_JSON_NOT_LIST", str(path))
    ids: List[str] = []
    for row in payload:
        if not isinstance(row, dict) or not isinstance(row.get("id"), str):
            fail("TASK_ID_INVALID", str(path))
        ids.append(row["id"])
    if len(ids) != len(set(ids)):
        fail("TASK_ID_DUPLICATE", str(path))
    return ids


def parse_harness(text: str) -> Tuple[Dict[str, List[str]], Dict[str, int], str]:
    assertions: Dict[str, List[str]] = {}
    task_counts: Dict[str, int] = {}
    summary = ""
    for raw in text.splitlines():
        line = raw.strip()
        if line.startswith("ASSERT|"):
            parts = line.split("|", 3)
            if len(parts) != 4 or parts[2] != "PASS":
                fail("ASSERT_LINE_INVALID", line)
            assertions.setdefault(parts[1], []).append(parts[3])
        elif line.startswith("TASK|"):
            parts = line.split("|")
            if len(parts) != 4 or parts[2] != "PASS" or not parts[3].startswith("assertions="):
                fail("TASK_LINE_INVALID", line)
            try:
                task_counts[parts[1]] = int(parts[3].split("=", 1)[1])
            except ValueError:
                fail("TASK_ASSERTION_COUNT_INVALID", line)
        elif line.startswith("SUMMARY|"):
            summary = line
    if "R929_BATCH34_CONTRACT_PASS" not in text.splitlines():
        fail("HARNESS_PASS_MARKER_MISSING")
    if "batch3=12/12|batch4=10/10|tasks=22/22" not in summary:
        fail("HARNESS_SUMMARY_INVALID", summary)
    return assertions, task_counts, summary


def check_production_fail_closed(source: str) -> Dict[str, bool]:
    checks = {
        "generated_from_real_data_false": "GENERATED_FROM_REAL_DATA = false" in source,
        "future_oos_proven_false": "FUTURE_OOS_PROVEN = false" in source,
        "policy_effect_proven_false": "POLICY_EFFECT_PROVEN = false" in source,
        "current_evidence_has_no_dataset": re.search(r"return new Batch34RuntimeGateR929\.ProductionEvidence\(\s*null,", source) is not None,
    }
    failed = [key for key, value in checks.items() if not value]
    if failed:
        fail("CHECKED_IN_PRODUCTION_EVIDENCE_NOT_FAIL_CLOSED", ",".join(failed))
    return checks


def main() -> int:
    for path in (HARNESS, TASK3, TASK4, PRODUCTION):
        if not path.is_file():
            fail("REQUIRED_FILE_MISSING", str(path.relative_to(ROOT)))

    task3_ids = load_task_ids(TASK3)
    task4_ids = load_task_ids(TASK4)
    if task3_ids != EXPECTED3:
        fail("BATCH3_TASK_SET_DRIFT", f"actual={task3_ids}")
    if task4_ids != EXPECTED4:
        fail("BATCH4_TASK_SET_DRIFT", f"actual={task4_ids}")
    overlap = sorted(set(task3_ids).intersection(task4_ids))
    if overlap:
        fail("TASK_SET_OVERLAP", ",".join(overlap))

    harness_text = HARNESS.read_text(encoding="utf-8")
    assertions, task_counts, summary = parse_harness(harness_text)
    expected = EXPECTED3 + EXPECTED4
    unknown = sorted((set(assertions) | set(task_counts)) - set(expected))
    if unknown:
        fail("UNKNOWN_TASK_IN_HARNESS", ",".join(unknown))
    for task in expected:
        names = assertions.get(task, [])
        count = task_counts.get(task, 0)
        if not names or count != len(names):
            fail("TASK_EXECUTABLE_EVIDENCE_INCOMPLETE", f"{task}:lines={len(names)}:declared={count}")
        if len(names) != len(set(names)):
            fail("DUPLICATE_ASSERTION_NAME", task)

    production_source = PRODUCTION.read_text(encoding="utf-8")
    fail_closed = check_production_fail_closed(production_source)
    assertion_total = sum(len(value) for value in assertions.values())
    version_props = {}
    identity = ROOT / "release/release_identity.properties"
    if identity.is_file():
        for line in identity.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.lstrip().startswith("#"):
                key, value = line.split("=", 1)
                version_props[key.strip()] = value.strip()

    record = {
        "schema": "R929_BATCH34_ENGINEERING_ACCEPTANCE_V1",
        "source_commit": os.environ.get("GITHUB_SHA", "LOCAL_UNBOUND"),
        "release_identity": {
            "versionName": version_props.get("versionName", ""),
            "versionCode": version_props.get("versionCode", ""),
        },
        "task_contracts": {
            "batch3": {
                "accepted": len(EXPECTED3),
                "total": len(EXPECTED3),
                "percent": 100,
                "ids": EXPECTED3,
            },
            "batch4": {
                "accepted": len(EXPECTED4),
                "total": len(EXPECTED4),
                "percent": 100,
                "ids": EXPECTED4,
            },
            "combined": {
                "accepted": len(expected),
                "total": len(expected),
                "percent": 100,
                "assertion_count": assertion_total,
            },
        },
        "assertions": assertions,
        "harness_summary": summary,
        "harness_sha256": sha256(HARNESS),
        "task_manifest_sha256": {
            "batch3": sha256(TASK3),
            "batch4": sha256(TASK4),
        },
        "checked_in_production_fail_closed": fail_closed,
        "proof_boundary": {
            "engineering_construction": "PASS",
            "synthetic_fixture": "ENGINEERING_CODE_PATH_ONLY",
            "generated_from_real_data": False,
            "future_oos_effect_proven": False,
            "real_unlock": False,
            "non_real_system_opportunity_route": "SHADOW_REQUIRED_OR_AUDITED_REJECTION",
            "effect_targets": {
                "selected_coverage": 0.15,
                "selected_hit_rate": 0.40,
                "ece_max": 0.04,
                "longest_miss_max": 8,
            },
        },
    }
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(record, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(
        "R929_BATCH34_ACCEPTANCE_PASS"
        f"|batch3={len(EXPECTED3)}/{len(EXPECTED3)}"
        f"|batch4={len(EXPECTED4)}/{len(EXPECTED4)}"
        f"|tasks={len(expected)}/{len(expected)}"
        f"|assertions={assertion_total}"
        "|effect=UNPROVEN_FAIL_CLOSED"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
