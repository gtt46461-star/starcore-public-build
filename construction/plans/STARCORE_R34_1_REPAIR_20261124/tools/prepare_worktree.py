#!/usr/bin/env python3
"""Inspect or prepare a construction checkout at the installed source lineage."""
import argparse
import json
import os
import re
import subprocess
from pathlib import Path

BASELINE = "5562f9b19aa09f9f7d9e69ca085db56c537967fc"
PLAN_PATH = "construction/plans/STARCORE_R34_1_REPAIR_20261124"
EVIDENCE_PATH = "construction/evidence/STARCORE_R34_1_20261124"
IMPORTS = [PLAN_PATH, EVIDENCE_PATH, "CURRENT_FINAL_PLAN.md", "STARCORE_最终施工入口.md"]


def git(repo, *args):
    return subprocess.run(["git", "-C", str(repo), *args], check=True, stdout=subprocess.PIPE)


def field(repo, *args):
    return os.fsdecode(git(repo, *args).stdout).strip()


def paths(repo, *args):
    return [os.fsdecode(x) for x in git(repo, *args).stdout.split(b"\0") if x]


def need(condition, message):
    if not condition:
        raise ValueError(message)


def imported_path(path):
    return any(path == member or path.startswith(member + "/") for member in IMPORTS)


def common_directory(repo):
    return Path(field(repo, "rev-parse", "--path-format=absolute", "--git-common-dir")).resolve()


def prepare(repo, target, ref, branch, create):
    repo, target = repo.resolve(), target.resolve()
    need(re.fullmatch(r"[a-f0-9]{40}", ref), "Supply the exact plan commit")
    git(repo, "check-ref-format", "--branch", branch)
    for revision in (BASELINE, ref):
        git(repo, "cat-file", "-e", revision + "^{commit}")
    for member in IMPORTS:
        git(repo, "cat-file", "-e", ref + ":" + member)
    identity = json.loads(git(repo, "show", f"{ref}:{PLAN_PATH}/control/PLAN_IDENTITY.json").stdout)
    need(identity["application_baseline_commit"] == BASELINE, "Source baseline differs")
    existing = target.exists() and (not target.is_dir() or any(target.iterdir()))
    if existing:
        need(target.is_dir() and Path(field(target, "rev-parse", "--show-toplevel")).resolve() == target,
             "Target is not the root of a Git checkout")
        need(common_directory(target) == common_directory(repo), "Target belongs to a different repository")
        need(field(target, "symbolic-ref", "--short", "HEAD") == branch, "Different checked-out branch")
        git(target, "merge-base", "--is-ancestor", BASELINE, "HEAD")
        changed = paths(target, "diff", "--no-renames", "--name-only", "-z", ref, "HEAD", "--", *IMPORTS)
        local = paths(target, "status", "--porcelain=v1", "-z", "--untracked-files=all", "--", *IMPORTS)
        if not changed and not local:
            return {"status": "EXISTING_PLAN_VERIFIED", "plan_commit": ref, "head": field(target, "rev-parse", "HEAD")}
        if not create:
            return {"status": "PLAN_IMPORT_REQUIRED", "committed_differences": len(changed), "local_entries": len(local)}
        staged_matches = not paths(target, "diff", "--cached", "--name-only", "-z", ref, "--", *IMPORTS)
        unstaged = paths(target, "diff", "--name-only", "-z", "--", *IMPORTS)
        extras = paths(target, "ls-files", "--others", "--exclude-standard", "-z", "--", *IMPORTS)
        if staged_matches and not unstaged and not extras:
            staged = paths(target, "diff", "--cached", "--no-renames", "--name-only", "-z")
            need(all(imported_path(p) for p in staged), "Staged changes include product files. Preserve and review them separately")
            return {"status": "PLAN_ALREADY_STAGED_VERIFY_AND_COMMIT", "plan_commit": ref}
        need(not paths(target, "status", "--porcelain=v1", "-z", "--untracked-files=all"),
             "Existing checkout has local changes. Preserve them before importing the revised plan")
    elif not create:
        return {"status": "PREFLIGHT_OK_NO_MUTATION", "baseline": BASELINE, "plan_commit": ref, "imports": IMPORTS}
    else:
        refs = field(repo, "for-each-ref", "--format=%(refname)", "refs/heads/" + branch).splitlines()
        if "refs/heads/" + branch in refs:
            git(repo, "merge-base", "--is-ancestor", BASELINE, "refs/heads/" + branch)
            git(repo, "worktree", "add", str(target), branch)
        else:
            git(repo, "worktree", "add", "-b", branch, str(target), BASELINE)
    git(target, "restore", "--source=" + ref, "--staged", "--worktree", "--", *IMPORTS)
    paths_changed = paths(target, "diff", "--cached", "--no-renames", "--name-only", "-z")
    need(all(imported_path(p) for p in paths_changed),
         "Inspect staged paths before committing. An unrelated path is staged")
    need(not paths(target, "diff", "--cached", "--name-only", "-z", ref, "--", *IMPORTS),
         "Staged plan import differs from the requested tree")
    return {"status": "PLAN_IMPORT_STAGED", "plan_commit": ref, "branch": branch,
            "head": field(target, "rev-parse", "HEAD"), "next": "Review and commit the plan-only import"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--plan-ref", required=True)
    parser.add_argument("--worktree", type=Path, required=True)
    parser.add_argument("--branch", default="construction/r341-repair-20261124")
    parser.add_argument("--create", action="store_true", help="Create or update an isolated checkout")
    args = parser.parse_args()
    try:
        result = prepare(args.repo, args.worktree, args.plan_ref, args.branch, args.create)
    except (OSError, ValueError, KeyError, subprocess.CalledProcessError) as exc:
        raise SystemExit(f"FAIL_WORKTREE: {exc}")
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
