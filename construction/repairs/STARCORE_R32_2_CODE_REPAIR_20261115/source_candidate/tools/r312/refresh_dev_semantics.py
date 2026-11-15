#!/usr/bin/env python3
"""Apply the fixed S00 eight-path review through the original strict R967/R82 contracts.

No release-manifest writer, signing, r982 freeze, qualification inheritance,
policy change, validator change, or automatic review of unknown bytes is used.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
REVIEW = ROOT / "tools/r312/semantic_review.json"
METADATA = ("release/research_dependencies_r967.json", "release/r82_training_invalidation.json")
ALLOWED = {"app/build.gradle", "app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java"} | {
    "app/src/main/java/com/ethk3/novaonepro/strategyv2/" + name + ".java"
    for name in ("FailureDispositionR31", "FeatureCacheR27", "MemoryBudgetR31", "ReportR27",
                 "ReportSnapshotR30", "WriterHoldLedgerR30")}


def sha(data):
    return hashlib.sha256(data).hexdigest()


def encoded(value):
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode()


def git(*args):
    return subprocess.check_output(["git", "-C", str(ROOT), *args])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--inspect", action="store_true", help="Validate fixed source pins; report required refresh without writes")
    mode.add_argument("--check", action="store_true", help="Require the already-refreshed metadata and original generated outputs")
    mode.add_argument("--apply", action="store_true", help="Write only reviewed metadata and original R967 generated outputs")
    args = parser.parse_args()
    review = json.loads(REVIEW.read_text())
    if review["format"] != "R312_FIXED_DEV_SEMANTIC_REVIEW_V1" or review["qualification_inheritance"] is not False:
        raise ValueError("R312_REVIEW_SCOPE")
    pins = {v["path"]: v for v in review["source_reviews"]}
    if set(pins) != ALLOWED or len(pins) != len(review["source_reviews"]):
        raise ValueError("R312_REVIEW_MUST_BE_EXACT_EIGHT_PATHS")
    base = review["baseline_commit"]
    subprocess.run(["git", "-C", str(ROOT), "merge-base", "--is-ancestor", base, "HEAD"], check=True)
    for name, expected in review["protected_sha256"].items():
        if sha((ROOT / name).read_bytes()) != expected:
            raise ValueError("R312_PROTECTED_POLICY_IDENTITY_BASELINE_OR_VALIDATOR_CHANGED:" + name)
    for name, row in pins.items():
        if sha(git("show", base + ":" + name)) != row["before_sha256"]:
            raise ValueError("R312_REVIEW_BASELINE_MISMATCH:" + name)
        if sha((ROOT / name).read_bytes()) != row["after_sha256"]:
            raise ValueError("R312_UNREVIEWED_SOURCE_BYTES:" + name)
        if sha(git("diff", "--unified=2", base, "--", name)) != row["diff_sha256"]:
            raise ValueError("R312_REVIEW_DIFF_MISMATCH:" + name)
    spec = importlib.util.spec_from_file_location("r312_original_r967", ROOT / "scripts/r967_semantic_identity.py")
    semantic = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(semantic)
    before = {name: git("show", base + ":" + name) for name in METADATA}
    if any(sha(before[name]) != review["metadata_before_sha256"][name] for name in METADATA):
        raise ValueError("R312_INITIAL_REVIEWED_METADATA_CHANGED")
    manifest = semantic.identity_json(before[METADATA[0]])
    proof = semantic.identity_json(before[METADATA[1]])
    if proof["predecessors"] != [] or proof["decision"] != "INVALIDATE_OLD_RULE":
        raise ValueError("R312_QUALIFICATION_INHERITANCE_FORBIDDEN")
    classified = {v["path"]: v for v in manifest["files"]}
    actual = semantic.controlled_paths(ROOT)
    if actual != set(classified) or len(classified) != len(manifest["files"]):
        raise ValueError("R312_UNREVIEWED_CONTROLLED_PATH_OR_ROLE")
    blobs = {}
    for entry in git("ls-tree", "-rz", base).split(b"\0"):
        if entry:
            meta, name = entry.split(b"\t", 1)
            blobs[name.decode()] = meta.decode().split()[2]
    hashes = {}
    for name in sorted(actual):
        data = (ROOT / name).read_bytes()
        hashes[name] = sha(data)
        if name not in ALLOWED and name not in METADATA:
            blob = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
            if blobs.get(name) != blob:
                raise ValueError("R312_NON_WHITELIST_SOURCE_CHANGE:" + name)
        if name in pins:
            row = classified[name]
            if row["role"] != pins[name]["role"]:
                raise ValueError("R312_ROLE_CHANGE_FORBIDDEN:" + name)
            if row["role"] in ("NUMERIC", "BUILD_CONTRACT") and name.endswith(".java") and semantic.DYNAMIC.search(data.decode()):
                row["dynamic_review_sha256"] = hashes[name]
    old = {v["path"]: v for v in semantic.identity_json((ROOT / proof["controlled_baseline_file"]).read_bytes())["files"]}
    previous = {v["path"]: v for v in proof["controlled_changes"]}
    changes = []
    for name in sorted(set(old) | actual):
        b, a = old.get(name, {}), classified.get(name, {})
        br, ar = b.get("role", ""), a.get("role", "")
        if br == "PROVENANCE" and ar == "PROVENANCE" or not br and ar == "PROVENANCE":
            continue
        bh, ah = b.get("sha256", ""), hashes.get(name, "")
        if (bh, br) == (ah, ar):
            continue
        if name in pins:
            pin = pins[name]
            changes.append({"path": name, "before_sha256": bh, "after_sha256": ah,
                            "before_role": br, "after_role": ar,
                            "reviewer": pin["reviewer"], "rationale": pin["rationale"]})
        else:
            prior = previous.get(name)
            if not prior or (prior["before_sha256"], prior["after_sha256"], prior["before_role"], prior["after_role"]) != (bh, ah, br, ar):
                raise ValueError("R312_EXISTING_CONTROLLED_REVIEW_DOES_NOT_MATCH:" + name)
            changes.append(prior)
    roles = sha(semantic.canonical(sorted([p, v["role"]] for p, v in classified.items())).encode())
    if roles != proof["current_controlled_roles_sha256"]:
        raise ValueError("R312_CONTROLLED_ROLE_CHANGE_FORBIDDEN")
    proof["controlled_changes"] = changes
    proof["reviewed_input_provenance_sha256"] = sha(semantic.canonical(
        [[p, hashes[p]] for p in sorted(actual) if classified[p]["role"] != "PROVENANCE"]).encode())
    # Runtime identity, immutable baselines, policy, new-role reviews and [] predecessors stay exact.
    desired = {METADATA[0]: encoded(manifest), METADATA[1]: encoded(proof)}
    for name in METADATA:
        if (ROOT / name).read_bytes() not in (before[name], desired[name]):
            raise ValueError("R312_UNKNOWN_METADATA_CHANGE:" + name)
    pending = [p for p in METADATA if (ROOT / p).read_bytes() != desired[p]]
    if args.inspect:
        print(json.dumps({"state": "FIXED_EIGHT_SOURCE_PINS_VALIDATED_NO_WRITES", "sourcePaths": 8,
                          "metadataPending": pending, "qualificationInheritance": False}, indent=2))
        return
    if args.check:
        if pending:
            raise ValueError("R312_REVIEWED_METADATA_REFRESH_REQUIRED")
        semantic.verify_outputs(ROOT)
        return
    targets = [*METADATA, semantic.GENERATED, semantic.REPORT]
    backup = {p: (ROOT / p).read_bytes() if (ROOT / p).is_file() else None for p in targets}
    try:
        for name in METADATA:
            (ROOT / name).write_bytes(desired[name])
        semantic.write_outputs(ROOT)
        semantic.verify_outputs(ROOT)
        subprocess.run([sys.executable, str(ROOT / "scripts/r967_semantic_identity.py"), "--self-test"], check=True)
    except BaseException:
        for name, data in backup.items():
            if data is None:
                (ROOT / name).unlink(missing_ok=True)
            else:
                (ROOT / name).write_bytes(data)
        raise
    print("R312_REVIEWED_DEV_SEMANTICS_REFRESHED_NO_RELEASE_FREEZE_NO_PREDECESSOR_QUALIFICATION")


if __name__ == "__main__":
    main()
