#!/usr/bin/env python3
"""Create an isolated construction checkout from the exact installed-source branch.

Imports only the new plan and evidence paths from an explicit plan commit.
Never merges main application source into the installed-source branch.
Existing worktrees are inspected, not reset. No credentials are printed.
"""
import argparse
import json
import re
import subprocess
from pathlib import Path

BASELINE = "5562f9b19aa09f9f7d9e69ca085db56c537967fc"
PLAN_PATH = "construction/plans/STARCORE_R34_1_REPAIR_20261124"
EVIDENCE_PATH = "construction/evidence/STARCORE_R34_1_20261124"
IMPORTS = [PLAN_PATH, EVIDENCE_PATH, "CURRENT_FINAL_PLAN.md", "STARCORE_最终施工入口.md"]


def git(repo, *args):
    return subprocess.run(["git", "-C", str(repo), *args], text=True,
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True).stdout.strip()


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--repo", type=Path, required=True)
    p.add_argument("--plan-ref", required=True, help="Exact verified plan commit SHA")
    p.add_argument("--worktree", type=Path, required=True)
    p.add_argument("--branch", default="construction/r341-repair-20261124")
    p.add_argument("--create", action="store_true", help="Explicitly create; default is read-only preflight")
    a = p.parse_args()
    if not re.fullmatch(r"[0-9a-f]{40}", a.plan_ref):
        p.error("--plan-ref must be an exact commit SHA, never a moving main alias")
    git(a.repo, "cat-file", "-e", BASELINE + "^{commit}")
    git(a.repo, "cat-file", "-e", a.plan_ref + ":" + PLAN_PATH + "/control/PLAN_IDENTITY.json")
    identity = json.loads(git(a.repo, "show", a.plan_ref + ":" + PLAN_PATH + "/control/PLAN_IDENTITY.json"))
    if identity["application_baseline_commit"] != BASELINE:
        raise SystemExit("FAIL: plan/source baseline mismatch")
    target = a.worktree.resolve()
    if target.exists():
        head = git(target, "rev-parse", "HEAD")
        git(target, "merge-base", "--is-ancestor", BASELINE, head)
        print(json.dumps({"status": "EXISTING_WORKTREE_INSPECTED_NOT_RESET", "head": head,
                          "next": "Inspect execution receipts and continue the first uncompleted batch."}))
        return
    if not a.create:
        print(json.dumps({"status": "PREFLIGHT_OK_NO_MUTATION", "baseline": BASELINE,
                          "plan_commit": a.plan_ref, "imports": IMPORTS}))
        return
    git(a.repo, "check-ref-format", "--branch", a.branch)
    git(a.repo, "worktree", "add", "-b", a.branch, str(target), BASELINE)
    git(target, "restore", "--source=" + a.plan_ref, "--staged", "--worktree", "--", *IMPORTS)
    changed = git(target, "diff", "--cached", "--name-only").splitlines()
    for path in changed:
        if not any(path == allowed or path.startswith(allowed + "/") for allowed in IMPORTS):
            raise SystemExit("FAIL: unexpected imported product path " + path)
    print(json.dumps({"status": "WORKTREE_CREATED_PLAN_STAGED", "worktree": str(target),
                      "branch": a.branch, "product_baseline": BASELINE,
                      "next": "Commit the plan-only import, run B01, and preserve the original signing identity."}))


if __name__ == "__main__":
    main()
