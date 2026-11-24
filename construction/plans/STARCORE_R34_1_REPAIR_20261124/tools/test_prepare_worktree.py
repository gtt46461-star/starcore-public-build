"""Exercise the import on real temporary Git worktrees, including non-ASCII paths."""
import json
import hashlib
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import prepare_worktree as w
import verify_plan as sealtool


class ImportTest(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.repo = Path(temp.name) / "repo"
        self.target = Path(temp.name) / "worker"
        self.repo.mkdir()
        for args in (("init", "-q", "-b", "main"), ("config", "user.name", "Fixture"),
                     ("config", "user.email", "fixture@example.invalid")):
            w.git(self.repo, *args)
        (self.repo / "Product.java").write_text("installed")
        self.commit("installed")
        baseline = w.field(self.repo, "rev-parse", "HEAD")
        owner = patch.object(w, "BASELINE", baseline)
        owner.start()
        self.addCleanup(owner.stop)
        real_seal_baseline = patch.object(sealtool, "BASELINE", baseline)
        real_seal_baseline.start()
        self.addCleanup(real_seal_baseline.stop)
        for member in w.IMPORTS:
            target = self.repo / member
            if member in (w.PLAN_PATH, w.EVIDENCE_PATH):
                target = target / ("control/PLAN_IDENTITY.json" if member == w.PLAN_PATH else "evidence.txt")
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps({"application_baseline_commit": baseline}))
        (self.repo / "Product.java").write_text("different main source")
        report = self.repo / w.REPORT_PATH
        report.parent.mkdir(parents=True, exist_ok=True)
        report.write_bytes(b"controlled raw report")
        facts = self.repo / w.PLAN_PATH / "evidence/BASELINE_FACTS.json"
        facts.parent.mkdir()
        facts.write_text(json.dumps({"report_outer_sha256":hashlib.sha256(report.read_bytes()).hexdigest()}))
        sealtool.seal(self.repo / w.PLAN_PATH)
        self.commit("first plan")
        self.ref = w.field(self.repo, "rev-parse", "HEAD")

    def commit(self, msg, repo=None):
        root = repo or self.repo
        w.git(root, "add", ".")
        w.git(root, "commit", "-qm", msg)

    def run_import(self, create=True, branch="construction/repair"):
        return w.prepare(self.repo, self.target, self.ref, branch, create)["status"]

    def test_unicode_scope_resume_and_upgrade(self):
        self.assertEqual(self.run_import(False), "PREFLIGHT_OK_NO_MUTATION")
        self.assertFalse(self.target.exists())
        self.assertEqual(self.run_import(), "PLAN_IMPORT_STAGED")
        self.assertEqual((self.target / "Product.java").read_text(), "installed")
        self.assertEqual(self.run_import(), "PLAN_ALREADY_STAGED_VERIFY_AND_COMMIT")
        self.commit("import", self.target)
        self.assertEqual(self.run_import(), "EXISTING_PLAN_VERIFIED")
        (self.repo / w.IMPORTS[-1]).write_text("revised")
        self.commit("new plan")
        self.ref = w.field(self.repo, "rev-parse", "HEAD")
        self.assertEqual(self.run_import(False), "PLAN_IMPORT_REQUIRED")
        (self.target / w.IMPORTS[-1]).write_text("local edit")
        with self.assertRaises(ValueError):
            self.run_import()
        self.assertEqual((self.target / w.IMPORTS[-1]).read_text(), "local edit")
        w.git(self.target, "restore", "--", w.IMPORTS[-1])
        self.assertEqual(self.run_import(), "PLAN_IMPORT_STAGED")
        self.assertEqual((self.target / w.IMPORTS[-1]).read_text(), "revised")

    def test_unrelated_staged_content_is_preserved(self):
        self.run_import()
        product = self.target / "Product.java"
        product.write_text("unfinished local change")
        w.git(self.target, "add", "--", product.name)
        with self.assertRaisesRegex(ValueError, "product files"):
            self.run_import()
        self.assertEqual(w.field(self.target, "show", ":Product.java"), "unfinished local change")

    def change_plan(self):
        (self.repo / w.IMPORTS[-1]).write_text("next entry")
        self.commit("next plan")
        self.ref = w.field(self.repo, "rev-parse", "HEAD")

    def test_ref_bytes_verified_before_worktree_creation(self):
        (self.repo / w.PLAN_PATH / "extra.py").write_text("unexpected")
        self.commit("changed without new seal")
        self.ref = w.field(self.repo, "rev-parse", "HEAD")
        for create in (False, True):
            with self.assertRaisesRegex(ValueError, "unlisted"):
                self.run_import(create)
            self.assertFalse(self.target.exists())

    def test_existing_committed_or_unstaged_evidence_is_preserved(self):
        self.run_import()
        self.commit("import", self.target)
        original = self.target / w.EVIDENCE_PATH / "field-run.json"
        original.write_text("original record")
        self.commit("field run", self.target)
        original.write_text("additional device observation")
        (self.target / "Product.java").write_text("legitimate work in progress")
        self.change_plan()
        self.assertEqual(self.run_import(), "PLAN_IMPORT_STAGED")
        self.assertEqual(original.read_text(), "additional device observation")
        self.assertEqual((self.target / "Product.java").read_text(), "legitimate work in progress")
        self.assertEqual(w.paths(self.target,"diff","--cached","--name-only","-z"), [w.IMPORTS[-1]])

    def test_plan_with_wrong_or_missing_raw_report_fails_without_mutation(self):
        raw = self.repo / w.REPORT_PATH
        raw.write_bytes(b"other report")
        self.commit("substituted raw report")
        self.ref = w.field(self.repo, "rev-parse", "HEAD")
        with self.assertRaisesRegex(ValueError, "report bytes differ"):
            self.run_import()
        self.assertFalse(self.target.exists())
        raw.unlink()
        self.commit("missing original")
        self.ref = w.field(self.repo, "rev-parse", "HEAD")
        with self.assertRaisesRegex(ValueError, "report is missing"):
            self.run_import()
        self.assertFalse(self.target.exists())

    def test_committed_evidence_collisions_do_not_overwrite(self):
        self.run_import()
        self.commit("import", self.target)
        rel = Path(w.EVIDENCE_PATH) / "field-run.json"
        (self.target / rel).write_text("local record")
        self.commit("field run", self.target)
        (self.repo / rel).write_text("other result")
        self.change_plan()
        with self.assertRaisesRegex(ValueError, "collides"):
            self.run_import()
        self.assertEqual((self.target / rel).read_text(), "local record")
        self.assertFalse(w.paths(self.target, "diff", "--cached", "--name-only", "-z"))
        # A previously committed branch needs the same checks before adding a new checkout.
        w.git(self.repo, "worktree", "remove", str(self.target))
        for create in (False, True):
            with self.assertRaisesRegex(ValueError, "collides"):
                self.run_import(create)
            self.assertFalse(self.target.exists())

    def test_ignored_evidence_name_prevents_overwrite(self):
        self.run_import()
        self.commit("import", self.target)
        rel = Path(w.EVIDENCE_PATH) / "copy.apk"
        (self.target / rel).write_text("untracked local")
        with (self.repo / ".git/info/exclude").open("a") as stream:
            stream.write("\n*.apk\n")
        (self.repo / rel).write_text("new public evidence")
        w.git(self.repo, "add", "-f", "--", str(rel))
        self.change_plan()
        with self.assertRaisesRegex(ValueError, "collides"):
            self.run_import()
        self.assertEqual((self.target / rel).read_text(), "untracked local")
        self.assertFalse(w.paths(self.target, "diff", "--cached", "--name-only", "-z"))

    def test_expected_seal_mismatch_retains_branch(self):
        with self.assertRaisesRegex(ValueError,"different manifest"):
            w.prepare(self.repo,self.target,self.ref,"construction/repair",True,"0"*64)
        self.assertFalse(self.target.exists())

    def test_published_removal_and_new_checkout_on_current_branch(self):
        old = self.repo / w.EVIDENCE_PATH / "old-source.txt"
        old.write_text("old source")
        self.commit("old publication")
        self.ref = w.field(self.repo, "rev-parse", "HEAD")
        self.run_import()
        self.commit("import", self.target)
        old.unlink()
        self.change_plan()
        self.assertEqual(self.run_import(), "PLAN_IMPORT_STAGED")
        self.assertFalse((self.target / w.EVIDENCE_PATH / "old-source.txt").exists())
        self.commit("import new revision", self.target)
        w.git(self.repo, "worktree", "remove", str(self.target))
        self.assertEqual(self.run_import(), "EXISTING_PLAN_VERIFIED")
        self.assertFalse(w.paths(self.target, "diff", "--cached", "--name-only", "-z"))

    def test_wrong_target_or_branch(self):
        self.run_import()
        with self.assertRaises(ValueError):
            self.run_import(branch="main")
        child = self.target / "other"
        child.mkdir()
        (child / "local").touch()
        self.target = child
        with self.assertRaises(ValueError):
            self.run_import()


if __name__ == "__main__":
    unittest.main()
