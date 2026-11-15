#!/usr/bin/env python3
"""Regression gate for the V50 "no position / no K3" cold-start failure."""
from __future__ import annotations

import csv
import gzip
import re

from v50_harness_common import ROOT, emit, read_java, require


def main() -> int:
    importer = read_java("HistoricalDrawImportEngine.java")
    auto = read_java("AutoFetchService.java")
    runtime = read_java("V50RuntimeOrchestrator.java")
    gate = read_java("DecisionGateEngine.java")
    diagnosis = read_java("RealBetDiagnosisEngine.java")
    activity = read_java("MainActivity.java")

    require("V50_RUNTIME_MINIMUM_ROWS = 96" in importer,
            "V50_HISTORY_MINIMUM_CHANGED")
    require('FIRST_SEED_ISSUE = "202603010251"' in importer,
            "V50_SEED_FIRST_BOUNDARY")
    require('LAST_SEED_ISSUE = "202603110295"' in importer,
            "V50_SEED_LAST_BOUNDARY")
    require("EXPECTED_SEED_ROWS = 10000" in importer,
            "V50_SEED_ROW_CONTRACT")
    for token in (
        "SEED_ROW_COUNT_", "SEED_BOUNDARY_MISSING", "HISTORY_LT_",
        "store.getDrawByIssue(FIRST_SEED_ISSUE)",
        "store.getDrawByIssue(LAST_SEED_ISSUE)",
    ):
        require(token in importer, "V50_SEED_VALIDATION_MISSING", token)

    load_start = auto.index("private List<Models.Draw> loadAnalysisHistory()")
    load_end = auto.index("private ArrayList<Models.Draw> limitRealtimeHistory",
                          load_start)
    load_body = auto[load_start:load_end]
    require(
        re.search(
            r"required\s*=\s*AppConfig\.ENABLE_V50_PRIME\s*"
            r"\?\s*HistoricalDrawImportEngine\.V50_RUNTIME_MINIMUM_ROWS",
            load_body,
        ) is not None,
        "LEGACY_30_ROW_SHORT_CIRCUIT_STILL_REACHABLE",
    )
    require(load_body.count("ensureV50HistoryBootstrap(") >= 4,
            "V50_BOOTSTRAP_NOT_COVERING_HISTORY_EXITS")
    require("localContinuous >= required" in load_body,
            "V50_GAPPED_ROWS_CAN_SHORT_CIRCUIT_BOOTSTRAP")
    require("afterContinuous" in load_body
            and "V50_RUNTIME_MINIMUM_ROWS" in load_body,
            "V50_BOOTSTRAP_READY_WITHOUT_CONTINUOUS_96")

    require("history.size() < 12" in runtime,
            "V50_COLD_START_OUTPUT_MINIMUM_MISSING")
    require("COLD_START_SHADOW_ONLY" in runtime,
            "V50_COLD_START_SHADOW_MODE_MISSING")
    require("V50Enums.Lane.SHADOW, false" in runtime,
            "V50_COLD_START_REAL_BET_NOT_BLOCKED")
    require("INCOMPLETE_RECENT_HISTORY_" in runtime
            and "V50_RUNTIME_MINIMUM_ROWS" in runtime,
            "V50_COLD_START_REASON_MISSING")
    for token in (
        "recentContinuousCount(allHistory)",
        "currentSequence != previousSequence + 1L",
        "dayDelta != 1L",
        '\"v50_runtime_history_continuity\"',
    ):
        require(token in runtime, "V50_RECENT_CONTINUITY_GUARD_MISSING",
                token)
    for token in (
        "features = featureEngine.build(",
        "fusion = mixtureEngine.fuse(",
        "actions = ranker.rank(",
        "selected = new V50Models.SelectedActionEnvelope(",
        "ledgerEngine.persist(",
    ):
        require(token in runtime, "V50_OUTPUT_STAGE_MISSING", token)

    mapping_start = gate.index("toLegacyDecisionV50")
    mapping_end = gate.index("/**", mapping_start)
    mapping = gate[mapping_start:mapping_end]
    for token in (
        "out.position = selected.position",
        "out.n1 = selected.top3[0]",
        "out.n2 = selected.top3[1]",
        "out.n3 = selected.top3[2]",
        "out.finalP = envelope.selectedHitP",
        "out.action = authorized.action.name()",
    ):
        require(token in mapping, "V50_UI_ACTION_MAPPING_MISSING", token)

    require("return diagnoseV50(store, betLimit, shadowLimit)" in diagnosis,
            "V50_DIAGNOSIS_NOT_ROUTED")
    require("V50CohortDiagnosisEngine" in diagnosis
            and "V50LedgerDiagnosisEngine" in diagnosis
            and "V50ModelHealthDiagnosisEngine" in diagnosis,
            "V50_DIAGNOSIS_NOT_PURE_COHORT")
    require("if (AppConfig.ENABLE_V50_PRIME) return \"\";" in activity,
            "LEGACY_DYNAMIC_DIAGNOSIS_STILL_VISIBLE")

    asset = ROOT / "app" / "src" / "main" / "assets" / "history_seed.csv.gz"
    require(asset.is_file(), "V50_HISTORY_ASSET_MISSING", str(asset))
    with gzip.open(asset, "rt", encoding="utf-8-sig", newline="") as handle:
        rows = list(csv.DictReader(handle))
    valid = []
    for row in rows:
        numbers = [int(value) for value in row["open_number"].split(",")]
        if row.get("valid", "1") == "1" and sorted(numbers) == list(range(1, 11)):
            valid.append(row)
    require(len(valid) == 10000, "V50_HISTORY_ASSET_VALID_COUNT",
            str(len(valid)))
    require(valid[0]["period"] == "202603010251",
            "V50_HISTORY_ASSET_FIRST", valid[0]["period"])
    require(valid[-1]["period"] == "202603110295",
            "V50_HISTORY_ASSET_LAST", valid[-1]["period"])

    return emit("v50_runtime_output", {
        "seed_valid_rows": len(valid),
        "seed_first_issue": valid[0]["period"],
        "seed_last_issue": valid[-1]["period"],
        "full_history_minimum": 96,
        "cold_start_prediction_minimum": 12,
        "cold_start_action": "SHADOW",
        "cold_start_real_bet": False,
        "legacy_diagnosis_visible": False,
    })


if __name__ == "__main__":
    raise SystemExit(main())
