#!/usr/bin/env python3
"""Read-only plan/evidence verifier; it does not validate the repaired Android product.

Run from any directory.  The default plan root is the parent of this tools directory.
Only an explicitly requested --output file is written.  No ZIP extraction, application
mutation, build, device operation, network request, or candidate application occurs.
"""
from __future__ import annotations

import argparse
import csv
import fnmatch
import hashlib
import io
import json
import re
import sys
import zipfile
from pathlib import Path, PurePosixPath


class VerificationError(Exception):
    pass


def require(condition, message):
    if not condition:
        raise VerificationError(message)


def unique(values, description):
    values = list(values)
    require(len(values) == len(set(values)), description + ": duplicate values")
    return set(values)


def no_duplicate_keys(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, "duplicate JSON key: " + key)
        result[key] = value
    return result


def parse_json(text):
    return json.loads(text, object_pairs_hook=no_duplicate_keys)


def relative_path(value):
    require(isinstance(value, str) and value, "empty/non-string path")
    path = PurePosixPath(value)
    require(not path.is_absolute() and ".." not in path.parts and "\\" not in value,
            "unsafe/nonportable relative path: " + value)
    require(value not in (".", ""), "empty path")
    return path


def inside(root, value):
    path = root.joinpath(*relative_path(value).parts)
    require(path.resolve().is_relative_to(root.resolve()), "path escapes root: " + value)
    return path


def hash_stream(stream):
    digest, size = hashlib.sha256(), 0
    while True:
        block = stream.read(1024 * 1024)
        if not block:
            break
        digest.update(block)
        size += len(block)
    return size, digest.hexdigest()


def hash_file(path):
    with path.open("rb") as stream:
        return hash_stream(stream)


def check_descriptor(root, descriptor):
    name = descriptor["path"]
    path = inside(root, name)
    require(path.is_file(), "missing file: " + name)
    size, sha = hash_file(path)
    require(size == descriptor["bytes"], "byte count mismatch: " + name)
    require(sha == descriptor["sha256"], "SHA256 mismatch: " + name)


