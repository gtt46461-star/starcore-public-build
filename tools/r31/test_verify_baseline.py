#!/usr/bin/env python3
"""Fault injection for the actual R31 baseline verifier (no device claims)."""
import contextlib
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import zipfile

SPEC = importlib.util.spec_from_file_location("r31_verifier", Path(__file__).with_name("verify_baseline.py"))
v = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(v)


class PlanIntegrityTest(unittest.TestCase):
    def fixture(self, directory):
        plan = Path(directory)
        members = []
        for name, data in (("a.txt", b"first"), ("nested/b.txt", b"second")):
            path = plan / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
            members.append({"path": name, "bytes": len(data), "sha256": v.sha256(data)})
        manifest = json.dumps({"files": members}).encode()
        (plan / "MANIFEST_SHA256.json").write_bytes(manifest)
        return plan, v.sha256(manifest)

    def test_real_sealed_package_and_legacy_originals(self):
        result = v.verify_manifest(v.ROOT / v.ORIGINAL_PLAN_REL, v.ORIGINAL_PLAN_MANIFEST_SHA256)
        self.assertEqual("PASS", result["status"])
        self.assertEqual(405, result["member_count"])
        self.assertEqual([1832, 299, 1832], [r["rows"] for r in result["preservation"]])
        self.assertTrue(all(r["original_bytes_preserved"] for r in result["preservation"]))
        self.assertTrue(all(r["semantic_implementation_acceptance"] == "NOT_PERFORMED"
                            for r in result["preservation"]))

    def test_resealed_manifest_does_not_change_fixed_expectation(self):
        with tempfile.TemporaryDirectory() as directory:
            plan, expected = self.fixture(directory)
            (plan / "MANIFEST_SHA256.json").write_text('{"files": []}')
            result = v.verify_manifest(plan, expected)
            self.assertIn("PINNED_PLAN_MANIFEST_MISMATCH", result["errors"])
            self.assertEqual("FAIL", result["status"])

    def test_all_missing_members_are_reported(self):
        with tempfile.TemporaryDirectory() as directory:
            plan, expected = self.fixture(directory)
            (plan / "a.txt").unlink()
            (plan / "nested/b.txt").unlink()
            result = v.verify_manifest(plan, expected)
            self.assertEqual(["a.txt", "nested/b.txt"], result["missing"])
            self.assertEqual("FAIL", result["status"])

    def test_all_changed_members_are_reported(self):
        with tempfile.TemporaryDirectory() as directory:
            plan, expected = self.fixture(directory)
            (plan / "a.txt").write_text("changed")
            (plan / "nested/b.txt").write_text("changed")
            result = v.verify_manifest(plan, expected)
            self.assertEqual(["a.txt", "nested/b.txt"], result["mismatch"])

    def test_unsealed_extra_member_is_not_silently_accepted(self):
        with tempfile.TemporaryDirectory() as directory:
            plan, expected = self.fixture(directory)
            (plan / "extra.txt").write_text("not in original seal")
            result = v.verify_manifest(plan, expected)
            self.assertEqual(["extra.txt"], result["unexpected"])
            self.assertEqual("FAIL", result["status"])

    def test_symlink_and_parent_path_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            outside = root / "outside.txt"
            outside.write_text("test")
            plan = root / "plan"
            plan.mkdir()
            (plan / "link").symlink_to(outside)
            with self.assertRaisesRegex(ValueError, "SYMLINK_OR_ESCAPED_INPUT"):
                v.safe_file(plan, "link")
            with self.assertRaisesRegex(ValueError, "UNSAFE_MANIFEST_PATH"):
                v.safe_file(plan, "../outside.txt")


