"""Verify that each normative file card has a scheduled, traceable receipt."""
import hashlib
import json
from pathlib import Path


def read(file):
    return json.loads(file.read_bytes())


def need(ok, message):
    if not ok:
        raise ValueError(message)


def verify(root):
    wiring = read(root / "control/FILE_VERIFICATION.json")
    counts = read(root / "control/SCOPE_COUNTS.json")
    cards = {f["production_path"]: f for f in read(root / "FILE_CARDS.json")["files"]}
    cases = read(root / "ACCEPTANCE_MATRIX.json")["cases"]
    orders = read(root / "WORK_ORDERS.json")["batches"]
    issues = {i["id"]: i for b in orders for i in b["issues"]}
    probes = {r["key"]: r for r in wiring["probes"]}
    need(len(wiring["probes"]) == len(probes), "Duplicate file verification")
    harnesses = set(wiring["harnesses"])
    need(not harnesses.intersection(probes) and set(probes) | harnesses == set(cards),
         "File cards and scheduled validation differ")
    owned = {f["path"] for i in issues.values() for f in i["files"]}
    need(owned == set(cards), "Orphan planned file or file card")
    expected_harnesses = {f["path"] for i in issues.values() for f in i["files"]
                          if f.get("kind") == "ACCEPTANCE_HARNESS_CONTRACT"}
    need(harnesses == expected_harnesses and harnesses <=
         {c["test_contract"].get("path") for c in cases}, "Unbound test runner")
    seen = set()
    for case in cases:
        rows = case.get("inherited_path_probes", [])
        need([r["key"] for r in rows] == case.get("file_probe_keys", []), "Case path list differs")
        for r in rows:
            key = r["key"]
            target = probes[key]
            body = {k: r[k] for k in ("key", "effective_path", "probe")}
            digest = hashlib.sha256(json.dumps(dict(case=case["id"], **body), ensure_ascii=False,
                                    sort_keys=True, separators=(",", ":")).encode()).hexdigest()
            paths = {f["path"] for f in issues[case["issue_id"]]["files"]}
            need(all(target[k] == v for k, v in r.items()) and target["case"] == case["id"] and
                 case["level"] == "ENGINEERING" and target["issue"] == case["issue_id"] and
                 key not in seen and key in paths and r["contract_sha256"] == digest and
                 r["effective_path"] == (cards[key]["effective_replacement"] or key), "File/case mismatch: " + key)
            seen.add(key)
    missing = sum(f["original_gap"] for f in probes.values())
    need(seen == set(probes) and missing == counts["field4_formerly_unmapped"] ==
         wiring["previous_unmapped_count"] == 73, "Incomplete coverage of prior gaps")
    identity = read(root / "evidence/FIELD3_IDENTITY.json")
    asset = read(root / "evidence/production-baseline.json")
    need(asset["job"] == identity["job_id"] and asset["source_commit"] == identity["source_commit"]
         and asset["snapshot"] == identity["snapshot_sha256"] and asset["source_sha256"] and
         all({m["name"]:m["sha256"] for m in identity["members"]}[k] == v
             for k,v in asset["source_sha256"].items()), "Asset baseline origin differs")
    need(all(f.get("sha256") and f.get("git_blob_sha") for f in
             read(root / "evidence/SOURCE_MANIFEST.json")["files"]), "Incomplete source byte verification")
    all_checks = read(root / "control/REVIEW_CONTRACTS.json")["checks"]
    updates = [x for x in all_checks if x["id"].startswith("F4-")]
    computed = dict(field4_work_items=len(updates), field4_affected_issues=len({x["issue"] for x in updates}),
                    field4_affected_cases=len({x["case"] for x in updates}), field4_bound_file_probes=len(seen),
                    field4_harness_paths=len(harnesses), field4_formerly_unmapped=missing)
    need(all(counts[k] == v for k,v in computed.items()), "Field 4 scope totals differ")
    return computed
