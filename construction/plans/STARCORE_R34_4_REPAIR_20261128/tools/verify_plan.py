#!/usr/bin/env python3
"""Verify the public plan-only package without accessing any private report.

The default root is Path(__file__).resolve().parents[1], suitable for installation
as <plan>/tools/verify_plan.py. Only an explicitly requested --output is written.
This checks public file scope, contracts, public source bytes, and package hashes;
it does not repeat private report/ZIP/coverage validation or certify the product.
"""
from __future__ import annotations

import argparse
import fnmatch
import hashlib
import json
import re
import sys
from pathlib import Path, PurePosixPath


class Invalid(Exception):
    pass


def need(condition, message):
    if not condition:
        raise Invalid(message)


def unique(values, label):
    values = list(values)
    need(len(values) == len(set(values)), label + ": duplicate values")
    return set(values)


def pairs_unique(pairs):
    result = {}
    for key, value in pairs:
        need(key not in result, "duplicate JSON key: " + key)
        result[key] = value
    return result


def relpath(value):
    need(isinstance(value, str) and bool(value), "empty/non-string relative path")
    path = PurePosixPath(value)
    need(not path.is_absolute() and ".." not in path.parts and "\\" not in value
         and value != ".", "unsafe/nonportable path: " + value)
    return path


def beneath(root, name):
    path = root.joinpath(*relpath(name).parts)
    need(path.resolve().is_relative_to(root.resolve()), "path escapes package/source root: " + name)
    return path


def digest(path):
    sha, size = hashlib.sha256(), 0
    with path.open("rb") as stream:
        while True:
            block = stream.read(1024 * 1024)
            if not block:
                break
            size += len(block)
            sha.update(block)
    return size, sha.hexdigest()


def descriptor(root, row):
    file = beneath(root, row["path"])
    need(file.is_file(), "missing declared file: " + row["path"])
    size, sha = digest(file)
    need(sha == row["sha256"], "SHA mismatch: " + row["path"])
    if "bytes" in row:
        need(size == row["bytes"], "byte-count mismatch: " + row["path"])


