#!/usr/bin/env python3
"""Verify a construction receipt; never execute a command supplied by a receipt.

The tool checks identity, source changes, test coverage, measured assertions and
artifact digests. It cannot replace review of test implementations or device data.
"""
import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path
from verify_plan import verify_seal


def need(condition, message):
    if not condition:
        raise ValueError(message)


def sha(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def git(repo, *args):
    return subprocess.run(["git", "-C", str(repo), *args], check=True,
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                          text=True).stdout.strip()


def assert_measured(assertion):
    need(assertion.get("evidence_artifact"), "Assertion lacks original evidence")
    need("actual" in assertion and "expected" in assertion, "Assertion lacks measured values")
    actual, expected = assertion["actual"], assertion["expected"]
    import math
    for value in (actual, expected):
        if type(value) is float:
            need(math.isfinite(value), "Nonfinite measurement")
    op = assertion.get("operator", "eq")
    if op == "eq":
        result = type(actual) is type(expected) and actual == expected
    elif op in {"ge", "gt", "le", "lt"}:
        need(type(actual) in (int, float) and type(expected) in (int, float),
             "Numerical assertion must contain finite numeric values")
        need(math.isfinite(actual) and math.isfinite(expected), "Nonfinite measurement")
        result = {"ge": actual >= expected, "gt": actual > expected,
                  "le": actual <= expected, "lt": actual < expected}[op]
    elif op == "in":
        need(isinstance(expected, list), "Membership target must be a list")
        result = any(type(actual) is type(item) and actual == item for item in expected)
    else:
        raise ValueError("Unknown assertion operator " + str(op))
    need(result, "Measured assertion failed: " + str(assertion.get("metric")))


def product_paths(changed):
    """Reject a receipt based only on version metadata, tests or generated files.

    This is a structural guard, not proof that a code change is semantically useful.
    """
    result = set()
    for path in changed:
        if path.startswith(("app/build/", "app/src/test/", "app/src/androidTest/", "verification/", "release/")):
            continue
        if path.endswith("/ReleaseManifestR926.java") or Path(path).name.startswith("test_"):
            continue
        if path.startswith(("app/", "tools/", "scripts/", "gradle/")):
            result.add(path)
    return result


def background_window_seconds(start, end, required=259200):
    from datetime import datetime
    a, b = datetime.fromisoformat(start), datetime.fromisoformat(end)
    need(a.tzinfo is not None and b.tzinfo is not None, "Background window lacks timezone")
    seconds = (b-a).total_seconds()
    need(seconds >= required, "72h final campaign is shorter than 259200 real wall seconds")
    return seconds


def validate(plan, receipt, evidence_root, repo, level):
    spec = json.loads((plan / "WORK_ORDERS.json").read_text())
    ident = json.loads((plan / "control/PLAN_IDENTITY.json").read_text())
    seal = verify_seal(plan)
    need(receipt.get("schema") == "STARCORE_R341_EXECUTION_RECEIPT_1", "Wrong receipt schema")
    need(receipt.get("plan_manifest_sha256") == seal["manifest_sha256"], "Stale plan receipt")
    need(receipt.get("level") == level, "Receipt/evaluation levels differ")
    batches = {b["id"]: b for b in spec["batches"]}
    batch_id = receipt.get("batch")
    need(batch_id in batches, "Unknown batch")
    batch = batches[batch_id]
    issues = batch["issues"]
    need(set(receipt.get("issue_ids", [])) == {i["id"] for i in issues},
         "Batch issue coverage mismatch")
    for key in ("source_before", "source_after"):
        need(re.fullmatch(r"[0-9a-f]{40}", receipt.get(key, "")), "Missing exact source commit")
        git(repo, "cat-file", "-e", receipt[key] + "^{commit}")
    git(repo, "merge-base", "--is-ancestor", ident["application_baseline_commit"],
        receipt["source_after"])
    git(repo, "merge-base", "--is-ancestor", ident["application_baseline_commit"],
        receipt["source_before"])
    git(repo, "merge-base", "--is-ancestor", receipt["source_before"], receipt["source_after"])
    if level == "ENGINEERING":
        changed = set(git(repo, "diff", "--name-only", receipt["source_before"],
                          receipt["source_after"]).splitlines())
        product_changes = product_paths(changed)
        need(product_changes or receipt.get("revalidation_only") is True,
             "No product change: document/version status alone cannot sign implementation")
        if receipt.get("revalidation_only") is True:
            need(receipt.get("original_implementation_receipt_sha256"),
                 "Revalidation requires the prior implementation receipt identity")
    root = evidence_root.resolve()
    artifacts = {}
    for artifact in receipt.get("artifacts", []):
        relative = artifact.get("path", "")
        need(isinstance(relative, str) and relative and not Path(relative).is_absolute()
             and ".." not in Path(relative).parts, "Evidence path must be relative and contained")
        path = (root / relative).resolve()
        need(path.is_relative_to(root) and path.is_file(), "Missing or escaping evidence path")
        need(re.fullmatch(r"[0-9a-f]{64}", artifact.get("sha256", "")), "Invalid evidence hash")
        need(sha(path) == artifact["sha256"], "Evidence bytes changed: " + relative)
        need(relative not in artifacts, "Duplicate evidence entry")
        artifacts[relative] = artifact
    need(artifacts, "No evidence artifacts")
    required = {t["id"] for i in issues for t in i["acceptance"]
                if t.get("level", "ENGINEERING") == level}
    need(required, "No acceptance cases at this level; use NOT_APPLICABLE, not PASS")
    if receipt.get("revalidation_only") is True:
        need(receipt["original_implementation_receipt_sha256"] in
             {a["sha256"] for a in artifacts.values()},
             "Original implementation receipt bytes are not supplied")
    results = receipt.get("test_results", [])
    need(len(results) == len({x["id"] for x in results}), "Duplicate test result")
    need({x["id"] for x in results} == required, "Missing or surplus acceptance case")
    for result in results:
        need(result.get("status") == "PASS", "Test not passed: " + result["id"])
        need(result.get("log") in artifacts, "Test log not hash-bound")
        need(type(result.get("exit_code")) is int and result["exit_code"] == 0,
             "Test command did not succeed")
        need(isinstance(result.get("command"), list) and result["command"],
             "Missing recorded command argv")
        need(result.get("assertions"), "Exit code without measured assertions is insufficient")
        for assertion in result["assertions"]:
            need(assertion.get("evidence_artifact") in artifacts, "Unknown assertion evidence")
            assert_measured(assertion)
    if level in {"DEVICE", "NATURAL"}:
        need(re.fullmatch(r"[0-9a-f]{64}", receipt.get("apk_sha256", "") or "")
             and receipt.get("device_identity")
             and type(receipt.get("installed_version_code")) is int
             and receipt["installed_version_code"] > ident["application_baseline_version_code"],
             "Unbound or stale device evidence")
        need(receipt.get("window_start") and receipt.get("window_end"), "Missing observation window")
        from datetime import datetime
        start, end = (datetime.fromisoformat(receipt[k]) for k in ("window_start", "window_end"))
        need(start.tzinfo is not None and end.tzinfo is not None and end > start,
             "Observation window must have timezone and positive duration")
        need(receipt.get("fixture") is False, "Fixture cannot sign device/natural evidence")
    if level == "NATURAL":
        need(receipt.get("preregistration_artifact") in artifacts,
             "Natural evidence requires original preregistration")
        need(receipt.get("future_calendar_artifact") in artifacts,
             "Natural evidence requires full opportunity calendar")
        need(receipt.get("natural_evaluator_artifact") in artifacts,
             "Natural evidence requires the actual frozen qualification evaluator result")
    if level == "DEVICE" and "A-B12-04a" in required:
        background_window_seconds(receipt["window_start"], receipt["window_end"])
        ref=receipt.get("background_campaign_artifact")
        need(ref in artifacts, "72h campaign metadata original is missing")
        campaign=json.loads((root/ref).read_text())
        for key in ("apk_sha256", "device_identity", "window_start", "window_end"):
            need(campaign.get(key)==receipt[key], "Campaign/receipt identity mismatch: "+key)
        need(campaign.get("source_commit")==receipt["source_after"]
             and campaign.get("fixture") is False, "Stale or synthetic background campaign")
        for key in ("preregistered_calendar_artifact", "opportunity_ledger_artifact",
                    "boot_segments_artifact", "interventions_artifact", "raw_events_artifact",
                    "checkpoint_24h_artifact", "coverage_evaluator_artifact"):
            need(campaign.get(key) in artifacts, "Campaign original missing: "+key)
        calendar=json.loads((root/campaign["preregistered_calendar_artifact"]).read_text())
        ledger=json.loads((root/campaign["opportunity_ledger_artifact"]).read_text())
        targets=calendar.get("targets",[]); rows=ledger.get("opportunities",[])
        keys=[x["target_key"] for x in targets]; observed=[x["target_key"] for x in rows]
        need(keys and len(keys)==len(set(keys)), "Calendar is empty or duplicates targets")
        need(len(observed)==len(set(observed)) and set(keys)==set(observed),
             "Campaign hides missing target opportunities")
        need(all(x.get("terminal_state") and x.get("reason") and x.get("role") for x in rows),
             "Opportunity lacks terminal state, reason or role")
    return {"status": "PASS_RECEIPT_STRUCTURE_AND_DIGESTS", "batch": batch_id,
            "level": level, "cases": len(required),
            "boundary": "Review actual test runner and raw observations; this validator does not create qualification."}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--plan", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--receipt", type=Path, required=True)
    parser.add_argument("--evidence-root", type=Path, required=True)
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--level", choices=["ENGINEERING", "DEVICE", "NATURAL"], required=True)
    args = parser.parse_args()
    receipt = json.loads(args.receipt.read_text())
    try:
        print(json.dumps(validate(args.plan, receipt, args.evidence_root, args.repo, args.level),
                         ensure_ascii=False))
    except (ValueError, KeyError, OSError, subprocess.CalledProcessError) as error:
        raise SystemExit("FAIL_RECEIPT: " + str(error))


if __name__ == "__main__":
    main()
