#!/usr/bin/env python3
"""Reconcile exported metadata only; never execute or repair Android/SQLite business state."""
import argparse
import collections
import hashlib
import json
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    path = args.report / "DIAGNOSTIC_SCOPE.json"
    original = path.read_bytes()
    document = json.loads(original)
    tables = {row["table"]: (i, row) for i, row in enumerate(document["tables"])}
    metadata = {row["table"]: (i, row) for i, row in enumerate(document["recentEvidence"])}

    def full_rows(name):
        index, section = metadata[name]
        assert section["complete"] is True, name
        assert section["totalRows"] == section["exportedRows"] == len(section["rows"]), name
        return index, section["rows"]

    task_index, tasks = full_rows("v57_settlement_task_r36")
    observation_index, observations = full_rows("v57_shadow_observation_r35")
    canonical_index, canonical = full_rows("v57_draw_canonical_r811")
    draw_index, draws = full_rows("draws")
    task_status = dict(collections.Counter(row["status"] for row in tasks))
    table_task_index, table_task = tables["v57_settlement_task_r36"]
    assert task_status == {r["value"]: r["rows"] for r in table_task["states"] if r["column"] == "status"}
    observation_by_issue = {row["target_issue"]: row for row in observations}
    canonical_by_issue = {row["target_issue"]: row for row in canonical}
    draw_by_issue = {row["issue"]: row for row in draws}
    assert len(observation_by_issue) == len(observations)
    assert len(canonical_by_issue) == len(canonical)
    assert len(draw_by_issue) == len(draws)
    active = []
    for i, row in enumerate(tasks):
        if row["status"] not in ("PENDING", "RUNNING", "RETRY_WAIT"):
            continue
        target = row["target_issue"]
        observed = observation_by_issue.get(target)
        revealed = canonical_by_issue.get(target)
        active.append({
            "taskPath": f"/recentEvidence/{task_index}/rows/{i}",
            "target": target,
            "status": row["status"],
            "taskClass": row["task_class"],
            "shadowObservationPresent": observed is not None,
            "canonicalStatus": revealed["status"] if revealed else None,
            "canonicalHashMatchesTask": bool(revealed and revealed["canonical_draw_hash"] == row["draw_hash"]),
            "actualDrawInHotTable": target in draw_by_issue,
        })
    deferred_index, deferred = tables["v57_deferred_evidence_job"]
    deferred_status = {r["value"]: r["rows"] for r in deferred["states"] if r["column"] == "status"}
    result = {
        "scope": "ORIGINAL_REPORT_METADATA_RECONCILIATION_ONLY",
        "diagnosticFileSha256": hashlib.sha256(original).hexdigest(),
        "snapshotSha256": document["snapshotSha256"],
        "sourceCommit": document["sourceCommit"],
        "notCurrentReleaseOnlyCounts": True,
        "settlementTablePath": f"/tables/{table_task_index}",
        "settlementStatusCounts": task_status,
        "deferredTablePath": f"/tables/{deferred_index}",
        "deferredStatusCounts": deferred_status,
        "deferredNonterminal": sum(deferred_status.get(k, 0) for k in ("PENDING", "RUNNING", "RETRY_WAIT")),
        "joinSourcePaths": [f"/recentEvidence/{i}" for i in (task_index, observation_index, canonical_index, draw_index)],
        "activeTasks": len(active),
        "activeTaskClasses": dict(collections.Counter(row["taskClass"] for row in active)),
        "activeMissingShadowObservation": sum(not row["shadowObservationPresent"] for row in active),
        "activeConfirmedCanonicalHashMatch": sum(row["canonicalStatus"] == "CONFIRMED_CANONICAL_DRAW" and row["canonicalHashMatchesTask"] for row in active),
        "activeActualDrawPresent": sum(row["actualDrawInHotTable"] for row in active),
        "activeActualDrawMissing": sum(not row["actualDrawInHotTable"] for row in active),
        "activeRows": active,
        "boundary": "Missing hot rows do not prove that no archived original exists. No frozen prediction, settlement, task status, or archive was mutated.",
        "javaExecuted": False,
        "androidCompiled": False,
        "deviceMeasured": False,
    }
    text = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.write_text(text, encoding="utf-8")
    else:
        print(text, end="")


if __name__ == "__main__":
    main()
