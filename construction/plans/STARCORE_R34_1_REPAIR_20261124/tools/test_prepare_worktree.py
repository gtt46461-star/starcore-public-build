"""Exercise the import on real temporary Git worktrees, including non-ASCII paths."""
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import prepare_worktree as w


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
        for member in w.IMPORTS:
            target = self.repo / member
            if member in (w.PLAN_PATH, w.EVIDENCE_PATH):
                target = target / ("control/PLAN_IDENTITY.json" if member == w.PLAN_PATH else "evidence.txt")
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps({"application_baseline_commit": baseline}))
        (self.repo / "Product.java").write_text("different main source")
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
