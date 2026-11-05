#!/usr/bin/env python3
"""R31 source/plan identity. Never modifies the source tree or imports credentials.

The plan's 190 blobs are audit anchors, not a buildable source tree. This tool
requires the complete Git checkout, verifies the frozen blobs against Git, and
reports current differences without silently changing their expected hashes.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys
import zipfile

BASELINE = "5237b265a27fe5ad27fd842901c445d5f8c27749"
PLAN_REL = "construction/STARCORE_R31_FINAL_20261104"
PLAN_REVISION = "R31.2-SEQUENTIAL-20261105"
PLAN_MEMBER_COUNT = 452
ORIGINAL_PLAN_MEMBER_COUNT = 405
PLAN_AUTHORIZATION_COMMIT = "f2d9275fefb1f2ebb3608414790e5cafd58d230d"
PLAN_MANIFEST_SHA256 = "b32ba63a1a7aafda83b978d9ea2c4576b045620cabd81651777aa69bf00e8b85"
ORIGINAL_PLAN_REL = "construction/archives/STARCORE_R31_FINAL_20261104"
ORIGINAL_PLAN_REVISION = "R31_FINAL_20261104"
ORIGINAL_PLAN_MANIFEST_SHA256 = "13f34908b624c74e2852ce8f944b99f8978348820aab23f83043fab1d72753ce"
PRESERVED = {
    "evidence/derived/legacy_requirements_1832.csv": (1832, "6e9efda4947e8b3df96a98325b74000573e56d9fc4f63ef412115902a0930735"),
    "evidence/derived/legacy_file_cards_299.csv": (299, "0eacc521566e5dd7714945cdf205ea62614de6a8d6538d55ae86d42a3d70b85a"),
    "ledgers/legacy_1832_preservation_map.csv": (1832, "faf6fd11e193d58671442b3689df429648a6f0649cb1d29582e150f81ecdb4e9"),
}
ROOT = Path(__file__).resolve().parents[2]


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def canonical(data) -> bytes:
    return json.dumps(data, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def git(root: Path, *args: str) -> bytes:
    result = subprocess.run(["git", "-C", str(root), *args], capture_output=True)
    if result.returncode:
        # Do not print remote configuration or environment values on failure.
        raise ValueError("GIT_OPERATION_FAILED:" + args[0])
    return result.stdout


def safe_file(root: Path, relative: str) -> Path:
    path = PurePosixPath(relative)
    if path.is_absolute() or not relative or any(p in ("", ".", "..") for p in path.parts):
        raise ValueError("UNSAFE_MANIFEST_PATH:" + relative)
    target = root.joinpath(*path.parts)
    if target.is_symlink() or root.resolve() not in target.resolve().parents:
        raise ValueError("SYMLINK_OR_ESCAPED_INPUT:" + relative)
    return target


def verify_manifest(plan: Path, expected_hash: str = PLAN_MANIFEST_SHA256) -> dict:
    errors, missing, mismatch = [], [], []
    manifest_file = plan / "MANIFEST_SHA256.json"
    if not manifest_file.is_file() or manifest_file.is_symlink():
        return {"status": "FAIL", "errors": ["PLAN_MANIFEST_MISSING"], "missing": [], "mismatch": []}
    manifest_bytes = manifest_file.read_bytes()
    if sha256(manifest_bytes) != expected_hash:
        errors.append("PINNED_PLAN_MANIFEST_MISMATCH")
    manifest = json.loads(manifest_bytes)
    seen = set()
    for member in manifest["files"]:
        name = member["path"]
        if name in seen:
            errors.append("DUPLICATE_MANIFEST_MEMBER:" + name)
        seen.add(name)
        path = safe_file(plan, name)
        if not path.is_file():
            missing.append(name)
            continue
        data = path.read_bytes()
        if len(data) != member["bytes"] or sha256(data) != member["sha256"]:
            mismatch.append(name)
    actual = {p.relative_to(plan).as_posix() for p in plan.rglob("*") if p.is_file()}
    unexpected = sorted(actual - seen - {"MANIFEST_SHA256.json"})
    if unexpected:
        errors.append("UNSEALED_PLAN_MEMBERS")
    preservation = []
    for relative, (expected_count, expected_sha) in PRESERVED.items():
        path = safe_file(plan, relative)
        if not path.is_file():
            continue  # Already present in the complete missing member list.
        data = path.read_bytes()
        rows = list(csv.DictReader(io.StringIO(data.decode("utf-8-sig"))))
        preserved = sha256(data) == expected_sha and len(rows) == expected_count
        if not preserved:
            errors.append("PRESERVED_ORIGINAL_MISMATCH:" + relative)
        preservation.append({"path": relative, "rows": len(rows), "sha256": sha256(data),
                             "original_bytes_preserved": preserved,
                             "semantic_implementation_acceptance": "NOT_PERFORMED"})
    return {"status": "PASS" if not (errors or missing or mismatch) else "FAIL",
            "manifest_sha256": sha256(manifest_bytes), "member_count": len(seen),
            "errors": errors, "missing": sorted(missing), "mismatch": sorted(mismatch),
            "unexpected": unexpected, "preservation": preservation}


def verify_plan_packages(root: Path, plan: Path) -> dict:
    """Two separately pinned roles, authorized by the R31.2 user revision.

    A manifest cannot select its own role or update its expected digest. The
    original archive remains mandatory even when --plan selects an active copy.
    """
    roles = (("active", plan, PLAN_MANIFEST_SHA256, PLAN_MEMBER_COUNT),
             ("original_archive", root / ORIGINAL_PLAN_REL, ORIGINAL_PLAN_MANIFEST_SHA256, ORIGINAL_PLAN_MEMBER_COUNT))
    results = {}
    for role, directory, expected, count in roles:
        result = verify_manifest(directory, expected)
        if result.get("member_count") != count:
            result["errors"].append("EXACT_PLAN_MEMBER_COUNT_MUST_BE_" + str(count))
            result["status"] = "FAIL"
        results[role] = result
    return results


def require_plan_packages(root: Path, plan: Path) -> dict:
    results = verify_plan_packages(root, plan)
    for role, result in results.items():
        if result["status"] != "PASS":
            details = result["errors"] + ["MISSING:" + p for p in result["missing"]]
            details += ["MISMATCH:" + p for p in result["mismatch"]]
            raise ValueError(role.upper() + "_PLAN_INTEGRITY_FAILED:" + ";".join(details)[:1024])
    return results


def tree_entries(root: Path, revision: str) -> dict:
    result = {}
    for record in git(root, "ls-tree", "-rz", revision).split(b"\0"):
        if not record:
            continue
        metadata, path = record.split(b"\t", 1)
        mode, kind, blob = metadata.decode("ascii").split()
        if kind == "blob":
            result[path.decode("utf-8")] = {"mode": mode, "blob": blob}
    return result


def compare_tree(root: Path, plan: Path, baseline: str = BASELINE) -> dict:
    errors, anchors, current_diff, missing_current = [], [], [], []
    head = git(root, "rev-parse", "HEAD").decode().strip()
    baseline_commit = git(root, "rev-parse", baseline + "^{commit}").decode().strip()
    if baseline_commit != baseline:
        errors.append("BASELINE_MUST_BE_FULL_COMMIT_SHA")
    ancestry = subprocess.run(["git", "-C", str(root), "merge-base", "--is-ancestor", baseline, head],
                              capture_output=True)
    if ancestry.returncode:
        errors.append("HEAD_NOT_DESCENDANT_OF_FIXED_BASELINE")
    baseline_tree = tree_entries(root, baseline)
    receipts = json.loads((plan / "ledgers/source_receipts.json").read_text())
    if len(receipts) != 190 or len({r["path"] for r in receipts}) != 190:
        errors.append("EXACT_SOURCE_RECEIPT_COUNT_MUST_BE_190")
    for receipt in receipts:
        path = receipt["path"]
        expected = receipt["git_blob_sha1"]
        entry = baseline_tree.get(path)
        row = {"path": path, "expected_blob": expected, "baseline_blob": entry["blob"] if entry else None}
        if receipt.get("source_commit") != baseline:
            row["status"] = "BASELINE_SOURCE_COMMIT_MISMATCH"
            errors.append("BASELINE_SOURCE_COMMIT_MISMATCH:" + path)
        elif not entry or entry["blob"] != expected:
            row["status"] = "BASELINE_BLOB_MISMATCH"
            errors.append("BASELINE_BLOB_MISMATCH:" + path)
        else:
            original = git(root, "cat-file", "blob", expected)
            packaged = safe_file(plan / "baseline_source", path)
            if (sha256(original) != receipt["sha256"] or len(original) != receipt["bytes"]
                    or not packaged.is_file() or packaged.read_bytes() != original):
                row["status"] = "BASELINE_BYTES_MISMATCH"
                errors.append("BASELINE_BYTES_MISMATCH:" + path)
            else:
                row["status"] = "EXACT_BLOB_VERIFIED"
        current = safe_file(root, path)
        current_sha = sha256(current.read_bytes()) if current.is_file() else None
        if current_sha is None:
            missing_current.append(path)
            errors.append("CURRENT_SOURCE_FILE_MISSING:" + path)
        row["current_sha256"] = current_sha
        row["current_matches_anchor"] = current_sha == receipt["sha256"]
        if not row["current_matches_anchor"]:
            current_diff.append({"path": path, "baseline_blob": expected,
                                 "current_sha256": current_sha, "automatic_old_line_patch": "BLOCKED"})
        anchors.append(row)
    card_owner = {}
    for card in json.loads((plan / "ledgers/cards.json").read_text()):
        for file in card["files"]:
            card_owner.setdefault(file["path"], []).append(card["id"])
    changed = git(root, "diff", "--name-status", "--no-renames", baseline, "HEAD").decode().splitlines()
    return {"status": "PASS" if not errors else "FAIL", "head": head, "baseline": baseline,
            "head_tree": git(root, "rev-parse", "HEAD^{tree}").decode().strip(),
            "baseline_tree": git(root, "rev-parse", baseline + "^{tree}").decode().strip(),
            "ancestor_verified": ancestry.returncode == 0, "errors": errors,
            "source_anchor_count": len(anchors), "source_anchors": anchors,
            "missing_current_files": sorted(missing_current),
            "current_anchor_differences": current_diff,
            "committed_diff_from_baseline": changed,
            "planned_file_cards": card_owner,
            "automatic_old_line_patch": "BLOCKED_CURRENT_DIFF" if current_diff else "ANCHORS_MATCH"}


def product_inputs(root: Path) -> list[dict]:
    """Actual build inputs; generated outputs and evidence logs are separate.

    The Git tree is recorded independently. The digest below binds current source
    bytes even in a dirty developer build, including new source files.
    """
    candidates = []
    for directory in ("app/src/main", "scripts", "tools/r31", "gradle"):
        base = root / directory
        candidates.extend(p for p in base.rglob("*") if p.is_file()
                          and "__pycache__" not in p.parts and p.suffix not in (".pyc", ".pyo"))
    for name in ("app/build.gradle", "build.gradle", "settings.gradle", "gradle.properties", "gradlew",
                 "release/release_manifest.json", "release/release_identity.properties",
                 "release/research_dependencies_r967.json", "release/research_gate_plan_r967.json"):
        path = root / name
        if not path.is_file():
            raise ValueError("REQUIRED_BUILD_INPUT_MISSING:" + name)
        candidates.append(path)
    records = []
    for path in sorted(set(candidates)):
        relative = path.relative_to(root).as_posix()
        data = safe_file(root, relative).read_bytes()
        records.append({"path": relative, "bytes": len(data), "sha256": sha256(data)})
    return records


def build_identity(root: Path, plan: Path) -> dict:
    packages = require_plan_packages(root, plan)
    manifest = safe_file(plan, "MANIFEST_SHA256.json").read_bytes()
    head = git(root, "rev-parse", "HEAD").decode().strip()
    ancestry = subprocess.run(["git", "-C", str(root), "merge-base", "--is-ancestor", BASELINE, head],
                              capture_output=True)
    if ancestry.returncode:
        raise ValueError("HEAD_NOT_DESCENDANT_OF_FIXED_BASELINE")
    inputs = product_inputs(root)
    migration_inputs = [r for r in inputs if r["path"].startswith("app/src/main/")
                        and ("Schema" in Path(r["path"]).name
                             or Path(r["path"]).name in ("DbStore.java", "DatabaseCoordinator.java"))]
    schema = json.loads((root / "release/release_manifest.json").read_text())["schema"]
    migration = {"schema": schema, "inputs": migration_inputs,
                 "acceptance_config_sha256": sha256((plan / "ledgers/acceptance_config.json").read_bytes())}
    status_records = git(root, "status", "--porcelain=v1", "-z", "--untracked-files=all").split(b"\0")
    status = [row.decode("utf-8") for row in status_records if row]
    return {"format": "R31_BUILD_IDENTITY_V1", "baseline_commit": BASELINE, "source_commit": head,
            "source_tree": git(root, "rev-parse", "HEAD^{tree}").decode().strip(),
            "source_dirty": bool(status), "git_status": status,
            "product_worktree_sha256": sha256(canonical(inputs)), "product_input_count": len(inputs),
            "product_digest_excludes": ["app/build (generated outputs)", "verification (test evidence)",
                                        "construction (plan separately sealed)", "private signing inputs"],
            "plan_manifest_sha256": sha256(manifest),
            "revision_plan_manifest_sha256": sha256(manifest),
            "plan_revision": PLAN_REVISION,
            "plan_authorization_commit": PLAN_AUTHORIZATION_COMMIT,
            "original_plan_revision": ORIGINAL_PLAN_REVISION,
            "original_plan_path": ORIGINAL_PLAN_REL,
            "original_plan_manifest_sha256": packages["original_archive"]["manifest_sha256"],
            "original_plan_member_count": packages["original_archive"]["member_count"],
            "migration_contract_sha256": sha256(canonical(migration)), "schema": schema,
            "device_identity": "NOT_RUN", "signature_identity": "NOT_VERIFIED_HERE",
            "natural_quality": "NOT_PROVEN"}


def verify_apk(root: Path, apk: Path, apksigner: Path, aapt: Path, expected_certificate: str,
               identity: dict) -> dict:
    """Inspect a real build output. Signature verification never reads a key.

    Installation and the certificate of the user's installed package are separate
    device evidence; this only compares an APK with an explicitly supplied pin.
    """
    expected_certificate = expected_certificate.replace(":", "").lower()
    if not re.fullmatch(r"[0-9a-f]{64}", expected_certificate):
        raise ValueError("EXPECTED_CERTIFICATE_SHA256_REQUIRED")
    if not apk.is_file() or apk.is_symlink():
        raise ValueError("APK_MISSING_OR_SYMLINK")
    signed = subprocess.run([str(apksigner), "verify", "--verbose", "--print-certs", str(apk)],
                            capture_output=True, text=True)
    if signed.returncode:
        return {"status": "FAIL", "errors": ["APK_SIGNATURE_VERIFICATION_FAILED"],
                "device_installation": "NOT_RUN"}
    certificates = re.findall(r"Signer #\d+ certificate SHA-256 digest:\s*([0-9a-fA-F]+)", signed.stdout)
    certificates = sorted(set(value.lower() for value in certificates))
    errors = []
    if certificates != [expected_certificate]:
        errors.append("APK_CERTIFICATE_PIN_MISMATCH")
    badging = subprocess.run([str(aapt), "dump", "badging", str(apk)], capture_output=True, text=True)
    if badging.returncode:
        raise ValueError("APK_PACKAGE_INSPECTION_FAILED")
    match = re.search(r"^package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'",
                      badging.stdout, re.MULTILINE)
    if not match:
        raise ValueError("APK_PACKAGE_IDENTITY_MISSING")
    manifest = json.loads((root / "release/release_manifest.json").read_text())
    application_id, version_code, version_name = match.groups()
    if (application_id != manifest["applicationId"] or application_id != "com.starcore.research.r13"):
        errors.append("APK_PUBLIC_APPLICATION_ID_MISMATCH")
    if version_code != str(manifest["versionCode"]) or version_name != manifest["versionName"]:
        errors.append("APK_RELEASE_VERSION_MISMATCH")
    with zipfile.ZipFile(apk) as archive:
        try:
            embedded = json.loads(archive.read("assets/diagnostics/build-identity-r31.json"))
        except KeyError:
            embedded = {}
            errors.append("APK_R31_BUILD_IDENTITY_MISSING")
    for key in ("source_commit", "source_tree", "product_worktree_sha256",
                "plan_manifest_sha256", "migration_contract_sha256"):
        if embedded.get(key) != identity[key]:
            errors.append("APK_SOURCE_IDENTITY_MISMATCH:" + key)
    # A clean inspection checkout cannot make a previously dirty APK publishable.
    # Missing and mistyped flags carry no proof that the build used clean source.
    if embedded.get("source_dirty") is not False:
        errors.append("APK_SOURCE_NOT_PROVEN_CLEAN")
    digest = hashlib.sha256()
    with apk.open("rb") as stream:
        while chunk := stream.read(1024 * 1024):
            digest.update(chunk)
    abis = re.search(r"^native-code:\s*(.+)$", badging.stdout, re.MULTILINE)
    return {"status": "FAIL" if errors else "PASS", "errors": errors,
            "application_id": application_id, "version_code": int(version_code), "version_name": version_name,
            "certificate_sha256": certificates, "sha256": digest.hexdigest(), "bytes": apk.stat().st_size,
            "embedded_source_dirty": embedded.get("source_dirty"),
            "abis": re.findall(r"'([^']+)'", abis.group(1)) if abis else [],
            "device_installation": "NOT_RUN", "installed_device_certificate_match": "NOT_RUN"}


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=ROOT)
    parser.add_argument("--plan", type=Path)
    parser.add_argument("--identity-only", action="store_true", help="Emit build metadata; not full baseline acceptance")
    parser.add_argument("--require-clean", action="store_true", help="Reject dirty publication source")
    parser.add_argument("--strict-baseline", action="store_true", help="Require the 190 live source files to equal original anchors")
    parser.add_argument("--apk", type=Path, help="Actual APK whose embedded identity and signature should be verified")
    parser.add_argument("--apksigner", type=Path)
    parser.add_argument("--aapt", type=Path)
    parser.add_argument("--expected-cert-sha256")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args(argv)
    root = args.repo.resolve()
    plan = args.plan.resolve() if args.plan else root / PLAN_REL
    try:
        identity = build_identity(root, plan)
        if args.identity_only:
            result = identity
            errors = ["DIRTY_SOURCE_NOT_PUBLISHABLE"] if args.require_clean and identity["source_dirty"] else []
        else:
            packages = verify_plan_packages(root, plan)
            manifest = packages["active"]
            baseline = compare_tree(root, plan)
            original_baseline = compare_tree(root, root / ORIGINAL_PLAN_REL)
            errors = list(manifest["errors"]) + list(baseline["errors"])
            errors += ["ORIGINAL_ARCHIVE:" + error for error in packages["original_archive"]["errors"]]
            errors += ["ORIGINAL_ARCHIVE:" + error for error in original_baseline["errors"]]
            if manifest["status"] != "PASS":
                errors.append("PACKAGE_MEMBER_INTEGRITY_FAILED")
            if packages["original_archive"]["status"] != "PASS":
                errors.append("ORIGINAL_ARCHIVE_PACKAGE_MEMBER_INTEGRITY_FAILED")
            if args.require_clean and identity["source_dirty"]:
                errors.append("DIRTY_SOURCE_NOT_PUBLISHABLE")
            if args.strict_baseline and baseline["current_anchor_differences"]:
                errors.append("CURRENT_SOURCE_DIFFERS_FROM_FROZEN_ANCHORS")
            result = {"scope": "R31_BASELINE_AND_BUILD_IDENTITY_ONLY", "manifest": manifest,
                      "original_archive_manifest": packages["original_archive"],
                      "original_archive_baseline": original_baseline,
                      "baseline": baseline, "identity": identity, "errors": errors,
                      "status": "FAIL" if errors else "PASS",
                      "device_acceptance": "NOT_RUN", "natural_quality": "NOT_PROVEN"}
        if args.apk:
            if not args.apksigner or not args.aapt or not args.expected_cert_sha256:
                raise ValueError("APK_VERIFICATION_REQUIRES_APKSIGNER_AAPT_AND_CERTIFICATE_PIN")
            apk = verify_apk(root, args.apk, args.apksigner, args.aapt, args.expected_cert_sha256, identity)
            result["apk"] = apk
            errors.extend(apk["errors"])
            result["status"] = "FAIL" if errors else "PASS"
        if errors and args.identity_only:
            result = {**result, "status": "FAIL", "errors": errors}
    except (ValueError, OSError, KeyError, zipfile.BadZipFile, json.JSONDecodeError) as problem:
        result = {"status": "FAIL", "errors": [str(problem)], "device_acceptance": "NOT_RUN"}
        errors = result["errors"]
    rendered = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered)
    else:
        print(rendered, end="")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
