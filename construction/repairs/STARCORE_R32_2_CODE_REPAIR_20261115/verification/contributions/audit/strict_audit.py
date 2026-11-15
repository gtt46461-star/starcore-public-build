#!/usr/bin/env python3
"""Audit plan code coverage without interpreting a source copy as implementation.

This is a packaging audit, not an Android correctness test. --require-complete
fails for every declared write without explicit verified replacement/reuse proof.
"""
import argparse
import collections
import hashlib
import json
import pathlib
import re
import sys


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else None


def patch_inventory(plan):
    found = {}
    errors = []
    for patch in sorted((plan / "patches").glob("P*.patch")):
        text = patch.read_text(encoding="utf-8")
        parts = re.split(r"(?m)^diff --git a/(.+?) b/(.+?)\n", text)
        for i in range(1, len(parts), 3):
            before, path, body = parts[i:i + 3]
            if path in found:
                errors.append("multiple patch writers: " + path)
            index = re.search(r"(?m)^index ([0-9a-f]+)\.\.([0-9a-f]+)", body)
            found[path] = {
                "patch": patch.relative_to(plan).as_posix(),
                "patch_sha256": digest(patch),
                "owner": patch.stem,
                "preimage_blob_prefix": index.group(1) if index else None,
                "postimage_blob_prefix": index.group(2) if index else None,
                "hunks": re.findall(r"(?m)^@@.*@@.*$", body),
                "added_lines": sum(s.startswith("+") and not s.startswith("+++") for s in body.splitlines()),
                "deleted_lines": sum(s.startswith("-") and not s.startswith("---") for s in body.splitlines()),
                "is_new_file": "new file mode" in body,
            }
            if before != path:
                errors.append("rename requires explicit disposition: " + path)
    return found, errors


