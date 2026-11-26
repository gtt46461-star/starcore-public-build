#!/usr/bin/env python3
"""Exercise the current sealed-plan verifier with one valid and eight invalid copies.

The plan and source repository are read-only inputs. All mutations occur in a
temporary directory under --out. Semantic negative cases are validly resealed
before invoking the unmodified copied tools/verify_plan.py. This verifies only
the plan verifier contract; it does not execute Android or strategy acceptance.
"""
import argparse
import ast
import hashlib
import json
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path


MANIFEST = "MANIFEST_SHA256.json"
MANIFEST_SCHEMA = "STARCORE_R343_PLAN_MANIFEST_1"
SCHEMA = "STARCORE_R343_PLAN_REJECTION_TEST_1"


def require(ok, message):
    if not ok:
        raise AssertionError(message)


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def inventory(root, include_manifest=True):
    rows = []
    for path in sorted(root.rglob("*")):
        if not path.is_file() or "__pycache__" in path.parts or path.suffix in (".pyc", ".pyo"):
            continue
        name = path.relative_to(root).as_posix()
        if not include_manifest and name == MANIFEST:
            continue
        require(not path.is_symlink(), "fixture contains symlink: " + name)
        raw = path.read_bytes()
        rows.append({"path": name, "bytes": len(raw), "sha256": sha(raw)})
    return rows


def inventory_hash(rows):
    return sha(json.dumps(rows, sort_keys=True, separators=(",", ":")).encode("utf-8"))


def source_identity(verifier):
    constants = {}
    for node in ast.parse(verifier.read_text(encoding="utf-8")).body:
        if isinstance(node, ast.Assign):
            for target in node.targets:
                if isinstance(target, ast.Name) and target.id in ("BASE", "REPORT"):
                    constants[target.id] = ast.literal_eval(node.value)
    require(set(constants) == {"BASE", "REPORT"}, "verifier must declare BASE and REPORT")
    require(all(isinstance(v, str) for v in constants.values()), "verifier identity constants must be strings")
    return constants


def seal(root, baseline):
    rows = inventory(root, include_manifest=False)
    value = {"schema": MANIFEST_SCHEMA, "source_commit": baseline,
             "file_count": len(rows), "files": rows}
    write_json(root / MANIFEST, value)
    return value


def seal_is_valid(root, baseline):
    value = read_json(root / MANIFEST)
    rows = inventory(root, include_manifest=False)
    return (value.get("schema") == MANIFEST_SCHEMA and
            value.get("source_commit") == baseline and
            value.get("file_count") == len(rows) and value.get("files") == rows)


def git(repo, *args):
    return subprocess.run(["git", "--no-optional-locks", "-C", str(repo), *args],
                          check=True, capture_output=True).stdout


def invoke(root, repo, destination, forbidden_output=False):
    destination.mkdir(parents=True, exist_ok=True)
    output = root / "negative_test_output.json" if forbidden_output else destination / "validator.json"
    argv = [sys.executable, str(root / "tools/verify_plan.py"), "--plan", str(root),
            "--source-repo", str(repo), "--out", str(output)]
    started = time.monotonic()
    proc = subprocess.run(argv, capture_output=True, timeout=180)
    (destination / "stdout.txt").write_bytes(proc.stdout)
    (destination / "stderr.txt").write_bytes(proc.stderr)
    write_json(destination / "argv.json", argv)
    try:
        result = json.loads(proc.stdout.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError):
        result = None
    return {"returncode": proc.returncode, "result": result,
            "stderr": proc.stderr.decode("utf-8", errors="replace"),
            "elapsed_seconds": round(time.monotonic() - started, 6),
            "stdout_sha256": sha(proc.stdout), "stderr_sha256": sha(proc.stderr),
            "output_written": output.exists()}


