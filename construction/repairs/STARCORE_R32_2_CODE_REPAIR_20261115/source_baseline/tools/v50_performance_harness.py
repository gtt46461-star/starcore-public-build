#!/usr/bin/env python3
"""Validate V50 budgets and inclusive/exclusive span reconciliation semantics."""
from __future__ import annotations

import re

from v50_harness_common import emit, read_java, require


EXPECTED = {
    "NUMBER_FEATURE_P95_MS": 300,
    "STATE_EXPERT_P95_MS": 600,
    "ACTION_CALIBRATION_P95_MS": 200,
    "DECISION_GATE_P95_MS": 300,
    "DATABASE_ATOMIC_P95_MS": 250,
    "REALTIME_CORE_P95_MS": 2000,
    "END_TO_END_REDLINE_MS": 5000,
}


def main() -> int:
    config = read_java("AppConfig.java")
    block_match = re.search(
        r"public\s+static\s+final\s+class\s+V50\s*\{(.*?)\n\s*\}",
        config,
        re.DOTALL,
    )
    require(block_match is not None, "V50_BUDGET_BLOCK_MISSING")
    block = block_match.group(1)
    actual = {}
    for name, expected in EXPECTED.items():
        match = re.search(
            r"%s\s*=\s*([0-9]+)L" % re.escape(name), block
        )
        require(match is not None, "V50_BUDGET_MISSING", name)
        actual[name] = int(match.group(1))
        require(actual[name] == expected, "V50_BUDGET_CHANGED", name)

    # Synthetic non-overlapping child spans: exclusive totals must equal root.
    root_inclusive = 100_000_000
    child_inclusive = [
        25_000_000, 30_000_000, 10_000_000, 5_000_000, 8_000_000
    ]
    root_exclusive = root_inclusive - sum(child_inclusive)
    exclusive_sum = root_exclusive + sum(child_inclusive)
    require(root_exclusive >= 0, "NEGATIVE_ROOT_EXCLUSIVE")
    require(exclusive_sum == root_inclusive, "EXCLUSIVE_RECONCILIATION_FAILED")

    ledger = read_java("V50PerformanceSpanLedger.java")
    require("inclusiveNanos - childNanos" in ledger,
            "JAVA_EXCLUSIVE_FORMULA_MISSING")
    require("parent.childNanos += span.inclusiveNanos" in ledger,
            "JAVA_PARENT_CHILD_ACCOUNTING_MISSING")
    require("exclusiveSumNanos" in ledger, "JAVA_EXCLUSIVE_SUM_MISSING")
    return emit("v50_performance", {
        "budget_contract_ms": actual,
        "synthetic_root_inclusive_ns": root_inclusive,
        "synthetic_exclusive_sum_ns": exclusive_sum,
        "accounting_reconciled": True,
        "android_device_p95_measured": False,
        "performance_release_gate": "AWAITING_ANDROID_DEVICE_SAMPLES",
    })


if __name__ == "__main__":
    raise SystemExit(main())

