"""Check that every review obligation is executable from its existing batch."""
import json
from collections import defaultdict


def read(path):
    return json.loads(path.read_bytes())


def require(ok, reason):
    if not ok:
        raise ValueError(reason)


def verify(root):
    scope = read(root / "control/SCOPE_COUNTS.json")
    document = read(root / "control/REVIEW_CONTRACTS.json")
    ledger = document["checks"]
    order = read(root / "WORK_ORDERS.json")
    issues = {i["id"]: i for batch in order["batches"] for i in batch["issues"]}
    matrix = {c["id"]: c for c in read(root / "ACCEPTANCE_MATRIX.json")["cases"]}
    cards = {c["production_path"]: c for c in read(root / "FILE_CARDS.json")["files"]}
    source = {s["path"]: s for s in read(root / "evidence/SOURCE_MANIFEST.json")["files"]}
    parts = {i["id"]: i for f in ("runtime", "learning", "delivery")
             for b in read(root / "parts" / (f + ".json"))["batches"] for i in b["issues"]}
    expected_issues = defaultdict(list)
    expected_cases = defaultdict(list)
    ids = set()
    for r in ledger:
        key = r["id"]
        require(key not in ids, "Duplicate review item " + key)
        ids.add(key)
        issue, case = issues[r["issue"]], matrix[r["case"]]
        require(r["batch"] == issue["batch"] == case["batch"] and case["issue_id"] == issue["id"]
                and case["level"] == "ENGINEERING", "Wrong review owner or test level: " + key)
        require(r["files"] and r["implementation"] and r["acceptance"], "Empty work or test: " + key)
        require((root / r["spec"]).is_file() and key in (root / "batches" / (r["batch"] + ".md")).read_text(),
                "Missing written batch contract " + key)
        expected_issues[issue["id"]].append(key)
        expected_cases[case["id"]].append(key)
        for f in r["files"]:
            owned = [x for x in issue["files"] if x["path"] == f]
            require(len(owned) == 1 and any(key in c for c in owned[0]["changes"]), "File missing from issue: " + f)
            card = cards[f]
            require(issue["id"] in card["issue_ids"] and r["batch"] in card["ordered_batches"], "Wrong file card " + f)
            if card["before_state"] == "EXISTING_122":
                require(source[f]["git_blob_sha"] == card["before_git_blob_sha"] and
                        source[f]["sha256"] == card["before_sha256"], "Inconsistent source identity " + f)
    for issue_id, issue in issues.items():
        checks = expected_issues[issue_id]
        require(checks == issue.get("required_review_ids", []) == parts[issue_id].get("required_review_ids", []),
                "Issue review lists disagree " + issue_id)
    for case_id, c in matrix.items():
        require(c.get("required_review_ids", []) == expected_cases[case_id], "Unenforced review ID " + case_id)
    sealed = read(root / "evidence/REPORT_COVERAGE.json")
    count_fields = dict(objects=165, needs=8, checks=96, metrics=44, followups=9, diagnostic_rules=16, k3_layers=9)
    require(sealed["counts"] == count_fields and all(len(sealed[k]) == n for k, n in count_fields.items()),
            "Original report groups do not reconcile")
    require(document["report_sha256"] == sealed["outer_sha256"] and document["application_baseline"]
            == sealed["installed_source"] == order["source_baseline"], "Review identities disagree")
    totals = dict(additional_review_obligations=len(ids), review_affected_issues=sum(bool(x) for x in expected_issues.values()),
                  review_affected_cases=sum(bool(x) for x in expected_cases.values()), review_rows_from_original_report=sum(count_fields.values()))
    require(all(scope[k] == v for k, v in totals.items()) and scope["file_cards"] == len(cards)
            and scope["method_contracts"] == len(read(root / "evidence/SOURCE_ANCHORS.json")["anchors"]),
            "Published scope counts disagree")
    from field_direction_routes import check_field3
    return dict(totals, **check_field3(root))