def mutate(root, kind, identity, repo):
    if kind == "member_tamper":
        path = root / "README.md"
        before = sha(path.read_bytes())
        path.write_bytes(path.read_bytes() + b"\nNEGATIVE_TEST_MEMBER_TAMPER\n")
        return {"path": "README.md", "before_sha256": before, "after_sha256": sha(path.read_bytes())}
    if kind == "missing_original_report":
        path = root / "report/ORIGINAL_DIAGNOSTIC_173.zip"
        detail = {"path": path.relative_to(root).as_posix(), "removed_sha256": sha(path.read_bytes())}
        path.unlink()
        return detail
    if kind == "extra_member":
        name = "NEGATIVE_TEST_UNMANIFESTED.txt"
        require(not (root / name).exists(), "extra-member fixture name collision")
        (root / name).write_text("An unexpected plan member.\n", encoding="utf-8")
        return {"path": name}
    if kind == "missing_diagnostic_check":
        path = root / "coverage/CHECK_ACTIONS_96.json"
        value = read_json(path)
        removed = value["checks"].pop(0)
        value["count"] = len(value["checks"])
        write_json(path, value)
        return {"path": path.relative_to(root).as_posix(), "removed_id": removed["id"],
                "remaining_checks": len(value["checks"])}
    if kind == "invalid_issue_source_anchor":
        path = root / "ISSUES.json"
        value = read_json(path)
        issue = next(q for q in value["issues"] if any(s.get("anchor") for s in q["source"]))
        source = next(s for s in issue["source"] if s.get("anchor"))
        replacement = "__R343_NEGATIVE_ANCHOR_MUST_NOT_EXIST_742be659d3734ccf__"
        baseline = git(repo, "show", identity["BASE"] + ":" + source["path"])
        require(replacement.encode() not in baseline, "negative source anchor unexpectedly exists")
        old = source["anchor"]
        source["anchor"] = replacement
        write_json(path, value)
        return {"path": "ISSUES.json", "issue_id": issue["id"], "source_path": source["path"],
                "unchanged_source_sha256": source["source_sha256"], "old_anchor": old,
                "new_anchor": replacement}
    if kind == "incorrect_cumulative_before":
        path = root / "PATCHSET.json"
        value = read_json(path)
        previous = {}
        for group in value["groups"]:
            for row in group["files"]:
                name = row["path"]
                if name in previous:
                    require(row["before_sha256"] == previous[name], "baseline cumulative chain already invalid")
                    before_path = root / "patches" / group["id"] / "before" / name
                    original = before_path.read_bytes()
                    replacement = original + b"\n// NEGATIVE_TEST_WRONG_CUMULATIVE_BEFORE\n"
                    before_path.write_bytes(replacement)
                    row["before_sha256"] = sha(replacement)
                    row["before_bytes"] = len(replacement)
                    write_json(path, value)
                    require(sha(before_path.read_bytes()) == row["before_sha256"],
                            "individual before-file hash must be valid")
                    return {"path": "PATCHSET.json", "group": group["id"], "source_path": name,
                            "previous_group_after_sha256": previous[name],
                            "wrong_but_locally_valid_before_sha256": row["before_sha256"],
                            "individual_before_file_hash_valid": True}
                previous[name] = row["after_sha256"]
        raise AssertionError("no repeated source file available for cumulative-before fixture")
    if kind == "missing_engineering_case":
        path = root / "ACCEPTANCE_MATRIX.json"
        value = read_json(path)
        index = next(i for i, case in enumerate(value["cases"]) if case["level"] == "ENGINEERING")
        removed = value["cases"].pop(index)
        value["count"] = len(value["cases"])
        value["engineering_count"] = sum(c["level"] == "ENGINEERING" for c in value["cases"])
        write_json(path, value)
        return {"path": "ACCEPTANCE_MATRIX.json", "removed_id": removed["id"],
                "remaining_engineering": value["engineering_count"], "remaining_total": value["count"]}
    if kind == "output_inside_sealed_plan":
        return {"attempted_output": "negative_test_output.json", "mutation": "CLI --out only"}
    raise AssertionError("unknown fixture kind: " + kind)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--plan", type=Path, required=True)
    parser.add_argument("--source-repo", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True, help="external output directory")
    args = parser.parse_args()
    plan, repo, out = args.plan.resolve(), args.source_repo.resolve(), args.out.resolve()
    require(plan.is_dir() and repo.is_dir(), "plan and source repository must exist")
    require(not out.is_relative_to(plan) and not out.is_relative_to(repo),
            "test output must be outside both read-only inputs")
    out.mkdir(parents=True, exist_ok=True)
    original_inventory = inventory(plan)
    source_status = git(repo, "status", "--porcelain=v1", "--untracked-files=all")
    identity = source_identity(plan / "tools/verify_plan.py")
    require(git(repo, "rev-parse", identity["BASE"]).decode().strip() == identity["BASE"],
            "source repository lacks verifier baseline")
    result = {"schema": SCHEMA, "status": "RUNNING", "source_commit": identity["BASE"],
              "original_report_sha256": identity["REPORT"],
              "verifier_sha256": sha((plan / "tools/verify_plan.py").read_bytes()),
              "test_script_sha256": sha(Path(__file__).read_bytes()),
              "input_plan_inventory_sha256": inventory_hash(original_inventory),
              "scope": "Unmodified verify_plan.py CLI: sealed positive plus eight isolated negative fixtures; semantic negatives validly resealed.",
              "android": "NOT_RUN", "physical_device": "NOT_RUN", "natural_quality": "NOT_PROVEN",
              "cases": []}
    specifications = [
        ("member_tamper", False, "MEMBER_HASH:README.md"),
        ("missing_original_report", True, "ORIGINAL_DIAGNOSTIC_173.zip"),
        ("extra_member", False, "MEMBER_SET"),
        ("missing_diagnostic_check", True, "CHECK_SET"),
        ("invalid_issue_source_anchor", True, "ISSUE_SOURCE_ANCHOR:"),
        ("incorrect_cumulative_before", True, "CUMULATIVE_BEFORE:"),
        ("missing_engineering_case", True, "ACCEPTANCE_COUNT"),
        ("output_inside_sealed_plan", False, "OUTPUT_MUST_BE_OUTSIDE_SEALED_PLAN"),
    ]
    try:
        with tempfile.TemporaryDirectory(prefix="plan-rejection-fixtures-", dir=out) as temporary:
            temporary = Path(temporary)
            base = temporary / "sealed_positive"
            shutil.copytree(plan, base)
            require(inventory(base) == original_inventory, "input changed while snapshot was copied")
            sealed = seal(base, identity["BASE"])
            require(seal_is_valid(base, identity["BASE"]), "positive fixture seal invalid")
            destination = out / "cases" / "sealed_positive"
            positive = invoke(base, repo, destination)
            write_json(destination / "fixture_manifest.json", sealed)
            positive_case = {"id": "sealed_positive", "expected": "ACCEPTED", "resealed": True,
                             "manifest_valid_before_invocation": True, **positive}
            result["cases"].append(positive_case)
            require(positive["returncode"] == 0 and positive["result"] and
                    positive["result"]["status"] == "PLAN_INTEGRITY_AND_SCOPED_HOST_CHECKS_PASSED" and
                    positive["result"].get("cumulative_git_apply") == "PASS",
                    "sealed positive did not pass the full source/patch contract")
            positive_case["status"] = "PASS"
            for kind, reseal, error in specifications:
                fixture = temporary / kind
                shutil.copytree(base, fixture)
                detail = mutate(fixture, kind, identity, repo)
                if reseal:
                    seal(fixture, identity["BASE"])
                valid_seal = seal_is_valid(fixture, identity["BASE"])
                expected_valid_seal = reseal or kind == "output_inside_sealed_plan"
                require(valid_seal == expected_valid_seal, "unexpected fixture seal state: " + kind)
                before = inventory(fixture)
                destination = out / "cases" / kind
                actual = invoke(fixture, repo, destination, kind == "output_inside_sealed_plan")
                write_json(destination / "fixture_manifest.json", read_json(fixture / MANIFEST))
                write_json(destination / "mutation.json", detail)
                case = {"id": kind, "expected": "REJECTED", "expected_error_token": error,
                        "resealed": reseal, "manifest_valid_before_invocation": valid_seal,
                        "mutation": detail, **actual}
                result["cases"].append(case)
                actual_error = actual["result"].get("error", "") if isinstance(actual["result"], dict) else actual["stderr"]
                require(actual["returncode"] != 0 and error in actual_error,
                        "negative rejected at wrong gate or was accepted: " + kind + ": " + actual_error)
                if kind == "missing_original_report":
                    require(actual["result"].get("type") == "FileNotFoundError", "missing report used wrong rejection")
                elif kind != "output_inside_sealed_plan":
                    require(actual["result"].get("status") == "REJECTED" and
                            actual["result"].get("type") == "ValueError", "missing structured rejection: " + kind)
                if kind == "output_inside_sealed_plan":
                    require(not actual["output_written"], "verifier wrote into sealed plan")
                    require(inventory(fixture) == before, "forbidden output altered sealed fixture")
                require(sha((fixture / "tools/verify_plan.py").read_bytes()) == result["verifier_sha256"],
                        "verifier bytes changed in fixture")
                case["status"] = "PASS"
            result["positive_accepted"] = 1
            result["negative_rejected_at_expected_gate"] = len(specifications)
            result["semantic_rejections_after_valid_reseal"] = sum(reseal for _, reseal, _ in specifications)
        require(inventory(plan) == original_inventory, "original plan changed during test")
        require(git(repo, "status", "--porcelain=v1", "--untracked-files=all") == source_status,
                "source repository status changed during test")
        result["input_plan_inventory_unchanged"] = True
        result["source_repository_status_unchanged"] = True
        result["status"] = "PASS"
    except Exception as exc:
        result["status"] = "FAIL"
        result["test_error"] = {"type": type(exc).__name__, "message": str(exc)}
    write_json(out / "result.json", result)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result["status"] == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())
