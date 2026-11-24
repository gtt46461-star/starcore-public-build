#!/usr/bin/env python3
"""Inspect or prepare a construction checkout at the installed source lineage."""
import argparse
import hashlib
import json
import os
import re
import subprocess
import tempfile
from verify_plan import verify_seal
from pathlib import Path

BASELINE = "5562f9b19aa09f9f7d9e69ca085db56c537967fc"
PLAN_PATH = "construction/plans/STARCORE_R34_1_REPAIR_20261124"
EVIDENCE_PATH = "construction/evidence/STARCORE_R34_1_20261124"
REPORT_PATH = EVIDENCE_PATH + "/raw/STARCORE-9325298a3e033715226cf1126404c5a5273a6928f88.zip"
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


def common_directory(repo):
    return Path(field(repo, "rev-parse", "--path-format=absolute", "--git-common-dir")).resolve()


def import_tree(repo, rev):
    tree = {}
    for entry in paths(repo, "ls-tree", "-r", "-z", rev, "--", *IMPORTS):
        meta, name = entry.split("\t", 1)
        mode, kind, oid = meta.split()
        need(mode in {"100644", "100755"} and kind == "blob", "Nonregular import: " + name)
        tree[name] = (mode, oid)
    return tree


def verify_plan_commit(repo, tree, expected_manifest):
    # Read the selected Git objects, even if a different plan is checked out at --repo.
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        for name, (_, oid) in tree.items():
            if name.startswith(PLAN_PATH + "/"):
                relative = Path(name).relative_to(PLAN_PATH)
                need("__pycache__" not in relative.parts and relative.suffix != ".pyc",
                     "Compiled Python cache is not a sealed plan input: " + name)
                target = root / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(git(repo, "cat-file", "blob", oid).stdout)
        seal = verify_seal(root)
        facts = json.loads((root / "evidence/BASELINE_FACTS.json").read_bytes())
        need(REPORT_PATH in tree, "Expected baseline report is missing from the selected commit")
        report = git(repo, "cat-file", "blob", tree[REPORT_PATH][1]).stdout
        need(hashlib.sha256(report).hexdigest() == facts["report_outer_sha256"],
             "Baseline report bytes differ from the sealed report identity")
        identity = json.loads((root / "control/PLAN_IDENTITY.json").read_bytes())
        need(identity["application_baseline_commit"] == BASELINE, "Source baseline differs")
        need(expected_manifest is None or expected_manifest == seal["manifest_sha256"],
             "Selected plan commit has a different manifest from the requested local seal")
        return seal["manifest_sha256"]


def owned_before(repo, ref, before, wanted):
    """Find the previous plan import; evidence added by builders remains outside its ownership."""
    def match(tree):
        return (all(before.get(k) == v for k, v in tree.items()) and
                all(name in tree or name.startswith(EVIDENCE_PATH + "/") for name in before))

    if match(wanted):
        return wanted
    baseline_tree = import_tree(repo, BASELINE)
    if match(baseline_tree):
        return baseline_tree
    # A product branch commits the selective tree import; the main commit itself
    # need not be its ancestor. Compare to actual earlier published import trees.
    history = field(repo, "rev-list", ref, "--", *IMPORTS).splitlines()
    for revision in history:
        if revision == ref:
            continue
        tree = import_tree(repo, revision)
        if match(tree):
            return tree
    raise ValueError("Committed import paths differ from the previous published plan. Preserve and reconcile local edits")


def overlaps(a, b):
    return a == b or a.startswith(b + "/") or b.startswith(a + "/")


