#!/usr/bin/env python3
"""Verify the R34.1 crosswalk against the exact original device report and plan."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re


COUNTS = dict(objects=165, needs=8, checks=96, metrics=44, followups=9,
              diagnostic_rules=16, k3_layers=9)
OBJECT_GROUPS = ("capabilities", "researchCharts", "legacyCharts", "matrices", "logicalTables")
MEMBERS = ("R30_REPORT.json", "UPGRADE_DIAGNOSIS.json", "R31_SEMANTIC_RECEIPT.json",
           "ARCHIVE_MANIFEST.jsonl")
# Inputs in K3FailureDiagnosisEngine.diagnose / diagnoseSnapshotCP4 at 122,
# Git blob 70244ff822a12b7bd5f93e4f53b6831075bc768b. The ZIP records the layer outcomes.
LAYER_INPUTS = dict(NUMBER="artifact(V51NumberModelArtifact)",
                    RANK="artifact(V51ActionRankerArtifact)", THIRD="v51_third_boundary_summary",
                    WINDOW="b05_window_summary", REGIME="b05_state_summary",
                    MOTIF="b06_motif_summary", RELATION="b06_relation_summary",
                    CONSENSUS="v51_consensus_summary", CALIBRATION="artifact(V51SelectedCalibrationArtifact)")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, f"Duplicate JSON key {key}")
        result[key] = value
    return result


def read_json(path):
    return json.loads(path.read_bytes(), object_pairs_hook=unique_object)


def index(rows, key, label):
    result = {}
    for row in rows:
        identity = row[key]
        require(identity not in result, f"{label}: duplicate {identity}")
        result[identity] = row
    return result


def same(actual, expected, label):
    # bool, null and numeric zero have different meanings throughout the report.
    normal = lambda x: json.dumps(x, ensure_ascii=False, sort_keys=True, allow_nan=False)
    require(normal(actual) == normal(expected), f"{label}: differs from original")


def check_catalog(members, receipt):
    contracts, results = {}, {}
    for name in sorted(n for n in members if re.fullmatch(r"catalog-\d{4}\.jsonl", n)):
        for line in members[name].splitlines():
            row = json.loads(line)
            kind = row.get("kind")
            if kind not in {"check_contract", "check_result"}:
                continue
            result = contracts if kind == "check_contract" else results
            key = row["id"]
            require(key not in result, f"Catalog {kind}: duplicate {key}")
            result[key] = (row, name)
    ids = set(receipt["checks"])
    require(ids == set(results) == set(contracts), "Catalog contract/result/semantic ID mismatch")
    projected = []
    for key in ids:
        contract, _ = contracts[key]
        result, name = results[key]
        semantic = receipt["checks"][key]
        for field in ("domain_outcome", "health", "execution_state", "completes_contract"):
            same(result[field], semantic[field], f"{key}.{field}")
        projected.append(dict(id=key, source=name, title=contract["section"],
                              report_owner=contract["source_path"] + "#" + contract["source_method"],
                              health=semantic["health"], result=semantic["domain_outcome"]))
    return projected, results["RSEC-066"][0]["evidence"]


def project(plan, members):
    report = json.loads(members["R30_REPORT.json"])
    receipt = json.loads(members["R31_SEMANTIC_RECEIPT.json"])
    upgrade = json.loads(members["UPGRADE_DIAGNOSIS.json"])["capabilityRepairR32"]
    sources = report["objectIndex"]
    object_keys = [group + ":" + obj["id"] for group in OBJECT_GROUPS for obj in sources[group]["objects"]]
    cases = index(upgrade["improvementCasesR335"]["cases"], "needKey", "cases")
    contracts = index(read_json(plan / "specs/RESEARCH_OBJECT_CONTRACTS.json")["objects"], "key", "object contracts")
    observed_rows = index([dict(key=r["group"] + ":" + r["id"], gap=r["firstKnownGap"])
                           for r in upgrade["rows"]], "key", "object observations")
    require(len(object_keys) == len(set(object_keys)) == COUNTS["objects"], "Object identities")
    require(set(object_keys) == set(cases) == set(contracts) == set(observed_rows), "Object coverage")
    sections = {"objects": [], "metrics": [], "followups": [], "k3_layers": []}
    for key in object_keys:
        case, contract = cases[key], contracts[key]
        same(key, case["group"] + ":" + case["objectId"], key + ".group/objectId")
        same(case["firstObservedGap"], observed_rows[key]["gap"], key + ".gap")
        same(contract["source_evidence"]["object_hash"], case["sourceObjectSha256"], key + ".objectHash")
        sections["objects"].append(dict(key=key, gap=case["firstObservedGap"], caseKey=case["caseKey"],
                                        occurrenceId=case["occurrenceId"], verify=case["verificationEntry"],
                                        work_orders=contract["work_orders"]))
    sections["needs"] = [dict(id=r["needKey"], title=r["title"], classification=r["classification"],
                              positive=r["positiveVerification"], negative=r["negativeVerification"],
                              lookup=r["closestExistingObjects"])
                         for r in upgrade["capabilityNeedDiagnosticsR335"]["needResults"]]
    for row in report["metricResults"]:
        definition = row["r31Definition"]
        values = {k: definition.get(k) for k in ("target", "minimumSample", "minimumSampleScope")}
        sections["metrics"].append(dict(id=row["metricId"], name=row["name"], status=row["status"],
                                        samples=row["sampleCount"], value=row["value"], **values))
    inherited = index(read_json(plan / "R34_CROSSWALK.json")["requirements"], "legacy_id", "R34 crosswalk")
    for row in report["chapters"][11]["fieldFollowupR34"]["sources"]:
        count = None if row["state"] == "SOURCE_UNAVAILABLE" else len(row["rows"])
        sections["followups"].append(dict(id=row["issueId"], state=row["state"], row_count=count,
                                          source_reason=row.get("reason"),
                                          work_orders=inherited[row["issueId"]]["current_issue_ids"]))
    sections["diagnostic_rules"] = [dict(id=r["id"], status=r["status"], field=r["evidence"]["field"],
                                         value=r["evidence"]["value"]) for r in report["diagnosis"]["findings"]]
    sections["checks"], evidence = check_catalog(members, receipt)
    pattern = r"^\d+\.([A-Z]+)\|([A-Z_]+)\|confidence=[^\n]*\n(?:[^\n]+\n)*?Hash=([0-9a-f]{64})$"
    for layer, state, digest in re.findall(pattern, evidence, re.MULTILINE):
        sections["k3_layers"].append(dict(id=layer, baseline_input=LAYER_INPUTS[layer],
                                          baseline_state=state, section_finding_hash=digest))
    return sections, receipt, report


def verify(plan, archive, coverage):
    spec = importlib.util.spec_from_file_location("r341_original_report", plan / "tools/audit_report.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    outer, members = module.read_report(archive)
    data = read_json(coverage)
    expected, receipt, report = project(plan, members)
    same(data["schema"], "R341_REPORT_ACCEPTANCE_CROSSWALK_2", "schema")
    same(data["counts"], COUNTS, "declared counts")
    same(data["members"], {k: hashlib.sha256(members[k]).hexdigest() for k in MEMBERS}, "member digests")
    same(data["outer_sha256"], module.OUTER_SHA256, "outer SHA")
    same(data["report_job"], outer["jobId"], "job")
    same(data["report_job"], receipt["jobId"], "semantic job")
    same(data["installed_source"], receipt["sourceCommit"], "source")
    same(data["snapshot"], receipt["snapshotSha256"], "snapshot")
    same(data["natural_quality"], report["naturalQuality"], "natural quality")
    require(report["physicalDevice"] == "NOT_RUN_OR_RECEIPT_UNAVAILABLE" and data["device_acceptance"] == "NOT_RUN",
            "Device acceptance has no verifying receipt")
    work_orders = read_json(plan / "WORK_ORDERS.json")
    valid = {i["id"] for b in work_orders["batches"] for i in b["issues"]}
    for group, primary_count in COUNTS.items():
        primary = "key" if group == "objects" else "id"
        actual = index(data[group], primary, group)
        baseline = index(expected[group], primary, "original " + group)
        require(len(actual) == primary_count and set(actual) == set(baseline), f"{group}: missing/extra IDs")
        for key, preserved in baseline.items():
            row = actual[key]
            for field, value in preserved.items():
                require(field in row, f"{group}.{key}.{field}: missing")
                same(row[field], value, f"{group}.{key}.{field}")
            routes = row["work_orders"]
            require(isinstance(routes, list) and routes and set(routes) <= valid
                    and len(set(routes)) == len(routes), f"{group}.{key}: invalid work order")
    return {"status": "PASS_REPORT_COVERAGE", "counts": COUNTS,
            "report_job": outer["jobId"], "application_validation": "NOT_RUN"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--plan", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--input", type=Path, required=True, help="original outer ZIP")
    parser.add_argument("--coverage", type=Path, required=True, help="report crosswalk JSON")
    args = parser.parse_args()
    try:
        print(json.dumps(verify(args.plan, args.input, args.coverage), sort_keys=True))
    except (ValueError, KeyError, IndexError, TypeError, OSError) as error:
        parser.exit(1, f"FAIL_REPORT_COVERAGE: {error}\n")


if __name__ == "__main__":
    main()
