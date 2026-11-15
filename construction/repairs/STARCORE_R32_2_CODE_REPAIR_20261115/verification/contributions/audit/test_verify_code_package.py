#!/usr/bin/env python3
"""Positive fixture and mutation tests for the source-package verifier."""
import copy
import difflib
import hashlib
import importlib.util
import json
import pathlib
import shutil
import tempfile
import unittest
import zipfile

HERE = pathlib.Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("code_package_verifier", HERE / "verify_code_package.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
h = lambda b: hashlib.sha256(b).hexdigest()


class PackageFixture(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.package = pathlib.Path(self.temp.name)
        self.before = {"src/a.java": b"old-a\n", "src/b.java": b"old-b\n"}
        self.after = {"src/a.java": b"old-a\n", "src/b.java": b"new-b\n", "src/c.java": b"new-c\n"}
        self.rows = []
        for path in self.after:
            action = "PRESERVE_REFERENCE" if path.endswith("a.java") else "REPLACE" if path in self.before else "ADD"
            row = {"path": path, "owner": "P1", "action": action, "before_sha256": h(self.before[path]) if path in self.before else None, "after_sha256": h(self.after[path]), "source": "code/P1/files/" + path, "implementation_assessment": "EXISTING_SOURCE_DELIVERED_NOT_NEW_IMPLEMENTATION" if action == "PRESERVE_REFERENCE" else "MODIFIED_SOURCE_TEST_SCOPE_IN_VERIFICATION"}
            self.rows.append(row)
            self.write(row["source"], self.after[path])
            if action == "REPLACE":
                self.write("code/P1/before/" + path, self.before[path])
        self.manifest = {"revision": "TEST", "batches": {"P1": {"prerequisites": [], "files": self.rows}}}
        self.save_manifest()
        self.js("reference/R32.1/contracts/batches.json", [{"id": "P1", "prerequisites": []}])
        self.js("reference/R32.1/ledger/file_owners.json", [{"path": p, "owner": "P1"} for p in self.after])
        self.js("reference/R32.1/contracts/repair_cards.json", [{"id": "P1.01", "batch": "P1", "writes": list(self.after), "reads": [], "files": list(self.after), "traceOnly": []}])
        for row in self.rows:
            stem = h(row["path"].encode())[:12]
            self.write("issues/P1.01/" + stem + ".md", (row["path"] + " " + row["after_sha256"] + " " + row["action"] + " ../../" + row["source"]).encode())
        self.write("issues/P1.01/README.md", b"test\n")
        self.write("issues/P1.01/index.html", " ".join(r["source"] for r in self.rows).encode())
        self.archive("SOURCE_BASELINE.zip", self.before)
        self.archive("SOURCE_CANDIDATE.zip", self.after)
        patch = []
        for path in ["src/b.java", "src/c.java"]:
            patch.append("diff --git a/" + path + " b/" + path + "\n")
            if path not in self.before:
                patch.append("new file mode 100644\n")
            patch.extend(difflib.unified_diff(self.before.get(path, b"").decode().splitlines(keepends=True), self.after[path].decode().splitlines(keepends=True), fromfile="a/" + path if path in self.before else "/dev/null", tofile="b/" + path))
        self.write("patches/P1.patch", "".join(patch).encode())
        apply_tool = HERE.parents[1] / "package_work/apply_batch.py"
        if not apply_tool.is_file():
            apply_tool = HERE.parents[2] / "tools/apply_batch.py"
        require = apply_tool.is_file()
        if not require:
            self.fail("apply_batch.py fixture dependency unavailable")
        self.write("tools/apply_batch.py", apply_tool.read_bytes())

    def tearDown(self):
        self.temp.cleanup()

    def write(self, path, data):
        target = self.package / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)

    def js(self, path, data):
        self.write(path, (json.dumps(data) + "\n").encode())

    def save_manifest(self):
        self.js("code_manifest.json", self.manifest)
        self.js("contracts/file_actions.json", self.rows)

    def archive(self, name, files):
        with zipfile.ZipFile(self.package / name, "w") as z:
            for name, data in files.items():
                z.writestr(name, data)

    def test_complete_byte_package_and_roundtrip(self):
        result = module.verify(self.package, None)
        self.assertEqual(result["status"], "PASS")
        self.assertFalse(result["runtime_or_design_completion_proven"])
        self.assertFalse(result["source_copy_counted_as_new_implementation"])

    def test_missing_code_refuses(self):
        (self.package / self.rows[0]["source"]).unlink()
        with self.assertRaisesRegex(ValueError, "missing complete post-state"):
            module.verify(self.package, None)

    def test_missing_per_file_document_refuses(self):
        (self.package / "issues/P1.01" / (h(self.rows[0]["path"].encode())[:12] + ".md")).unlink()
        with self.assertRaisesRegex(ValueError, "missing per-card per-file"):
            module.verify(self.package, None)

    def test_preservation_cannot_claim_implementation(self):
        self.rows[0]["implementation_assessment"] = "IMPLEMENTED_PASS"
        self.save_manifest()
        with self.assertRaisesRegex(ValueError, "preservation falsely labeled"):
            module.verify(self.package, None)

    def test_duplicate_writer_refuses(self):
        self.rows.append(copy.deepcopy(self.rows[0]))
        self.save_manifest()
        with self.assertRaisesRegex(ValueError, "multiple file writers"):
            module.verify(self.package, None)

    def test_future_dependency_refuses(self):
        self.manifest["batches"]["P1"]["prerequisites"] = ["P2"]
        self.save_manifest()
        self.js("reference/R32.1/contracts/batches.json", [{"id": "P1", "prerequisites": ["P2"]}])
        with self.assertRaisesRegex(ValueError, "cycle/future prerequisite"):
            module.verify(self.package, None)

    def test_candidate_archive_mismatch_refuses(self):
        changed = dict(self.after)
        changed["src/b.java"] = b"unreported-edit\n"
        self.archive("SOURCE_CANDIDATE.zip", changed)
        with self.assertRaisesRegex(ValueError, "candidate ZIP code mismatch"):
            module.verify(self.package, None)

    def test_diff_not_matching_declared_bytes_refuses(self):
        patch = self.package / "patches/P1.patch"
        patch.write_text(patch.read_text().replace("+new-b", "+wrong-b"))
        with self.assertRaisesRegex(ValueError, "post-apply code differs"):
            module.verify(self.package, None)


if __name__ == "__main__":
    unittest.main()
