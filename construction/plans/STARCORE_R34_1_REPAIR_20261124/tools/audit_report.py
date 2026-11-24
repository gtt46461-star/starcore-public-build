#!/usr/bin/env python3
"""Read the original 122 report, preserve evidence scopes, and produce deterministic facts.

This verifies archive identity and extracts diagnostics. It does not execute Android
or certify model quality. No file is extracted through an untrusted archive path.
"""
import argparse
import collections
import hashlib
import io
import json
import re
import zipfile
from pathlib import Path, PurePosixPath

BASELINE = "5562f9b19aa09f9f7d9e69ca085db56c537967fc"
OUTER_SHA256 = "2b984d2b249f54e4363f7c02c5d2470e7b9393e6f3994b24e25bcead3e44c37d"
INNER_SHA256 = "49cb7b11d15da5b5d6c5d2e73d47310d3e3fc895fb698e58f28571c5080196a0"
MAX_UNCOMPRESSED = 256 * 1024 * 1024


def sha(data):
    return hashlib.sha256(data).hexdigest()


def checked_members(archive):
    infos = archive.infolist()
    names = [x.filename for x in infos]
    if len(names) != len(set(names)):
        raise ValueError("Duplicate ZIP member")
    if sum(x.file_size for x in infos) > MAX_UNCOMPRESSED:
        raise ValueError("Uncompressed report exceeds this contract")
    for name in names:
        path = PurePosixPath(name)
        if path.is_absolute() or ".." in path.parts:
            raise ValueError("Unsafe ZIP member")
    bad = archive.testzip()
    if bad:
        raise ValueError("ZIP CRC failed: " + bad)
    return infos


def read_report(path):
    outer_bytes = path.read_bytes()
    if sha(outer_bytes) != OUTER_SHA256:
        raise ValueError("Not the original 122 field report; use a new evidence epoch")
    with zipfile.ZipFile(io.BytesIO(outer_bytes)) as outer:
        checked_members(outer)
        candidates = [n for n in outer.namelist() if n.endswith("/STARCORE_FULL_REPORT.zip")]
        indices = [n for n in outer.namelist() if n.endswith("/index.json")]
        if len(candidates) != 1 or len(indices) != 1:
            raise ValueError("Ambiguous outer report")
        inner_bytes = outer.read(candidates[0])
        index = json.loads(outer.read(indices[0]))
    if sha(inner_bytes) != INNER_SHA256 or index["files"][0]["sha256"] != INNER_SHA256:
        raise ValueError("Inner archive identity mismatch")
    with zipfile.ZipFile(io.BytesIO(inner_bytes)) as inner:
        infos = checked_members(inner)
        members = {i.filename: inner.read(i.filename) for i in infos if not i.is_dir()}
    return index, members