def walk_objects(value):
    if isinstance(value, dict):
        yield value
        for child in value.values():
            yield from walk_objects(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk_objects(child)


class PublicVerifier:
    COMMIT = "5c7f1b36eaee6036df280e85835bb0fb4ce0b40d"
    CANDIDATES = {"C01", "C02", "C03", "C05"}
    COVERAGE_FILES = {"coverage/CHECK_RESPONSIBILITY.json", "coverage/METRIC_RESPONSIBILITY.json",
                      "coverage/CHARTS_18_CONTRACTS.json", "coverage/OBJECT_RESPONSIBILITY.json", "coverage/README.md"}
    RESULT = "validation/PUBLIC_PLAN_CHECK.json"
    PRIVATE_ISSUE_KEYS = {"evidence", "original_audit_records", "private_supplemental",
                          "private_supplemental_contract", "private_evidence", "report_evidence"}
    PRIVATE_FILES = {"FULL_REPORT_COVERAGE.json", "REPORT_INTEGRITY.json", "ORIGINAL_DIAGNOSTIC_211.zip",
                     "173_ISSUES_ORIGINAL.json", "ST06_fifth_training_recovery.patch",
                     "TrainingRecoveryR343AdmissionTest.java"}

    def __init__(self, root, source=None, output=None):
        self.root = root.resolve()
        self.source = source.resolve() if source else None
        self.output = output.resolve() if output else None
        self.allowed = set()
        self.scope = {}
        self.identity = {}
        self.issues, self.batches, self.anchors, self.baseline = [], [], [], []
        self.results = []

    def read(self, name):
        return json.loads(beneath(self.root, name).read_text(encoding="utf-8"),
                          object_pairs_hook=pairs_unique)

    def run_check(self, name, function, category="PUBLIC_PLAN"):
        try:
            self.results.append({"check": name, "category": category, "status": "PASS",
                                 **(function() or {})})
        except Exception as error:
            self.results.append({"check": name, "category": category, "status": "FAIL",
                                 "error": type(error).__name__ + ": " + str(error)})

    def pending_output(self):
        if self.output and self.output.is_relative_to(self.root):
            name = self.output.relative_to(self.root).as_posix()
            need(name == self.RESULT, "inside-package output must be " + self.RESULT)
            return name
        return None

    def forbidden_path(self, name):
        path = relpath(name)
        lower = name.lower()
        need(not ({part.lower() for part in path.parts} & {"evidence", "audit", "raw"}),
             "private/raw directory forbidden in public package: " + name)
        need(path.name not in self.PRIVATE_FILES, "private artifact forbidden: " + name)
        need(not re.search(r"\.(?:zip|zipx|csv)(?:$|[.-])", lower),
             "ZIP/CSV forbidden in public package: " + name)
        need(not re.search(r"\.(?:db|db3|sqlite|sqlite3|s3db|mdb|accdb)(?:$|[.-])", lower),
             "database file forbidden in public package: " + name)
        if path.parts[0].lower() == "coverage":
            need(name in self.COVERAGE_FILES, "unapproved/raw coverage inventory: " + name)

    def check_scope(self):
        self.scope = self.read("PUBLIC_SCOPE.json")
        need(isinstance(self.scope.get("allowed_files"), list), "PUBLIC_SCOPE.allowed_files must be an array")
        self.allowed = unique(self.scope["allowed_files"], "public allowed_files")
        required = {"PUBLIC_SCOPE.json", "MANIFEST.json", "PLAN_IDENTITY.json", "ISSUES.json",
                    "BATCHES.json", "ALIAS_MAP.json", "SOURCE_ANCHORS.json", self.RESULT}
        need(required <= self.allowed, "public allowed_files omits required package/control files")
        for name in self.allowed:
            self.forbidden_path(name)
        actual = set()
        for path in self.root.rglob("*"):
            name = path.relative_to(self.root).as_posix()
            need(not path.is_symlink(), "symlink forbidden in public package: " + name)
            if path.is_dir():
                need(path.name.lower() not in {"evidence", "audit", "raw"},
                     "private/raw directory forbidden even when empty: " + name)
                continue
            if not path.is_file():
                continue
            self.forbidden_path(name)
            with path.open("rb") as stream:
                magic = stream.read(16)
            need(not magic.startswith((b"PK\x03\x04", b"PK\x05\x06", b"PK\x07\x08")),
                 "renamed ZIP forbidden: " + name)
            need(magic != b"SQLite format 3\x00", "renamed SQLite database forbidden: " + name)
            actual.add(name)
        pending = self.pending_output()
        anticipated = actual | ({pending} if pending else set())
        need(anticipated == self.allowed,
             "public file-set mismatch: unlisted=" + repr(sorted(actual - self.allowed))
             + "; missing=" + repr(sorted(self.allowed - anticipated)))
        return {"allowed_files": len(self.allowed), "existing_files": len(actual),
                "pending_explicit_output": pending if pending and pending not in actual else None,
                "scope_boundary": "FILE_ALLOWLIST_AND_FORMAT_CHECK; NO_PRIVATE_REPORT_ACCESS"}

    def check_plan(self):
        self.identity = self.read("PLAN_IDENTITY.json")
        expected = {"issue_count": 46, "batch_count": 12, "source_file_count": 136,
                    "baseline_java_count": 1407, "candidate_count": 4}
        for key, value in expected.items():
            need(self.identity[key] == value, "public plan identity count mismatch: " + key)
        need(self.identity["application_source_commit"] == self.COMMIT, "wrong public source baseline")
        issue_doc = self.read("ISSUES.json")
        self.issues = issue_doc["issues"]
        ids = unique((row["id"] for row in self.issues), "public issue IDs")
        need(ids == {f"R344-{n:03d}" for n in range(1, 47)} and issue_doc["count"] == 46,
             "46 public issue IDs mismatch")
        mapping = [(origin, row["id"]) for row in self.issues for origin in row["origin_ids"]]
        need(len(unique((x[0] for x in mapping), "public aliases")) == 50, "expected 50 original aliases")
        need(self.read("ALIAS_MAP.json") == dict(mapping), "public aliases differ from issue origin mapping")
        need(issue_doc["source_commit"] == self.COMMIT, "public issue source mismatch")
        for row in self.issues:
            for obj in walk_objects(row):
                leaked = set(obj) & self.PRIVATE_ISSUE_KEYS
                need(not leaked, "private issue fields remain: " + row["id"] + ": " + repr(sorted(leaked)))
            for key in ("source_files", "preserve", "repair", "acceptance"):
                need(bool(row[key]), "empty public implementation contract: " + row["id"] + "/" + key)
            card = beneath(self.root, "issues/" + row["id"] + ".md").read_text(encoding="utf-8")
            need(row["id"] in card and row["batch"] in card, "public issue card identity mismatch")
            need(all(path in card for path in row["source_files"]), "issue card source list incomplete")
        return {"issues": 46, "aliases": 50, "issue_cards": 46,
                "original_report_evidence": "ABSENT_AND_NOT_VERIFIED"}

    def check_batches(self):
        self.batches = self.read("BATCHES.json")["batches"]
        ids = unique((row["id"] for row in self.batches), "batch IDs")
        need(ids == {f"B{n:02d}" for n in range(12)}, "12 batch IDs mismatch")
        need(all(row["batch"] in ids for row in self.issues), "issue references unknown batch")
        by_id = {row["id"]: row for row in self.batches}
        active, visited, order = set(), set(), []

        def visit(key):
            need(key not in active, "cyclic batch dependency: " + key)
            if key in visited:
                return
            active.add(key)
            for parent in unique(by_id[key]["depends_on"], "batch dependencies"):
                need(parent in by_id, "unknown batch dependency")
                visit(parent)
            active.remove(key)
            visited.add(key)
            order.append(key)

        for row in self.batches:
            visit(row["id"])
            need(row["steps"] and row["exit"], "empty batch implementation/exit contract")
            card = beneath(self.root, "batches/" + row["id"] + ".md").read_text(encoding="utf-8")
            assigned = [x for x in self.issues if x["batch"] == row["id"]]
            need(assigned and all(x["id"] in card for x in assigned), "batch card misses assigned issue")
            need(all(path in card for issue in assigned for path in issue["source_files"]),
                 "batch card source-file list incomplete")
        return {"batches": 12, "dag": "ACYCLIC", "topological_order": order,
                "boundary": "Structural dependencies do not prove runtime liveness."}

    def check_source_catalogs(self):
        doc = self.read("SOURCE_ANCHORS.json")
        self.anchors = doc["files"]
        paths = unique((x["path"] for x in self.anchors), "source anchors")
        need(len(paths) == 136 and doc["source_commit"] == self.COMMIT, "public source anchor identity/count mismatch")
        need(paths == {p for issue in self.issues for p in issue["source_files"]}, "issue/source anchor union differs")
        for row in self.anchors:
            relpath(row["path"])
            need(re.fullmatch(r"[a-f0-9]{64}", row["sha256"]) is not None, "invalid source SHA")
            need(isinstance(row["bytes"], int) and row["bytes"] >= 0, "invalid source byte count")
            need(set(row["issues"]) == {i["id"] for i in self.issues if row["path"] in i["source_files"]},
                 "source anchor issue linkage differs")
        base = self.read("inheritance/APPLICATION_SOURCE_BASELINE.json")
        self.baseline = base["files"]
        names = unique((x["path"] for x in self.baseline), "public Java baseline")
        need(base["count"] == len(names) == 1407 and base["source_commit"] == self.COMMIT,
             "public baseline identity/count mismatch")
        for row in self.baseline:
            relpath(row["path"])
            need(row["path"].startswith("app/src/main/java/") and row["path"].endswith(".java"),
                 "unexpected path in production Java baseline")
            need(re.fullmatch(r"[a-f0-9]{64}", row["sha256"]) is not None, "invalid baseline SHA")
        return {"source_anchor_descriptors": 136, "production_java_descriptors": 1407,
                "scope": "PUBLIC_SOURCE_HASH_CATALOGS; ACTUAL_BYTES_CHECKED_ONLY_WITH_SOURCE_ROOT"}

    def check_source_bytes(self):
        need(self.source is not None and self.source.is_dir(), "source-root missing/not a directory")
        expected = {x["path"] for x in self.baseline}
        actual = {x.relative_to(self.source).as_posix()
                  for x in (self.source / "app/src/main/java").rglob("*.java") if x.is_file()}
        need(actual == expected and len(actual) == 1407, "actual Java files differ from exact public baseline")
        records = {x["path"]: x for x in self.baseline}
        for row in self.anchors:
            if row["path"] in records:
                need(all(row[k] == records[row["path"]][k] for k in ("sha256", "bytes")),
                     "baseline/source descriptor conflict")
            records[row["path"]] = row
        failures = []
        for row in records.values():
            try:
                descriptor(self.source, row)
            except Exception as error:
                failures.append(str(error))
        need(not failures, f"{len(failures)} public source failures: " + "; ".join(failures[:20]))
        return {"java_files_verified": 1407, "source_anchors_verified": 136,
                "unique_files_hashed": len(records), "scope": "PUBLIC_BASELINE_BYTES_NOT_BUILD_OR_PRODUCT_ACCEPTANCE"}

    def check_coverage_contracts(self):
        issue_ids, batch_ids = {x["id"] for x in self.issues}, {x["id"] for x in self.batches}
        metrics = self.read("coverage/METRIC_RESPONSIBILITY.json")
        checks = self.read("coverage/CHECK_RESPONSIBILITY.json")
        charts = self.read("coverage/CHARTS_18_CONTRACTS.json")
        objects = self.read("coverage/OBJECT_RESPONSIBILITY.json")
        metric_ids = unique((x["metric_id"] for x in metrics), "public metric contracts")
        check_ids = unique((x["check_id"] for x in checks), "public check contracts")
        need(len(metric_ids) == 44 and len(check_ids) == 96, "public responsibility counts differ")
        need(unique((x["id"] for x in charts), "public chart contracts") == {f"R{n:02d}" for n in range(1, 19)},
             "18 public chart contracts differ")
        if isinstance(objects, dict):
            objects = objects.get("objects", objects.get("items"))
        need(isinstance(objects, list) and len(objects) == 165, "public object contract index must have 165 rows")
        keys = []
        for row in objects:
            object_id = row.get("object_id", row.get("id", row.get("objectId")))
            kind = next((row[k] for k in ("object_kind", "objectKind", "kind", "category", "group") if k in row), None)
            need(object_id is not None and kind is not None, "public object lacks kind/id composite identity")
            keys.append((kind, object_id))
        unique(keys, "public object kind/id index")
        for rows in (metrics, checks, charts, objects):
            for obj in walk_objects(rows):
                for key in obj:
                    lower = key.lower()
                    need(not (lower == "original" or lower.startswith("original_")
                              or lower.startswith("evidence") or lower == "historical_reference"
                              or lower.startswith("runtime") or "sha256" in lower
                              or lower in {"frame_id", "payload_hash", "run_id", "record_hash", "samplecount"}),
                         "private observation field in sanitized public coverage: " + key)
            for row in rows:
                if "issues" in row:
                    need(row["issues"] and set(row["issues"]) <= issue_ids, "public coverage issue unresolved")
                if "production_batches" in row:
                    need(set(row["production_batches"]) <= batch_ids, "public coverage batch unresolved")
        need(all(set(row.get("metrics", [])) <= metric_ids for row in checks), "unknown metric in public check contract")
        return {"metric_contracts": 44, "check_contracts": 96, "chart_contracts": 18,
                "object_contract_rows": 165,
                "scope": "SANITIZED_PUBLIC_RESPONSIBILITIES_ONLY; NO_ORIGINAL_REPORT_COVERAGE_RECHECK"}

    def check_candidates(self):
        data = self.read("PATCHSET.json")
        need(data["base_source_commit"] == self.COMMIT, "candidate public base commit differs")
        patches = data["patches"]
        need(unique((x["id"] for x in patches), "public candidates") == self.CANDIDATES,
             "public candidates must be exactly C01/C02/C03/C05; private C04 is excluded")
        issues = {x["id"]: x for x in self.issues}
        anchors = {x["path"]: x for x in self.anchors}
        patch_paths = set()
        for row in patches:
            need(row["issue"] in issues and row["batch"] == issues[row["issue"]]["batch"],
                 "candidate issue/batch mismatch")
            need(row["path"].startswith("candidates/") and row["path"].endswith(".patch"),
                 "candidate patch path outside candidates")
            need(row["path"] in self.allowed, "candidate not in public allowlist")
            descriptor(self.root, row)
            patch_paths.add(row["path"])
            for source in row.get("files", []):
                relpath(source["path"])
                before = source.get("before_sha256")
                if source["path"] in anchors and before is not None:
                    need(before == anchors[source["path"]]["sha256"], "candidate before SHA differs from public source anchor")
        actual = {p.relative_to(self.root).as_posix() for p in (self.root / "candidates").rglob("*.patch")}
        need(actual == patch_paths and len(actual) == 4, "unlisted/extra public candidate patch")
        return {"candidate_ids": sorted(self.CANDIDATES), "patch_files_hashed": 4,
                "scope": "PUBLIC_PATCH_BYTES_AND_LINKAGE_ONLY; NOT_APPLIED_OR_EXECUTED"}

    def check_manifest(self):
        data = self.read("MANIFEST.json")
        rows = data["files"]
        exclusions = data.get("exclude", data.get("excludes", []))
        need(isinstance(exclusions, list) and all(isinstance(x, str) for x in exclusions),
             "manifest exclude must be a string array")
        paths = unique((x["path"] for x in rows), "public manifest files")

        def excluded(name):
            return any(fnmatch.fnmatchcase(name, pattern) or
                       (pattern.endswith("/") and name.startswith(pattern)) for pattern in exclusions)

        need(excluded("MANIFEST.json") and excluded(self.RESULT), "manifest must exclude itself and public result")
        need(paths == {p for p in self.allowed if not excluded(p)}, "manifest and public allowlist differ after exclusions")
        for row in rows:
            need("bytes" in row, "manifest descriptor lacks byte count")
            descriptor(self.root, row)
        return {"manifest_files_hashed": len(paths), "exclude": exclusions,
                "scope": "PUBLIC_PACKAGE_BYTES_ONLY"}

    def run(self):
        for name, function in (("public_scope_and_forbidden_artifacts", self.check_scope),
                               ("issues_aliases_cards", self.check_plan), ("batches_cards_dag", self.check_batches),
                               ("public_source_catalogs", self.check_source_catalogs),
                               ("sanitized_responsibility_contracts", self.check_coverage_contracts),
                               ("four_public_candidates", self.check_candidates), ("public_manifest", self.check_manifest)):
            self.run_check(name, function)
        if self.source is not None:
            self.run_check("public_source_bytes", self.check_source_bytes, "PUBLIC_SOURCE")
        else:
            self.results.append({"check": "public_source_bytes", "category": "PUBLIC_SOURCE",
                                 "status": "NOT_RUN", "reason": "SOURCE_ROOT_NOT_PROVIDED"})
        failures = [x for x in self.results if x["status"] == "FAIL"]
        return {"schema": "STARCORE_R344_PUBLIC_PLAN_VERIFICATION_1", "scope": "PUBLIC_PLAN_ONLY",
                "verification_pass": not failures,
                "public_plan_integrity_pass": all(x["status"] == "PASS" for x in self.results if x["category"] == "PUBLIC_PLAN"),
                "source_verification": next(x["status"] for x in self.results if x["category"] == "PUBLIC_SOURCE"),
                "private_original_report_verification": "NOT_PERFORMED; ORIGINALS_AND_FULL_COVERAGE_ARE_NOT_IN_THIS_PACKAGE",
                "private_full_verification": "SEPARATE_PRIVATE_PACKAGE; NOT_REVALIDATED_OR_ATTESTED_BY_THIS_TOOL",
                "product_acceptance": "NOT_RUN_NOT_IMPLIED_BY_PUBLIC_PLAN_PASS", "android_build": "NOT_RUN",
                "physical_device": "NOT_RUN", "natural_quality": "NOT_PROVEN", "candidate_execution": "NOT_RUN",
                "read_only_inputs": True, "checks": self.results, "failure_count": len(failures)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--plan-root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--source-root", type=Path, help="Optional exact public 211 source baseline checkout.")
    parser.add_argument("--output", type=Path, help="Optional JSON result; inside the plan use validation/PUBLIC_PLAN_CHECK.json.")
    args = parser.parse_args()
    checker = PublicVerifier(args.plan_root, args.source_root, args.output)
    result = checker.run()
    text = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        output = args.output.resolve()
        if output.is_relative_to(checker.root):
            need(output.relative_to(checker.root).as_posix() == checker.RESULT, "refusing to overwrite public plan inputs")
            need(checker.RESULT in checker.allowed, "public result path not allowed")
        elif checker.source is not None:
            need(not output.is_relative_to(checker.source), "refusing output inside application source tree")
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(text, encoding="utf-8")
    else:
        sys.stdout.write(text)
    return 0 if result["verification_pass"] else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (Invalid, OSError) as error:
        print("verify_public_plan: " + str(error), file=sys.stderr)
        sys.exit(2)
