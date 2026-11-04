#!/usr/bin/env python3
"""Fault injection for the actual R31 baseline verifier (no device claims)."""
import contextlib
import hashlib
import importlib.util
import io
import json
from pathlib import Path
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
        result = v.verify_manifest(v.ROOT / v.PLAN_REL)
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