class PlanRevisionRoleTest(unittest.TestCase):
    @contextlib.contextmanager
    def plans(self):
        # Immutable hardlink copies avoid duplicating 260MiB of existing evidence.
        # Faults below unlink their one target before writing; originals stay intact.
        with tempfile.TemporaryDirectory(prefix="r31-plan-revision-", dir=v.ROOT / "verification") as directory:
            root = Path(directory)
            for relative in (v.PLAN_REL, v.ORIGINAL_PLAN_REL):
                shutil.copytree(v.ROOT / relative, root / relative, copy_function=os.link)
            yield root, root / v.PLAN_REL

    @staticmethod
    def replace_member(path, data):
        path.unlink()
        path.write_bytes(data)

    def test_hashes_are_fixed_to_authorized_user_commit(self):
        for relative, expected in ((v.PLAN_REL, v.PLAN_MANIFEST_SHA256),
                                   (v.ORIGINAL_PLAN_REL, v.ORIGINAL_PLAN_MANIFEST_SHA256)):
            sealed = v.git(v.ROOT, "show", v.PLAN_AUTHORIZATION_COMMIT + ":" + relative + "/MANIFEST_SHA256.json")
            self.assertEqual(expected, v.sha256(sealed))
        self.assertNotEqual(v.PLAN_MANIFEST_SHA256, v.ORIGINAL_PLAN_MANIFEST_SHA256)

    def test_real_active_and_original_archive_are_independently_sealed(self):
        results = v.require_plan_packages(v.ROOT, v.ROOT / v.PLAN_REL)
        self.assertEqual([410, 405], [results[r]["member_count"] for r in ("active", "original_archive")])
        for result in results.values():
            self.assertEqual("PASS", result["status"])
            self.assertEqual([1832, 299, 1832], [r["rows"] for r in result["preservation"]])
            self.assertTrue(all(r["original_bytes_preserved"] for r in result["preservation"]))

    def test_identity_points_to_active_and_preserves_original_trace(self):
        identity = v.build_identity(v.ROOT, v.ROOT / v.PLAN_REL)
        self.assertEqual(v.PLAN_MANIFEST_SHA256, identity["plan_manifest_sha256"])
        self.assertEqual(v.PLAN_REVISION, identity["plan_revision"])
        self.assertEqual(v.PLAN_AUTHORIZATION_COMMIT, identity["plan_authorization_commit"])
        self.assertEqual(v.ORIGINAL_PLAN_MANIFEST_SHA256, identity["original_plan_manifest_sha256"])
        self.assertEqual(v.ORIGINAL_PLAN_REVISION, identity["original_plan_revision"])
        self.assertEqual(v.ORIGINAL_PLAN_REL, identity["original_plan_path"])
        self.assertEqual(405, identity["original_plan_member_count"])
        self.assertEqual("NOT_RUN", identity["device_identity"])

    def test_original_archive_cannot_impersonate_active_revision(self):
        result = v.verify_plan_packages(v.ROOT, v.ROOT / v.ORIGINAL_PLAN_REL)
        self.assertEqual("FAIL", result["active"]["status"])
        self.assertIn("PINNED_PLAN_MANIFEST_MISMATCH", result["active"]["errors"])
        self.assertEqual("PASS", result["original_archive"]["status"])

    def test_resealed_manifest_in_either_role_is_rejected_by_identity(self):
        for role, relative in (("active", v.PLAN_REL), ("original_archive", v.ORIGINAL_PLAN_REL)):
            with self.subTest(role=role), self.plans() as (root, active):
                target = root / relative / "MANIFEST_SHA256.json"
                original = target.read_bytes()
                self.replace_member(target, original + b"\n")
                with self.assertRaisesRegex(ValueError, role.upper() + "_PLAN_INTEGRITY_FAILED:PINNED_PLAN_MANIFEST_MISMATCH"):
                    v.build_identity(root, active)
                self.assertEqual(original, (v.ROOT / relative / "MANIFEST_SHA256.json").read_bytes())

    def test_member_tamper_in_either_role_is_rejected_without_resealing(self):
        for role, relative in (("active", v.PLAN_REL), ("original_archive", v.ORIGINAL_PLAN_REL)):
            with self.subTest(role=role), self.plans() as (root, active):
                target = root / relative / "README.md"
                original = target.read_bytes()
                self.replace_member(target, original + b"\nCONTROLLED_TAMPER\n")
                result = v.verify_plan_packages(root, active)
                self.assertEqual("FAIL", result[role]["status"])
                self.assertEqual(["README.md"], result[role]["mismatch"])
                with self.assertRaisesRegex(ValueError, role.upper() + "_PLAN_INTEGRITY_FAILED:MISMATCH:README.md"):
                    v.build_identity(root, active)
                self.assertEqual(original, (v.ROOT / relative / "README.md").read_bytes())

    def test_original_and_active_each_retain_exact_190_baseline_blobs(self):
        for relative in (v.PLAN_REL, v.ORIGINAL_PLAN_REL):
            with self.subTest(plan=relative):
                result = v.compare_tree(v.ROOT, v.ROOT / relative)
                self.assertEqual("PASS", result["status"])
                self.assertEqual(190, result["source_anchor_count"])
                self.assertTrue(all(r["status"] == "EXACT_BLOB_VERIFIED" for r in result["source_anchors"]))