def summarize(index, members):
    def doc(name):
        return json.loads(members[name])

    source = doc("source-manifest.json")
    if source["sourceCommit"] != BASELINE:
        raise ValueError("Source baseline mismatch")
    r30 = doc("R30_REPORT.json")
    scope = doc("DIAGNOSTIC_SCOPE.json")
    upgrade = doc("UPGRADE_DIAGNOSIS.json")["capabilityRepairR32"]
    matrices = doc("STRATEGY_MATRICES.json")
    semantic = doc("R31_SEMANTIC_RECEIPT.json")
    runtime = doc("runtime-observation.json")
    tables = {row["table"]: row["rows"] for row in scope["tables"]}
    selected = [
        "draws", "r34_s0_shadow", "r34_cold_rebirth", "r34_cold_rebirth_retry",
        "r34_atomnet_optimizer_step", "r34_atomnet_unavailable_label",
        "r30r1_analysis_run", "r30r1_chart_cache", "r30r1_research_measure",
        "r30r1_proposal", "r30r1_consume", "research_matrix_snapshot_r930",
        "su2_matrix_snapshot", "r27_chart_cache", "r30_writer_hold",
        "r30_atomnet_sample", "r31_atomnet_sample_stage", "r31_atomnet_publication"
    ]
    table_facts = {
        name: {"state": "PRESENT" if name in tables else "MISSING_SCHEMA",
               "rows": tables.get(name)}
        for name in selected
    }
    checks = []
    for line in members["REPORT_DETAILS.txt"].decode().splitlines():
        if line.startswith('{"kind":"check_result"'):
            checks.append(json.loads(line))
    check_facts = [
        {k: c[k] for k in ("id", "execution_state", "domain_outcome", "health",
                           "reason", "evidence", "completes_contract") if k in c}
        for c in checks if c["id"] in {
            "RSEC-003", "RSEC-004", "RSEC-012", "RSEC-027", "RSEC-030",
            "RSEC-031", "RSEC-033", "RSEC-052", "RSEC-305", "RSEC-315"
        }
    ]
    charts = r30["objectIndex"]["researchCharts"]["objects"]
    chart_counts = collections.Counter(
        (str(x.get("currentSourceProducedFrames")), str(x.get("actualSourceConsumptionReceipts")))
        for x in charts
    )
    followup = r30["chapters"][11]["fieldFollowupR34"]
    heartbeat = members["runtime-observation-heartbeatSummary.txt"].decode()
    match = re.search(r"(?:^|\|)startAt=(\d+)", heartbeat)
    process_start = int(match.group(1)) if match else None
    important_evidence = {}
    for row in scope["recentEvidence"]:
        if row["table"] in {
            "v57_runtime_epoch_r925", "v57_runtime_event_r926", "research_long_head_r80",
            "v57_schema_migration_journal_r926", "v57_schema_domain_r926",
            "v57_research_task_r967"
        }:
            selected_rows = row["rows"]
            selection = "ALL_ROWS_AS_SUPPLIED_IN_REPORT"
            time_window_event_counts = None
            if row["table"] == "v57_runtime_event_r926" and process_start is not None:
                selected_rows = [x for x in selected_rows
                                 if int(x.get("created_at") or 0) >= process_start]
                time_window_event_counts = dict(collections.Counter(
                    x["event_type"] for x in selected_rows))
                fetch_types = {"FETCH", "FETCH_BINDING_R82", "FETCH_CANONICAL_BINDING_R82"}
                business = [x for x in selected_rows if x["event_type"] not in fetch_types]
                for event_type in sorted(fetch_types):
                    business.extend([x for x in selected_rows if x["event_type"] == event_type][-3:])
                selected_rows = sorted(business, key=lambda x: int(x["seq"]))
                selection = ("CREATED_AT_GE_OBSERVED_PROCESS_START;"
                             "ALL_NON_FETCH_EVENTS_PLUS_LAST_THREE_OF_EACH_FETCH_TYPE;"
                             "VERIFY_EPOCH_AND_RELEASE_BEFORE_ATTRIBUTING_TO_CURRENT_PROCESS")
            if row["table"] == "v57_schema_migration_journal_r926":
                latest = {}
                for item in selected_rows:
                    key = item["domain"]
                    old = latest.get(key)
                    if old is None or int(item.get("completed_at") or 0) > int(old.get("completed_at") or 0):
                        latest[key] = item
                selected_rows = sorted(latest.values(), key=lambda x: x["domain"])
                selection = "LATEST_RECORDED_COMPLETION_PER_DOMAIN;MAY_PREDATE_CURRENT_RELEASE"
            important_evidence[row["table"]] = {
                "source_scope": row.get("scope"),
                "total_rows_in_input": len(row["rows"]),
                "selection": selection,
                "time_window_event_counts_before_projection": time_window_event_counts,
                "selected_rows": len(selected_rows),
                "rows": selected_rows
            }
    details_header = members["REPORT_DETAILS.txt"].decode().splitlines()[:11]
    return {
        "schema": "STARCORE_R341_BASELINE_FACTS_1",
        "scope": "STATIC_REPORT_EXTRACTION_NOT_ANDROID_REEXECUTION",
        "source_commit": BASELINE,
        "report_outer_sha256": OUTER_SHA256,
        "report_inner_sha256": INNER_SHA256,
        "original_members": len(members),
        "report_header": details_header,
        "archive_contract": index,
        "checks_health_counts": dict(collections.Counter(
            x["health"] for x in semantic["checks"].values())),
        "overall_acceptance": r30["overallAcceptance"],
        "natural_quality": r30["naturalQuality"],
        "physical_device": r30["physicalDevice"],
        "snapshot_watermark": {k: v for k, v in r30["snapshotWatermark"].items()
                               if not isinstance(v, (dict, list))},
        "runtime_observation": runtime,
        "observed_process_start_wall_ms": process_start,
        "background_capabilities": doc("runtime-observation-backgroundRuntimeCapabilityR31.txt"),
        "table_count": len(tables),
        "table_facts": table_facts,
        "current_research_charts": {
            "registered": len(charts),
            "produced_consumed_distribution": [
                {"producedFrames": key[0], "consumptionReceipts": key[1], "objects": value}
                for key, value in sorted(chart_counts.items())
            ]
        },
        "matrix_inventory": matrices["matrixInventoryR34"],
        "operating_summary": upgrade["operatingSummary"],
        "r34_field_followup": followup,
        "selected_checks": check_facts,
        "selected_original_rowsets": important_evidence,
        "checkpoint_audit": source["r26CheckpointAuditR32"],
        "storage_census_original": members["STORAGE_CENSUS.txt"].decode().splitlines()[:2],
        "quality_targets": [
            {"id": m["metricId"], "name": m["name"],
             "definition": m["definition"], "target": m.get("r31Definition", {}).get("target"),
             "minimumSample": m.get("r31Definition", {}).get("minimumSample"),
             "scope": m.get("r31Definition", {}).get("minimumSampleScope"),
             "missingReason": m.get("missingReason")}
            for m in r30["metricResults"] if m["metricId"].startswith("Q")
        ],
        "boundaries": [
            "R34-B12 current process and retained historical rows must be separated.",
            "S0 shadow is distinct from mainline canonical and REAL.",
            "NULL or unavailable source is not numeric zero.",
            "Observed/check-executed is not engineering or natural-quality pass.",
            "Raw database, installed APK and numerical model binaries are omitted by the input report.",
            "Archive member integrity does not establish causal or statistical correctness."
        ],
        "member_hashes": {n: {"bytes": len(b), "sha256": sha(b)}
                          for n, b in sorted(members.items())}
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--out", type=Path)
    parser.add_argument("--compare", type=Path)
    args = parser.parse_args()
    index, members = read_report(args.input)
    facts = summarize(index, members)
    if args.compare and json.loads(args.compare.read_text()) != facts:
        raise SystemExit("FAIL: saved facts differ from exact input")
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(json.dumps(facts, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps({"status": "PASS_REPORT_EXTRACTION", "members": len(members),
                      "source_commit": BASELINE, "table_count": facts["table_count"],
                      "application_validation": "NOT_RUN"}, ensure_ascii=False))


if __name__ == "__main__":
    main()
