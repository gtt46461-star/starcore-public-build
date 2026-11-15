"""Recompute aggregate field evidence from the two full Android exports. Standard library, read-only."""
import argparse
import hashlib
import io
import json
import zipfile
from collections import Counter
from pathlib import Path

PLAN = Path(__file__).resolve().parents[1]


def json_member(archive, name):
    with archive.open(name) as member:
        return json.load(member)


def complete_rows(table):
    rows = table["rows"]
    if not len(rows) == table["exportedRows"] == table["totalRows"]:
        raise ValueError("Partial result cannot support a period-by-period comparison: " + table["table"])
    return rows


def by_key(rows, key):
    index = {row[key]: row for row in rows}
    if len(rows) != len(index):
        raise ValueError("Duplicate source key: " + key)
    return index


def inspect(path):
    with zipfile.ZipFile(path) as outer:
        inner_names = [k for k in outer.namelist() if k.endswith("/STARCORE_FULL_REPORT.zip")]
        if len(inner_names) != 1:
            raise ValueError("Expected one full report inside " + path.name)
        packed = io.BytesIO(outer.read(inner_names[0]))
    with zipfile.ZipFile(packed) as archive:
        r26 = json_member(archive, "R26_REPORT.json")
        diagnostic = json_member(archive, "DIAGNOSTIC_SCOPE.json")
        detail = by_key(diagnostic["recentEvidence"], "table")
        draws = by_key(complete_rows(detail["draws"]), "issue")
        tasks = by_key(complete_rows(detail["v57_settlement_task_r36"]), "target_issue")
        shadow = [r for r in complete_rows(detail["v57_shadow_observation_r35"]) if r["decision_action"] == "SHADOW"]
        tables = by_key(diagnostic["tables"], "table")
        report = json_member(archive, "R30_REPORT.json")
        chapters = by_key(report["chapters"], "sectionId")
        runtime = by_key(chapters["R30_04"]["data"]["tasks"], "task_id")
        neural = chapters["R30_06"]["data"]["neuralTraining"]["payload"]
        index = report["objectIndex"]
        progress = {name: {k: runtime[name][k] for k in ("cursor", "steps", "status")}
                    for name in ("foundry-su2", "r27-evolution", "r27-replay-dataset")}
        quota_reason = "QUOTA_MISSING" in runtime["foundry-su2"]["blocked_reason"]
        projections = {}
        for row in index["logicalTables"]["objects"]:
            record = {k: row[k] for k in ("status", "shown")}
            if row["id"] in ("T06", "T10"):
                frames = [json.loads(entry["payload_json"])["frame"] for entry in row["rows"]]
                record["pageFrameReasons"] = [f.get("reason") for f in frames]
                record["pageFramePointCounts"] = [len(f["points"]) for f in frames]
            projections[row["id"]] = record
        quality = chapters["R30_01"]["commonMetrics"]
        if r26["sourceCommit"] != index["sourceCommit"]:
            raise ValueError("Mismatched source identities within a report")
        summary = {
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "sourceCommit": r26["sourceCommit"], "apkHash": r26["apkHash"],
            "snapshot": index["sourceSnapshot"], "latestIssue": report["snapshotWatermark"]["latestIssue"],
            "draws": len(draws), "shadowFrozen": len(shadow),
            "shadowSettled": sum(str(x["settled"]) == "1" for x in shadow),
            "shadowHits": sum(str(x["settled"]) == "1" and str(x["hit_label"]) == "1" for x in shadow),
            "task": progress, "foundryQuotaMissing": quota_reason,
            "hypothesisQuotaRows": tables["r31_hypothesis_quota"]["rows"],
            "candidates": tables["r26_candidate"]["rows"], "realTradeRows": tables["trades"]["rows"],
            "neural": {k: neural[k] for k in ("materializedSamples", "trainingSamplesProcessed", "onlineSamples", "publicationReady")},
            "quality": {k: quality[k] for k in ("futureActs", "futureHits", "independentFutureIssues", "activeCertificates", "policyObjects")},
            "tables": projections
        }
        return summary, set(draws), {k: v["status"] for k, v in tasks.items()}


def compare(first, second):
    earlier, earlier_issues, _ = inspect(first)
    later, later_issues, settlement = inspect(second)
    if (earlier["sourceCommit"], earlier["apkHash"]) != (later["sourceCommit"], later["apkHash"]):
        raise ValueError("Versions differ; source-scoped progress is not comparable")
    if earlier["snapshot"] >= later["snapshot"]:
        raise ValueError("Supply the earlier report first")
    added = later_issues - earlier_issues
    return {"earlier": earlier, "later": later, "addedDrawIssues": len(added),
            "addedIssueSettlement": dict(sorted(Counter(settlement.get(issue, "NO_TASK_ROW") for issue in added).items()))}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("earlier", type=Path)
    parser.add_argument("later", type=Path)
    parser.add_argument("--verify-baseline", action="store_true", help="Compare with the 032 field pair shipped in this plan")
    args = parser.parse_args()
    result = compare(args.earlier, args.later)
    if args.verify_baseline:
        expected = json.loads((PLAN / "ledger" / "field_032.json").read_text())
        if result != expected:
            raise SystemExit("Field evidence mismatch; check report identity and contents")
        print("PASS 032 field exports, object pages and period-by-period settlement")
    else:
        print(json.dumps(result, ensure_ascii=False, indent=2))