class Verifier:
    OBJECT_COUNTS = {"capabilities": 82, "researchCharts": 18,
                     "legacyCharts": 29, "matrices": 20, "logicalTables": 16}

    def __init__(self, plan_root, source_root):
        self.root = plan_root.resolve()
        self.source = source_root.resolve() if source_root else None
        self.checks = []
        self.identity = None
        self.issues = None
        self.batches = None
        self.anchors = None
        self.baseline = None
        self.coverage = None
        self.report_facts = None

    def read(self, name):
        return parse_json(inside(self.root, name).read_text(encoding="utf-8"))

    def record(self, name, status, category="PLAN", **details):
        self.checks.append({"check": name, "category": category,
                            "status": status, **details})

    def run_check(self, name, function, category="PLAN"):
        try:
            details = function() or {}
            self.record(name, "PASS", category, **details)
        except Exception as error:
            self.record(name, "FAIL", category, error=type(error).__name__ + ": " + str(error))

    def check_identity(self):
        self.identity = self.read("PLAN_IDENTITY.json")
        require(self.identity["issue_count"] == 46, "expected 46 plan issues")
        require(self.identity["batch_count"] == 12, "expected 12 batches")
        require(self.identity["baseline_java_count"] == 1407, "expected 1407 baseline Java files")
        for field, count in (("checks", 96), ("metrics", 44), ("objects", 165),
                             ("database_tables", 826), ("research_charts", 18)):
            require(self.identity[field] == count, "identity count mismatch: " + field)
        require(self.identity["application_source_commit"] ==
                "5c7f1b36eaee6036df280e85835bb0fb4ce0b40d", "wrong audited application commit")
        return {"source_commit": self.identity["application_source_commit"],
                "scope": "PLAN_IDENTITY_ONLY_NOT_APK_IDENTITY"}

    def check_issues(self):
        data = self.read("ISSUES.json")
        aliases = self.read("ALIAS_MAP.json")
        self.issues = data["issues"]
        ids = unique((x["id"] for x in self.issues), "issue IDs")
        require(ids == {f"R344-{n:03d}" for n in range(1, 47)}, "46 canonical issue IDs mismatch")
        require(data["count"] == len(self.issues) == 46, "issue count mismatch")
        origin_pairs = [(origin, row["id"]) for row in self.issues for origin in row["origin_ids"]]
        origins = unique((x[0] for x in origin_pairs), "original finding IDs")
        require(len(origins) == 50 and aliases == dict(origin_pairs), "50 aliases do not match issue origins")
        for row in self.issues:
            require(row["execution_status"] == "PLAN_DEFINED_PRODUCT_ACCEPTANCE_PENDING",
                    "plan issue claims unexpected product completion: " + row["id"])
            require(row["origin_id"] in row["origin_ids"], "primary origin not in aliases")
            original_ids = {x["id"] for x in row["original_audit_records"]}
            require(original_ids == set(row["origin_ids"]), "original audit records lost: " + row["id"])
            for field in ("evidence", "cause", "source_files", "preserve", "repair", "acceptance"):
                require(bool(row[field]), "empty issue contract " + row["id"] + ": " + field)
            card = inside(self.root, "issues/" + row["id"] + ".md").read_text(encoding="utf-8")
            require(row["id"] in card and row["batch"] in card, "issue card identity mismatch: " + row["id"])
            require(all(path in card for path in row["source_files"]), "issue card omits source file: " + row["id"])
        require(data["source_commit"] == self.identity["application_source_commit"], "issue source mismatch")
        return {"issues": 46, "unique_original_findings": 50, "issue_cards": 46}

    def check_batches(self):
        self.batches = self.read("BATCHES.json")["batches"]
        ids = unique((x["id"] for x in self.batches), "batch IDs")
        require(ids == {f"B{n:02d}" for n in range(12)}, "12 canonical batches mismatch")
        require(all(row["batch"] in ids for row in self.issues), "unknown issue batch")
        by_id = {x["id"]: x for x in self.batches}
        visiting, seen, order = set(), set(), []

        def visit(batch):
            require(batch not in visiting, "cyclic batch dependency: " + batch)
            if batch in seen:
                return
            visiting.add(batch)
            parents = by_id[batch]["depends_on"]
            unique(parents, "batch dependencies " + batch)
            for parent in parents:
                require(parent in ids, "unknown dependency: " + parent)
                visit(parent)
            visiting.remove(batch)
            seen.add(batch)
            order.append(batch)

        for batch in self.batches:
            visit(batch["id"])
            require(batch["steps"] and batch["exit"], "batch lacks implementation/exit contract")
            card = inside(self.root, "batches/" + batch["id"] + ".md").read_text(encoding="utf-8")
            require(batch["id"] in card, "batch card identity mismatch")
            rows = [x for x in self.issues if x["batch"] == batch["id"]]
            require(rows, "batch has no assigned issues: " + batch["id"])
            require(all(x["id"] in card for x in rows), "batch card omits issue: " + batch["id"])
            require(all(path in card for row in rows for path in row["source_files"]),
                    "batch card omits source file: " + batch["id"])
        return {"batches": 12, "dag": "ACYCLIC", "topological_order": order,
                "boundary": "Structural DAG check does not prove runtime liveness."}

    def check_source_catalogs(self):
        data = self.read("SOURCE_ANCHORS.json")
        self.anchors = data["files"]
        paths = unique((x["path"] for x in self.anchors), "source anchor paths")
        expected = {path for row in self.issues for path in row["source_files"]}
        require(paths == expected, "source anchors != issue source-file union")
        require(len(paths) == self.identity["source_file_count"], "source_file_count not regenerated")
        require(data["source_commit"] == self.identity["application_source_commit"], "anchor source mismatch")
        matrix = inside(self.root, "FILE_MODIFICATION_MATRIX.md").read_text(encoding="utf-8")
        for anchor in self.anchors:
            relative_path(anchor["path"])
            require(re.fullmatch(r"[a-f0-9]{64}", anchor["sha256"]) is not None, "invalid source SHA")
            require(isinstance(anchor["bytes"], int) and anchor["bytes"] >= 0, "invalid source byte count")
            expected_issues = {row["id"] for row in self.issues if anchor["path"] in row["source_files"]}
            require(set(anchor["issues"]) == expected_issues, "source anchor issue linkage mismatch")
            require(anchor["path"] in matrix, "file matrix omits source: " + anchor["path"])
        base = self.read("inheritance/APPLICATION_SOURCE_BASELINE.json")
        self.baseline = base["files"]
        baseline_paths = unique((x["path"] for x in self.baseline), "baseline Java paths")
        require(base["count"] == len(baseline_paths) == 1407, "baseline Java inventory != 1407")
        require(base["source_commit"] == data["source_commit"], "baseline source mismatch")
        for row in self.baseline:
            relative_path(row["path"])
            require(row["path"].startswith("app/src/main/java/") and row["path"].endswith(".java"),
                    "non-production-Java entry in baseline")
            require(re.fullmatch(r"[a-f0-9]{64}", row["sha256"]) is not None, "invalid baseline SHA")
            require(isinstance(row["bytes"], int) and row["bytes"] >= 0, "invalid baseline bytes")
        return {"source_anchor_files": len(paths), "baseline_java_inventory": 1407,
                "scope": "DESCRIPTOR_UNION_ONLY; ACTUAL_BYTES_HAVE_A_SEPARATE_SOURCE_CHECK"}

    def check_coverage(self):
        self.coverage = self.read("coverage/FULL_REPORT_COVERAGE.json")
        coverage = self.coverage
        checks = unique((x["id"] for x in coverage["checks"]), "coverage check IDs")
        metrics = unique((x["id"] for x in coverage["metrics"]), "coverage metric IDs")
        tables = unique((x["table"] for x in coverage["table_inventory"]), "coverage table names")
        require((len(checks), len(metrics), len(tables)) == (96, 44, 826), "coverage counts mismatch")
        require(coverage["sourceCommit"] == self.identity["application_source_commit"], "coverage source mismatch")
        require(coverage["snapshotSha256"] == self.identity["report_snapshot_sha256"], "coverage snapshot mismatch")
        total = 0
        for group, count in self.OBJECT_COUNTS.items():
            entry = coverage["objects"][group]
            names = unique((x["id"] for x in entry["objects"]), "object IDs in " + group)
            require(len(names) == entry["expected"] == entry["shown"] == count,
                    "object group count mismatch: " + group)
            require(entry["omitted"] == 0, "omitted objects: " + group)
            total += len(names)
        require(total == coverage["summary"]["registered_index_rows"] == 165, "165 index rows mismatch")
        issue_ids, batch_ids = {x["id"] for x in self.issues}, {x["id"] for x in self.batches}
        for filename, key, expected in (("CHECK_RESPONSIBILITY.json", "check_id", checks),
                                        ("METRIC_RESPONSIBILITY.json", "metric_id", metrics)):
            rows = self.read("coverage/" + filename)
            require(unique((x[key] for x in rows), filename) == expected, "responsibility coverage mismatch")
            for row in rows:
                require(row["issues"] and set(row["issues"]) <= issue_ids, "unresolved responsibility issue")
                if key == "check_id":
                    require(set(row["metrics"]) <= metrics, "check references unknown metric")
                    require(row["original"]["id"] == row[key], "check original identity mismatch")
                else:
                    require(row["production_batches"] and set(row["production_batches"]) <= batch_ids,
                            "metric references unknown production batch")
                    require(row["original_definition"]["id"] == row[key], "metric definition identity mismatch")
        charts = self.read("coverage/CHARTS_18_CONTRACTS.json")
        chart_ids = unique((x["id"] for x in charts), "chart contracts")
        require(chart_ids == {f"R{n:02d}" for n in range(1, 19)}, "18 chart IDs mismatch")
        require(chart_ids == {x["id"] for x in coverage["objects"]["researchCharts"]["objects"]},
                "chart contracts and object coverage differ")
        require(chart_ids == {x["id"] for x in coverage["chart_construction_contracts"]},
                "embedded chart contract coverage differs")
        for row in charts:
            require(all(row.get(k) for k in ("producer", "inputs", "consumer", "acceptance_test")),
                    "incomplete chart contract: " + row["id"])
        return {"checks": 96, "metrics": 44, "indexed_object_rows": 165,
                "database_tables": 826, "research_chart_contracts": 18,
                "object_count_boundary": "20 matrix aliases overlap capabilities; 165 is not 165 independent capabilities."}

    def check_inheritance(self):
        old = self.read("inheritance/173_ISSUES_ORIGINAL.json")
        old_ids = unique((x["id"] for x in old["issues"]), "old issue IDs")
        require(old["count"] == len(old_ids) == 44, "old issue count != 44")
        crosswalk = self.read("inheritance/173_ISSUE_CROSSWALK.json")
        require(unique((x["old_id"] for x in crosswalk), "old issue crosswalk") == old_ids,
                "old issue crosswalk incomplete")
        issue_ids = {x["id"] for x in self.issues}
        for row in crosswalk:
            require(row["previous_contract_retained"] is True, "old contract not retained")
            require(row["new_issues"] and set(row["new_issues"]) <= issue_ids, "old crosswalk target unresolved")
        counts = {}
        for name, key, expected in (("REQUIREMENTS_CURRENT_INVENTORY.csv", "requirement_id", 1832),
                                    ("CARDS_CURRENT_INVENTORY.csv", "card_id", 299)):
            with inside(self.root, "inheritance/" + name).open(encoding="utf-8-sig", newline="") as stream:
                rows = list(csv.DictReader(stream))
            require(len(unique((x[key] for x in rows), name)) == expected, "inherited inventory count mismatch")
            require(all(x["this_plan_disposition"] == "RETAINED_CONTRACT_NOT_AUTO_ACCEPTED" for x in rows),
                    "inherited contract auto-accepted")
            counts[key] = len(rows)
        return {"old_issues": 44, **counts}

    @staticmethod
    def zip_names(archive):
        infos = archive.infolist()
        unique((x.filename for x in infos), "ZIP names")
        for info in infos:
            relative_path(info.filename.rstrip("/"))
        return {x.filename for x in infos if not x.is_dir()}

    def check_original_zip(self):
        integrity = self.read("evidence/REPORT_INTEGRITY.json")
        path = inside(self.root, "evidence/ORIGINAL_DIAGNOSTIC_211.zip")
        size, sha = hash_file(path)
        require(size == integrity["outer_bytes"], "outer ZIP bytes mismatch")
        require(sha == integrity["outer_sha256"] == self.identity["report_outer_sha256"], "outer ZIP SHA mismatch")
        with zipfile.ZipFile(path) as outer:
            outer_names = self.zip_names(outer)
            indices = [n for n in outer_names if PurePosixPath(n).name == "index.json"]
            require(len(indices) == 1, "outer index missing/ambiguous")
            index = parse_json(outer.read(indices[0]))
            require(index == integrity["outer_index"], "outer index differs from integrity record")
            require(index["expectedVolumes"] == index["exportedVolumes"] == len(index["files"]) == 1,
                    "unexpected volume count")
            require(index["omittedVolumes"] == 0, "omitted outer volume")
            item = index["files"][0]
            names = [n for n in outer_names if PurePosixPath(n).name == item["name"]]
            require(len(names) == 1 and outer_names == {indices[0], names[0]}, "unexpected outer payload")
            inner_bytes = outer.read(names[0])  # Full reads verify these ZIP members' CRC.
            require(len(inner_bytes) == item["bytes"] == integrity["inner_bytes"], "inner ZIP bytes mismatch")
            inner_sha = hashlib.sha256(inner_bytes).hexdigest()
            require(inner_sha == item["sha256"] == integrity["inner_sha256"], "inner ZIP SHA mismatch")
        with zipfile.ZipFile(io.BytesIO(inner_bytes)) as inner:
            inner_names = self.zip_names(inner)
            manifest_bytes = inner.read("ARCHIVE_MANIFEST.jsonl")
            require(hashlib.sha256(manifest_bytes).hexdigest() == index["manifestSha256"], "archive manifest SHA mismatch")
            lines = manifest_bytes.splitlines(keepends=True)
            records = [parse_json(line) for line in lines]
            require(records[0]["kind"] == "header" and records[-1]["kind"] == "footer", "manifest envelope invalid")
            require(records[0]["report_id"] == index["jobId"], "manifest report identity mismatch")
            footer = records[-1]
            require(hashlib.sha256(b"".join(lines[:-1])).hexdigest() == footer["manifest_body_sha256"],
                    "manifest body hash mismatch")
            members = [row for row in records if row["kind"] == "member"]
            parts = [row for row in records if row["kind"] == "part"]
            member_names = unique((x["name"] for x in members), "manifest payloads")
            require(len(members) == footer["members"] == 56, "manifest payload count != 56")
            require(len(parts) == footer["parts"], "manifest part descriptor count mismatch")
            require(inner_names == member_names | {"ARCHIVE_MANIFEST.jsonl"}, "inner ZIP payload set mismatch")
            saved = integrity["member_checks"]
            require(unique((x["name"] for x in saved), "saved member checks") == member_names,
                    "saved integrity payload set mismatch")
            saved = {x["name"]: x for x in saved}
            for row in members:
                with inner.open(row["name"]) as stream:
                    actual_size, actual_sha = hash_stream(stream)  # EOF also verifies ZIP CRC.
                require(actual_size == row["raw_bytes"] == saved[row["name"]]["bytes"],
                        "payload bytes mismatch: " + row["name"])
                require(actual_sha == row["raw_sha256"] == saved[row["name"]]["sha256"],
                        "payload SHA mismatch: " + row["name"])
            semantic = parse_json(inner.read("R31_SEMANTIC_RECEIPT.json"))
            source = parse_json(inner.read("source-manifest.json"))
            r30 = parse_json(inner.read("R30_REPORT.json"))
            facts = {"checks": semantic["checks"],
                     "metrics": {x["id"]: x["status"] for x in r30["metricResults"]},
                     "objects": {group: {x["id"] for x in r30["objectIndex"][group]["objects"]}
                                 for group in self.OBJECT_COUNTS}}
            del r30
            diagnostic = parse_json(inner.read("DIAGNOSTIC_SCOPE.json"))
            facts["tables"] = {x["table"]: x["rows"] for x in diagnostic["tables"]}
            require(len(facts["tables"]) == len(diagnostic["tables"]) == 826, "original table count/uniqueness mismatch")
            for obj in (source, semantic, diagnostic):
                require(obj["sourceCommit"] == self.identity["application_source_commit"], "original source identity mismatch")
                require(obj["snapshotSha256"] == self.identity["report_snapshot_sha256"], "original snapshot identity mismatch")
            self.report_facts = facts
        return {"outer_sha256": sha, "inner_sha256": inner_sha, "manifest_payloads_verified": 56,
                "crc": "ALL_OUTER_AND_INNER_FILE_MEMBERS_READ_TO_EOF_WITH_ZIP_CRC_CHECK",
                "archive_part_boundary": "Original zstd part descriptors are retained; absent original part bytes are not claimed to be replayed.",
                "original_checks_complete": index["checksComplete"],
                "original_all_checks_passed": index["allChecksPassed"]}

    def check_coverage_against_original(self):
        require(self.report_facts is not None, "original ZIP verification did not complete")
        require(self.coverage is not None, "coverage verification did not complete")
        facts, coverage = self.report_facts, self.coverage
        planned = {x["id"]: x for x in coverage["checks"]}
        require(set(planned) == set(facts["checks"]), "96 check IDs differ from original report")
        for check_id, original in facts["checks"].items():
            for field in ("execution_state", "domain_outcome", "health", "completes_contract"):
                require(planned[check_id][field] == original[field], "check finding changed: " + check_id + "/" + field)
        require({x["id"]: x["status"] for x in coverage["metrics"]} == facts["metrics"],
                "44 metric IDs/statuses differ from original")
        require({x["table"]: x["rows"] for x in coverage["table_inventory"]} == facts["tables"],
                "826 table names/counts differ from original")
        for group, ids in facts["objects"].items():
            require(ids == {x["id"] for x in coverage["objects"][group]["objects"]},
                    "object IDs differ from original: " + group)
        return {"original_report_coverage_matched": True,
                "business_results_preserved": "No FAIL/UNKNOWN/READ_ERROR was converted into product PASS."}

    def check_source_bytes(self):
        require(self.source is not None and self.source.is_dir(), "source-root is not a directory")
        require(self.anchors is not None and self.baseline is not None, "source inventories unavailable")
        expected_java = {x["path"] for x in self.baseline}
        java_root = self.source / "app/src/main/java"
        actual_java = {p.relative_to(self.source).as_posix() for p in java_root.rglob("*.java") if p.is_file()}
        require(actual_java == expected_java, "actual production Java paths differ from 1407-file baseline; "
                f"missing={len(expected_java - actual_java)}, extra={len(actual_java - expected_java)}")
        records = {x["path"]: x for x in self.baseline}
        for row in self.anchors:
            if row["path"] in records:
                require(all(records[row["path"]][k] == row[k] for k in ("sha256", "bytes")),
                        "anchor/baseline descriptor mismatch: " + row["path"])
            records[row["path"]] = row
        errors = []
        for row in records.values():
            try:
                check_descriptor(self.source, row)
            except Exception as error:
                errors.append(str(error))
        require(not errors, f"{len(errors)} source-byte failures: " + "; ".join(errors[:20]))
        return {"baseline_java_verified": 1407, "source_anchor_files_verified": len(self.anchors),
                "unique_source_files_hashed": len(records),
                "scope": "EXACT_AUDITED_BASELINE_BYTES_NOT_COMPILED_PRODUCT"}

    def check_patchset(self):
        data = self.read("PATCHSET.json")
        require(isinstance(data, (dict, list)), "PATCHSET must be object/list")
        checked = []

        if isinstance(data, dict) and "base_source_commit" in data:
            require(data["base_source_commit"] == self.identity["application_source_commit"],
                    "PATCHSET source commit mismatch")
        if isinstance(data, dict) and "patches" in data:
            rows = data["patches"]
            require(isinstance(rows, list), "PATCHSET patches must be a list")
            unique((row["id"] for row in rows), "candidate patch IDs")
            issues = {row["id"]: row for row in self.issues}
            anchors = {row["path"]: row for row in self.anchors}
            for row in rows:
                if "issue" in row:
                    require(row["issue"] in issues, "candidate references unknown issue")
                    if "batch" in row:
                        require(row["batch"] == issues[row["issue"]]["batch"],
                                "candidate issue/batch mismatch")
                for source in row.get("files", []):
                    relative_path(source["path"])
                    for key in ("before_sha256", "after_sha256"):
                        value = source.get(key)
                        require(value is None or re.fullmatch(r"[a-f0-9]{64}", value) is not None,
                                "invalid candidate source SHA")
                    if source["path"] in anchors and source.get("before_sha256") is not None:
                        require(source["before_sha256"] == anchors[source["path"]]["sha256"],
                                "candidate before SHA differs from source anchor: " + source["path"])

        def walk(value):
            if isinstance(value, dict):
                if all(key in value for key in ("path", "sha256")):
                    path = inside(self.root, value["path"])
                    require(path.is_file(), "missing declared candidate file: " + value["path"])
                    size, sha = hash_file(path)
                    require(sha == value["sha256"], "candidate SHA mismatch: " + value["path"])
                    if "bytes" in value:
                        require(size == value["bytes"], "candidate bytes mismatch: " + value["path"])
                    checked.append(value["path"])
                for child in value.values():
                    walk(child)
            elif isinstance(value, list):
                for child in value:
                    walk(child)

        walk(data)
        return {"available_file_descriptors_checked": len(checked),
                "scope": "JSON_AND_DECLARED_FILE_HASHES_ONLY; PATCHES_NOT_APPLIED_OR_EXECUTED"}

    def check_manifest(self):
        data = self.read("MANIFEST.json")
        rows, exclusions = data["files"], data.get("exclude", data.get("excludes", []))
        require(isinstance(exclusions, list) and all(isinstance(x, str) for x in exclusions),
                "manifest exclude must be a list of relative strings/patterns")
        paths = unique((x["path"] for x in rows), "manifest file paths")

        def excluded(name):
            return any(fnmatch.fnmatchcase(name, pattern) or
                       (pattern.endswith("/") and name.startswith(pattern)) for pattern in exclusions)

        for row in rows:
            check_descriptor(self.root, row)
            require(not excluded(row["path"]), "manifest both lists and excludes file: " + row["path"])
        actual = {p.relative_to(self.root).as_posix() for p in self.root.rglob("*") if p.is_file()}
        required = {name for name in actual if not excluded(name)}
        require(paths == required, "manifest file-set mismatch: missing=" + repr(sorted(required - paths)[:20])
                + ", nonexistent=" + repr(sorted(paths - required)[:20]))
        return {"files_verified": len(rows), "exclude": exclusions,
                "scope": "CURRENT_PLAN_PACKAGE_BYTES_ONLY"}

    def run(self):
        for name, function in (("plan_identity", self.check_identity), ("issues_aliases_cards", self.check_issues),
                               ("batches_cards_dag", self.check_batches), ("source_anchor_catalogs", self.check_source_catalogs),
                               ("coverage_inventories", self.check_coverage), ("inherited_contracts", self.check_inheritance),
                               ("original_zip_integrity", self.check_original_zip),
                               ("coverage_matches_original_report", self.check_coverage_against_original)):
            self.run_check(name, function)
        if self.source is None:
            self.record("source_bytes", "NOT_RUN", "SOURCE", reason="SOURCE_ROOT_NOT_PROVIDED")
        else:
            self.run_check("source_bytes", self.check_source_bytes, "SOURCE")
        for name, function in (("PATCHSET.json", self.check_patchset), ("MANIFEST.json", self.check_manifest)):
            if (self.root / name).is_file():
                self.run_check(name, function, "OPTIONAL_PACKAGE")
            else:
                self.record(name, "NOT_RUN", "OPTIONAL_PACKAGE", reason="OPTIONAL_FILE_NOT_PRESENT")
        failures = [x for x in self.checks if x["status"] == "FAIL"]
        plan_pass = all(x["status"] == "PASS" for x in self.checks if x["category"] == "PLAN")
        source_result = next(x["status"] for x in self.checks if x["category"] == "SOURCE")
        return {"schema": "STARCORE_R344_READ_ONLY_PLAN_VERIFICATION_1",
                "verification_pass": not failures, "plan_integrity_pass": plan_pass,
                "source_verification": source_result,
                "optional_package_checks_not_run": [x["check"] for x in self.checks
                                                    if x["category"] == "OPTIONAL_PACKAGE" and x["status"] == "NOT_RUN"],
                "product_acceptance": "NOT_RUN_NOT_IMPLIED_BY_PLAN_PASS",
                "android_build": "NOT_RUN", "physical_device": "NOT_RUN", "natural_quality": "NOT_PROVEN",
                "candidate_execution": "NOT_RUN", "read_only": True,
                "checks": self.checks, "failure_count": len(failures)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--plan-root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--source-root", type=Path,
                        help="Optional exact 211 baseline checkout; modified or incomplete trees fail source verification.")
    parser.add_argument("--output", type=Path, help="Optional JSON result; otherwise print JSON to stdout.")
    args = parser.parse_args()
    result = Verifier(args.plan_root, args.source_root).run()
    text = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        output = args.output.resolve()
        plan_validation = args.plan_root.resolve() / "validation"
        require(not args.source_root or not output.is_relative_to(args.source_root.resolve())
                or output.is_relative_to(plan_validation),
                "output inside the source checkout must use the plan validation directory")
        if output.is_relative_to(args.plan_root.resolve()):
            relative = output.relative_to(args.plan_root.resolve()).as_posix()
            require(relative not in {"PLAN_IDENTITY.json", "ISSUES.json", "BATCHES.json", "ALIAS_MAP.json",
                                     "SOURCE_ANCHORS.json", "PATCHSET.json", "MANIFEST.json"}
                    and not relative.startswith(("issues/", "batches/", "contracts/", "coverage/", "inheritance/",
                                                 "evidence/", "candidates/", "tools/")),
                    "output must not overwrite plan inputs; use a validation result path")
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(text, encoding="utf-8")
    else:
        sys.stdout.write(text)
    return 0 if result["verification_pass"] else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, VerificationError) as error:
        print("verify_plan: " + str(error), file=sys.stderr)
        sys.exit(2)