def prepare(repo, target, ref, branch, create, expected_manifest=None):
    repo, target = repo.resolve(), target.resolve()
    need(re.fullmatch(r"[a-f0-9]{40}", ref), "Supply the exact plan commit")
    git(repo, "check-ref-format", "--branch", branch)
    for revision in (BASELINE, ref):
        git(repo, "cat-file", "-e", revision + "^{commit}")
    wanted = import_tree(repo, ref)
    for member in IMPORTS:
        need(any(k == member or k.startswith(member + "/") for k in wanted), "Missing import: " + member)
    manifest = verify_plan_commit(repo, wanted, expected_manifest)
    identity = {"plan_commit": ref, "manifest_sha256": manifest}
    existing = target.exists() and (not target.is_dir() or any(target.iterdir()))
    branch_ref = "refs/heads/" + branch
    checkout_exists = branch_ref in field(repo, "for-each-ref", "--format=%(refname)", branch_ref).splitlines()
    if existing:
        need(target.is_dir() and Path(field(target, "rev-parse", "--show-toplevel")).resolve() == target,
             "Target is not the root of a Git checkout")
        need(common_directory(target) == common_directory(repo), "Target belongs to a different repository")
        need(field(target, "symbolic-ref", "--short", "HEAD") == branch, "Different checked-out branch")
    head = field(repo, "rev-parse", branch_ref) if checkout_exists else BASELINE
    git(repo, "merge-base", "--is-ancestor", BASELINE, head)
    before = import_tree(repo, head)
    previous = owned_before(repo, ref, before, wanted)
    extra = before.keys() - previous.keys()
    for path in extra:
        for incoming in wanted:
            if overlaps(incoming, path):
                need(incoming == path and wanted[incoming] == before[path],
                     "Published update collides with construction evidence: " + path)
    controlled = previous.keys() | wanted.keys()
    changed = sorted(name for name in controlled if before.get(name) != wanted.get(name))
    if existing:
        staged = set(paths(target, "diff", "--cached", "--no-renames", "--name-only", "-z"))
        unstaged = set(paths(target, "diff", "--no-renames", "--name-only", "-z", "--", *IMPORTS))
        # Default ls-files --others includes ignored paths. Never overwrite an ignored local original.
        others = paths(target, "ls-files", "--others", "-z", "--", *IMPORTS)
        for path in others:
            relative = Path(path)
            if path.startswith(PLAN_PATH + "/"):
                need("__pycache__" in relative.parts or relative.suffix == ".pyc",
                     "Untracked plan member would break the seal: " + path)
            need(not any(overlaps(path, c) for c in changed), "Untracked local path collides with import: " + path)
        if not changed and not ((staged | unstaged) & controlled):
            verify_seal(target / PLAN_PATH)
            return {"status": "EXISTING_PLAN_VERIFIED", **identity, "head": head}
        if not create:
            return {"status": "PLAN_IMPORT_REQUIRED", **identity, "committed_differences": len(changed)}
        need(not (unstaged & controlled), "Keep local edits to import paths before updating")
        if staged:
            need(not (staged - set(changed)), "Staged changes include unrelated or product files; preserve them separately")
            pending = paths(target, "diff", "--cached", "--no-renames", "--name-only", "-z", ref, "--", *IMPORTS)
            need(not (set(pending) & controlled), "Staged import has locally edited bytes")
            verify_seal(target / PLAN_PATH)
            return {"status": "PLAN_ALREADY_STAGED_VERIFY_AND_COMMIT", **identity}
    elif not create:
        return {"status": "PREFLIGHT_OK_NO_MUTATION", "baseline": BASELINE, **identity, "imports": IMPORTS}
    else:
        if checkout_exists:
            git(repo, "worktree", "add", str(target), branch)
        else:
            git(repo, "worktree", "add", "-b", branch, str(target), BASELINE)
    if not changed:
        verify_seal(target / PLAN_PATH)
        return {"status": "EXISTING_PLAN_VERIFIED", **identity, "head": head}
    # Passing individually owned file paths leaves extra construction evidence intact.
    git(target, "--literal-pathspecs", "restore", "--source=" + ref, "--staged", "--worktree", "--", *changed)
    staged = set(paths(target, "diff", "--cached", "--no-renames", "--name-only", "-z"))
    need(staged == set(changed), "Staged import differs from the selected path delta")
    pending = paths(target, "diff", "--cached", "--no-renames", "--name-only", "-z", ref, "--", *IMPORTS)
    need(not (set(pending) & controlled), "Staged import differs from the selected Git objects")
    verify_seal(target / PLAN_PATH)
    return {"status": "PLAN_IMPORT_STAGED", **identity, "branch": branch, "head": head,
            "next": "Review and commit the plan-only import"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--plan-ref", required=True)
    parser.add_argument("--expected-manifest-sha256", help="Optional seal digest of the intended plan")
    parser.add_argument("--worktree", type=Path, required=True)
    parser.add_argument("--branch", default="construction/r341-repair-20261124")
    parser.add_argument("--create", action="store_true", help="Create or update an isolated checkout")
    args = parser.parse_args()
    try:
        result = prepare(args.repo, args.worktree, args.plan_ref, args.branch, args.create, args.expected_manifest_sha256)
    except (OSError, ValueError, KeyError, subprocess.CalledProcessError) as exc:
        raise SystemExit(f"FAIL_WORKTREE: {exc}")
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
