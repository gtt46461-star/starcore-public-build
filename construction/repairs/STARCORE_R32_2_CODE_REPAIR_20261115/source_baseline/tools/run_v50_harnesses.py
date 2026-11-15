#!/usr/bin/env python3
"""Run every V50 executable gate and emit one machine-readable report."""
from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SCRIPTS = (
    "v50_architecture_contract_harness.py",
    "v50_runtime_output_harness.py",
    "strict_walk_forward_oos.py",
    "v50_feature_parity.py",
    "v50_deterministic_replay.py",
    "v50_action_rank_harness.py",
    "v50_selected_calibration_harness.py",
    "v50_ledger_harness.py",
    "v50_database_migration_harness.py",
    "v50_fault_injection_harness.py",
    "v50_performance_harness.py",
    "v50_ablation_benchmark.py",
)


def main() -> int:
    results = []
    failures = []
    for script in SCRIPTS:
        process = subprocess.run(
            [sys.executable, str(ROOT / "tools" / script)],
            cwd=str(ROOT),
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=300,
            check=False,
        )
        try:
            payload = json.loads(process.stdout) if process.stdout.strip() else {}
        except json.JSONDecodeError:
            payload = {"raw_stdout": process.stdout[-4000:]}
        entry = {
            "script": script,
            "returncode": process.returncode,
            "result": payload,
        }
        if process.stderr.strip():
            entry["stderr"] = process.stderr[-4000:]
        results.append(entry)
        if process.returncode != 0 or payload.get("status") != "PASS":
            failures.append(script)

    report_path = ROOT / "reports" / "V50_HARNESSES_50072403.json"
    output = {
        "version": "V50_PRIME_50072403",
        "engineering_gate": "PASS" if not failures else "FAIL",
        "model_gate": "NOT_PROVEN",
        "realtime_gate": "NOT_PROVEN",
        "harness_count": len(results),
        "passed": len(results) - len(failures),
        "failed": len(failures),
        "failed_scripts": failures,
        "results": results,
    }
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(
        json.dumps(output, ensure_ascii=False, indent=2, sort_keys=True),
        encoding="utf-8",
    )
    print(json.dumps(output, ensure_ascii=False, indent=2, sort_keys=True))
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