class GitBaselineTest(unittest.TestCase):
    def fixture(self, directory):
        root, plan = Path(directory) / "repo", Path(directory) / "plan"
        root.mkdir()
        plan.mkdir()
        def git(*args):
            return subprocess.check_output(["git", "-C", str(root), *args], stderr=subprocess.DEVNULL).decode().strip()
        git("init")
        git("config", "user.name", "R31 test fixture")
        git("config", "user.email", "r31-fixture@example.invalid")
        rows = []
        for index in range(190):
            name = "src/Input%03d.txt" % index
            data = ("immutable fixture %d\n" % index).encode()
            for base in (root, plan / "baseline_source"):
                path = base / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(data)
            rows.append({"path": name, "git_blob_sha1": hashlib.sha1(
                b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest(),
                         "sha256": v.sha256(data), "bytes": len(data)})
        git("add", ".")
        git("commit", "-m", "independent frozen fixture")
        baseline = git("rev-parse", "HEAD")
        for row in rows:
            row["source_commit"] = baseline
        (plan / "ledgers").mkdir()
        (plan / "ledgers/source_receipts.json").write_text(json.dumps(rows))
        (plan / "ledgers/cards.json").write_text(json.dumps([
            {"id": "fixture-card", "files": [{"path": rows[0]["path"]}]}]))
        return root, plan, baseline, git

    def test_exact_190_blobs_then_current_change_blocks_old_line_anchors(self):
        with tempfile.TemporaryDirectory() as directory:
            root, plan, baseline, git = self.fixture(directory)
            before = v.compare_tree(root, plan, baseline)
            self.assertEqual("PASS", before["status"])
            self.assertEqual("ANCHORS_MATCH", before["automatic_old_line_patch"])
            (root / "src/Input000.txt").write_text("inherited valid fix\n")
            git("add", ".")
            git("commit", "-m", "valid follow-up retained")
            after = v.compare_tree(root, plan, baseline)
            self.assertEqual("PASS", after["status"])
            self.assertTrue(after["ancestor_verified"])
            self.assertEqual("BLOCKED_CURRENT_DIFF", after["automatic_old_line_patch"])
            self.assertEqual(["src/Input000.txt"], [r["path"] for r in after["current_anchor_differences"]])
            self.assertEqual(["M\tsrc/Input000.txt"], after["committed_diff_from_baseline"])
            self.assertEqual("inherited valid fix\n", (root / "src/Input000.txt").read_text())

    def test_unrelated_branch_is_rejected_even_with_same_files(self):
        with tempfile.TemporaryDirectory() as directory:
            root, plan, baseline, git = self.fixture(directory)
            foreign = git("commit-tree", git("rev-parse", "HEAD^{tree}"), "-m", "unrelated history")
            git("update-ref", "HEAD", foreign)
            result = v.compare_tree(root, plan, baseline)
            self.assertEqual("FAIL", result["status"])
            self.assertIn("HEAD_NOT_DESCENDANT_OF_FIXED_BASELINE", result["errors"])

    def test_false_blob_receipt_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root, plan, baseline, git = self.fixture(directory)
            receipt_file = plan / "ledgers/source_receipts.json"
            rows = json.loads(receipt_file.read_text())
            rows[0]["git_blob_sha1"] = "0" * 40
            receipt_file.write_text(json.dumps(rows))
            result = v.compare_tree(root, plan, baseline)
            self.assertIn("BASELINE_BLOB_MISMATCH:src/Input000.txt", result["errors"])

    def test_all_missing_current_anchors_fail_even_after_committed_deletion(self):
        with tempfile.TemporaryDirectory() as directory:
            root, plan, baseline, git = self.fixture(directory)
            for name in ("src/Input000.txt", "src/Input001.txt"):
                (root / name).unlink()
            git("add", "-u")
            git("commit", "-m", "explicit missing current source negative control")
            result = v.compare_tree(root, plan, baseline)
            self.assertEqual("FAIL", result["status"])
            self.assertEqual(["src/Input000.txt", "src/Input001.txt"], result["missing_current_files"])
            self.assertIn("CURRENT_SOURCE_FILE_MISSING:src/Input000.txt", result["errors"])
            self.assertIn("CURRENT_SOURCE_FILE_MISSING:src/Input001.txt", result["errors"])
            self.assertEqual("BLOCKED_CURRENT_DIFF", result["automatic_old_line_patch"])
            self.assertEqual(190, result["source_anchor_count"])

    def test_wrong_receipt_commit_has_a_distinct_commit_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            root, plan, baseline, git = self.fixture(directory)
            receipt_file = plan / "ledgers/source_receipts.json"
            rows = json.loads(receipt_file.read_text())
            rows[0]["source_commit"] = "f" * 40
            receipt_file.write_text(json.dumps(rows))
            result = v.compare_tree(root, plan, baseline)
            self.assertEqual("FAIL", result["status"])
            self.assertIn("BASELINE_SOURCE_COMMIT_MISMATCH:src/Input000.txt", result["errors"])
            self.assertEqual("BASELINE_SOURCE_COMMIT_MISMATCH", result["source_anchors"][0]["status"])

    def test_dirty_release_mode_fails_without_rewriting_expected_identity(self):
        with patch.object(v, "build_identity", return_value={"format": "R31_BUILD_IDENTITY_V1", "source_dirty": True}), \
                contextlib.redirect_stdout(io.StringIO()) as stdout:
            code = v.main(["--identity-only", "--require-clean"])
        result = json.loads(stdout.getvalue())
        self.assertEqual(1, code)
        self.assertIn("DIRTY_SOURCE_NOT_PUBLISHABLE", result["errors"])
        self.assertTrue(result["source_dirty"])


class ApkInspectionAdapterTest(unittest.TestCase):
    # The tool responses here test pinning/identity rejection. These fixtures are
    # explicitly NOT signed APKs and are never used as Android acceptance.
    def test_embedded_dirty_or_unknown_build_cannot_be_a_release_receipt(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "release").mkdir()
            (root / "release/release_manifest.json").write_text(json.dumps({
                "applicationId": "com.starcore.research.r13", "versionCode": 123,
                "versionName": "fixture"}))
            identity = {key: "a" * 64 for key in (
                "source_commit", "source_tree", "product_worktree_sha256",
                "plan_manifest_sha256", "migration_contract_sha256")}
            signer = subprocess.CompletedProcess([], 0, stdout=
                "Signer #1 certificate SHA-256 digest: " + "c" * 64, stderr="")
            aapt = subprocess.CompletedProcess([], 0, stdout=
                "package: name='com.starcore.research.r13' versionCode='123' "
                "versionName='fixture'\n", stderr="")
            apk = root / "fixture.zip"
            for state in (False, True, None, "false", 0):
                with self.subTest(embedded_source_dirty=state):
                    embedded = dict(identity)
                    if state is not None:
                        embedded["source_dirty"] = state
                    with zipfile.ZipFile(apk, "w") as archive:
                        archive.writestr("assets/diagnostics/build-identity-r31.json",
                                         json.dumps(embedded))
                    with patch.object(v.subprocess, "run", side_effect=[signer, aapt]):
                        result = v.verify_apk(root, apk, Path("fixture-signer"),
                                              Path("fixture-aapt"), "c" * 64, identity)
                    if state is False:
                        self.assertEqual("PASS", result["status"])
                    else:
                        self.assertEqual("FAIL", result["status"])
                        self.assertIn("APK_SOURCE_NOT_PROVEN_CLEAN", result["errors"])
                    self.assertEqual("NOT_RUN", result["device_installation"])

    def test_certificate_mismatch_and_embedded_source_mismatch_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "release").mkdir()
            manifest = {"applicationId": "com.starcore.research.r13", "versionCode": 123,
                        "versionName": "fixture"}
            (root / "release/release_manifest.json").write_text(json.dumps(manifest))
            identity = {key: "a" * 64 for key in ("source_commit", "source_tree", "product_worktree_sha256",
                        "plan_manifest_sha256", "migration_contract_sha256")}
            apk = root / "fixture.zip"
            embedded = {**identity, "source_commit": "b" * 64}
            with zipfile.ZipFile(apk, "w") as archive:
                archive.writestr("assets/diagnostics/build-identity-r31.json", json.dumps(embedded))
            signer = subprocess.CompletedProcess([], 0, stdout="Signer #1 certificate SHA-256 digest: " + "c" * 64, stderr="")
            aapt = subprocess.CompletedProcess([], 0, stdout="package: name='com.starcore.research.r13' versionCode='123' versionName='fixture'\nnative-code: 'arm64-v8a'\n", stderr="")
            with patch.object(v.subprocess, "run", side_effect=[signer, aapt]):
                result = v.verify_apk(root, apk, Path("fixture-signer"), Path("fixture-aapt"), "d" * 64, identity)
            self.assertEqual("FAIL", result["status"])
            self.assertIn("APK_CERTIFICATE_PIN_MISMATCH", result["errors"])
            self.assertIn("APK_SOURCE_IDENTITY_MISMATCH:source_commit", result["errors"])
            self.assertEqual("NOT_RUN", result["device_installation"])

    def test_unsigned_inspection_failure_is_not_package_pass(self):
        with tempfile.TemporaryDirectory() as directory:
            apk = Path(directory) / "unsigned-fixture"
            apk.write_bytes(b"not an APK")
            with patch.object(v.subprocess, "run", return_value=subprocess.CompletedProcess([], 1, stdout="", stderr="")):
                result = v.verify_apk(Path(directory), apk, Path("fixture-signer"), Path("fixture-aapt"), "a" * 64, {})
            self.assertEqual(["APK_SIGNATURE_VERIFICATION_FAILED"], result["errors"])
            self.assertEqual("FAIL", result["status"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
