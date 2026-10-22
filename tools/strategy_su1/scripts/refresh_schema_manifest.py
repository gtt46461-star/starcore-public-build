#!/usr/bin/env python3
"""Regenerate the schema identity block of manifests/runtime_changes.json from the DDL.

Recomputes DDL SHA-256, per-unit SHA-256 (complete statements, trigger bodies kept
whole), sqlite_master definition hashes and PRAGMA table_info columns. Host only.
"""
from pathlib import Path
import hashlib
import json
import sqlite3

ROOT = Path(__file__).resolve().parents[1]


def units(sql):
    pending = ""
    for line in sql.splitlines(keepends=True):
        pending += line
        if sqlite3.complete_statement(pending):
            yield pending
            pending = ""


def main():
    manifest_path = ROOT / "manifests" / "runtime_changes.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    ddl_bytes = (ROOT / "schema" / "strategy_su1.sql").read_bytes()
    statements = list(units(ddl_bytes.decode("utf-8")))
    db = sqlite3.connect(":memory:")
    db.execute("PRAGMA foreign_keys=ON")
    for s in statements:
        db.execute(s)
    objects = db.execute("SELECT type,name,sql FROM sqlite_master WHERE name LIKE 'su1_%' "
                         "AND sql IS NOT NULL ORDER BY type,name").fetchall()
    tables = [n for t, n, _ in objects if t == "table"]
    manifest["schema"]["sha256"] = hashlib.sha256(ddl_bytes).hexdigest()
    manifest["schema"]["table_count"] = len(tables)
    manifest["schema"]["object_count"] = len(objects)
    manifest["schema"]["review_revision"] = "R10_20261022"
    manifest["migration_units"] = [
        {"unit_id": "SU1_STORAGE_V1_%03d" % i, "sha256": hashlib.sha256(s.encode("utf-8")).hexdigest()}
        for i, s in enumerate(statements)]
    manifest["expected_objects"] = [
        {"type": t, "name": n, "definition_sha256": hashlib.sha256(sql.encode("utf-8")).hexdigest()}
        for t, n, sql in objects]
    manifest["table_columns"] = {
        t: [{"name": r[1], "type": r[2], "not_null": bool(r[3]), "primary_key_order": r[5]}
            for r in db.execute("PRAGMA table_info(" + t + ")")]
        for t in sorted(tables)}
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"tables": len(tables), "objects": len(objects), "units": len(statements),
                      "sha256": manifest["schema"]["sha256"]}))


if __name__ == "__main__":
    main()