def receipt_proves(receipt, row, repo, manifest):
    """Evidence is scoped; no count/name-only proof is accepted."""
    failures = []
    required = ["source_commit", "input_sha256", "command", "started_at", "finished_at", "assertions", "log_path", "log_sha256"]
    if not isinstance(receipt, dict):
        return ["no executable evidence receipt"]
    for key in required:
        if not receipt.get(key):
            failures.append("missing receipt field " + key)
    if receipt.get("status") != "PASS" or receipt.get("exit_code") != 0:
        failures.append("receipt not an executed successful command")
    if receipt.get("evidence_tier") not in {"LOCAL", "EMU", "DEVICE"}:
        failures.append("unknown evidence tier")
    if row["path"] not in receipt.get("covered_paths", []):
        failures.append("receipt does not cover this path")
    if receipt.get("source_commit") != manifest.get("source_commit"):
        failures.append("receipt source commit differs from candidate source")
    if not isinstance(receipt.get("assertions"), list) or not all(isinstance(a, dict) and a.get("expected") is not None and a.get("actual") is not None and a.get("status") == "PASS" for a in receipt.get("assertions", [])):
        failures.append("missing concrete expected/actual assertions")
    if receipt.get("log_path"):
        log = repo / receipt["log_path"]
        if not log.is_file() or digest(log) != receipt.get("log_sha256"):
            failures.append("receipt log absent or hash mismatch")
    return failures


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--plan", required=True, type=pathlib.Path)
    parser.add_argument("--repo", required=True, type=pathlib.Path)
    parser.add_argument("--out", required=True, type=pathlib.Path)
    parser.add_argument("--implementation-manifest", type=pathlib.Path)
    parser.add_argument("--require-complete", action="store_true")
    args = parser.parse_args()
    plan, repo = args.plan.resolve(), args.repo.resolve()
    cards = read(plan / "contracts/repair_cards.json")
    batches = read(plan / "contracts/batches.json")
    owners = read(plan / "ledger/file_owners.json")
    historical_results = read(plan / "verify/RESULTS.json")
    patches, errors = patch_inventory(plan)
    by_path = {r["path"]: r for r in owners}
    declared = collections.defaultdict(list)
    write_owners = collections.defaultdict(set)
    order = [b["id"] for b in batches]
    ancestors = {}
    for i, batch in enumerate(batches):
        prereqs = batch["prerequisites"]
        if not set(prereqs).issubset(order[:i]):
            errors.append("cycle/future prerequisite: " + batch["id"])
        ancestors[batch["id"]] = set(prereqs)
        for p in prereqs:
            ancestors[batch["id"]].update(ancestors.get(p, set()))
    if len(by_path) != len(owners):
        errors.append("duplicate file owner entry")
    for card in cards:
        batch = card["batch"]
        for path in card["writes"]:
            declared[path].append(card["id"])
            write_owners[path].add(batch)
            if by_path.get(path, {}).get("owner") != batch:
                errors.append("declared writer and owner differ: " + path)
        for path in card["reads"]:
            if by_path.get(path, {}).get("owner") not in ancestors[batch]:
                errors.append("future or same-batch read falsely classified as upstream: " + card["id"] + " " + path)
    for path, batchset in write_owners.items():
        if len(batchset) > 1:
            errors.append("cross-batch write conflict: " + path)
    manifest = read(args.implementation_manifest) if args.implementation_manifest else {}
    manifest_rows = {r["path"]: r for r in manifest.get("files", [])}
    rows = []
    for path, cardids in sorted(declared.items()):
        patch = patches.get(path)
        base = by_path.get(path, {})
        current = repo / path
        row = {
            "path": path,
            "batch": next(iter(write_owners[path])) if len(write_owners[path]) == 1 else None,
            "cards": cardids,
            "declared_action": "WRITE",
            "baseline_sha256": base.get("sha256"),
            "current_source_sha256": digest(current),
            "baseline_source_exists": current.is_file(),
            "candidate_code": patch,
            "code_coverage_state": "R32_1_CANDIDATE_PATCH_PRESENT_PARTIAL_SCOPE" if patch else "R32_1_EXISTING_SOURCE_PRESERVED_REMAINING_SCOPE_UNASSESSED",
            "implementation_state": "NOT_VERIFIED",
            "test_state": "NO_PER_PATH_EXECUTION_RECEIPT",
            "proof_failures": [],
        }
        submitted = manifest_rows.get(path)
        if submitted:
            action = submitted.get("action")
            if action not in {"REPLACE", "RETAIN_VERIFIED", "DESIGN_ONLY", "UNKNOWN"}:
                row["proof_failures"].append("unknown implementation action")
            if action == "REPLACE":
                code = repo / str(submitted.get("candidate_path", ""))
                if not code.is_file() or digest(code) != submitted.get("candidate_sha256"):
                    row["proof_failures"].append("candidate code missing or hash mismatch")
            if action in {"REPLACE", "RETAIN_VERIFIED"}:
                if submitted.get("source_sha256") != row["current_source_sha256"] or not submitted.get("source_sha256"):
                    row["proof_failures"].append("receipt source bytes differ from current source")
                if action == "REPLACE" and submitted.get("candidate_sha256") == row["baseline_sha256"]:
                    row["proof_failures"].append("unchanged baseline copy cannot be a replacement")
                if not submitted.get("semantic_scope") or not submitted.get("problem_ids"):
                    row["proof_failures"].append("missing scoped implementation/reuse decision")
                row["proof_failures"] += receipt_proves(submitted.get("receipt"), row, repo, manifest)
                if not row["proof_failures"]:
                    row["implementation_state"] = "VERIFIED_REPLACEMENT" if action == "REPLACE" else "VERIFIED_RETENTION"
                    row["test_state"] = "EXECUTED_PASS_SCOPED"
            else:
                row["proof_failures"].append("explicit unfinished action")
        else:
            row["proof_failures"].append("no explicit per-file implementation/reuse proof")
        rows.append(row)
    by_batch = []
    for batch in order:
        subset = [r for r in rows if r["batch"] == batch]
        by_batch.append({"batch": batch, "declared_write_paths": len(subset), "patch_paths": sum(bool(r["candidate_code"]) for r in subset), "without_candidate_code": sum(not r["candidate_code"] for r in subset), "verified_paths": sum(r["implementation_state"].startswith("VERIFIED_") for r in subset)})
    unverified = [r["path"] for r in rows if not r["implementation_state"].startswith("VERIFIED_")]
    result = {
        "schema_version": 1,
        "scope": "R32_1_DECLARED_WRITE_CODE_DELIVERY_AND_EVIDENCE_AUDIT_ONLY",
        "rules": {"baseline_source_copy_counts_as_implementation": False, "patch_presence_proves_complete_feature": False, "nonempty_test_description_counts_as_execution": False, "structural_dag_proves_runtime_closure": False},
        "structural_errors": errors,
        "declared_unique_write_paths": len(rows),
        "candidate_patch_paths": len(patches),
        "unverified_declared_write_paths": len(unverified),
        "without_candidate_patch": sum(not r["candidate_code"] for r in rows),
        "by_batch": by_batch,
        "code_complete": not errors and not unverified,
        "android_gradle_apk": historical_results.get("java", {}).get("gradleApkBuild", "UNKNOWN"),
        "android_runtime": historical_results.get("androidRuntime", "UNKNOWN"),
        "natural_quality": historical_results.get("naturalQuality", "UNKNOWN"),
        "caveat": "This audit describes R32.1's patch delivery, not 95 missing implementations. Existing source can already implement some or all requirements. A path without patch preserves existing source; its remaining scope has not been adjudicated here. A path with patch can still have unfinished behavior outside its hunks. Per-file proof absence is not evidence of product failure.",
        "files": rows,
    }
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: result[k] for k in ["scope", "declared_unique_write_paths", "candidate_patch_paths", "without_candidate_patch", "unverified_declared_write_paths", "code_complete", "by_batch", "structural_errors"]}, ensure_ascii=False))
    return 2 if args.require_complete and not result["code_complete"] else 0


if __name__ == "__main__":
    sys.exit(main())
