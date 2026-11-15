#!/usr/bin/env python3
"""Run additive/rollback SQLite migration simulations for V47/V48/V49 shapes."""
from __future__ import annotations

import sqlite3

from v50_harness_common import emit, read_java, require


REQUIRED = {
    "strategy_cohort": "TEXT NOT NULL DEFAULT ''",
    "probability_owner": "TEXT NOT NULL DEFAULT ''",
    "rank_owner": "TEXT NOT NULL DEFAULT ''",
    "lane_owner": "TEXT NOT NULL DEFAULT ''",
    "contract_hash": "TEXT NOT NULL DEFAULT ''",
}


def columns(db: sqlite3.Connection, table: str) -> set[str]:
    return {str(row[1]) for row in db.execute("PRAGMA table_info(%s)" % table)}


def migrate(db: sqlite3.Connection, inject_failure: bool = False) -> None:
    db.execute("BEGIN IMMEDIATE")
    try:
        db.execute(
            "CREATE TABLE IF NOT EXISTS decisions "
            "(target_issue TEXT PRIMARY KEY,decision TEXT NOT NULL DEFAULT 'HOLD')"
        )
        current = columns(db, "decisions")
        for name, definition in REQUIRED.items():
            if name not in current:
                db.execute("ALTER TABLE decisions ADD COLUMN %s %s"
                           % (name, definition))
        db.execute(
            "CREATE INDEX IF NOT EXISTS idx_v50_decision_cohort "
            "ON decisions(strategy_cohort,target_issue)"
        )
        db.execute(
            "CREATE TABLE IF NOT EXISTS v50_schema_meta "
            "(meta_key TEXT PRIMARY KEY,meta_value TEXT NOT NULL)"
        )
        db.execute(
            "INSERT OR REPLACE INTO v50_schema_meta VALUES('schema_checksum','v50-test')"
        )
        if inject_failure:
            raise RuntimeError("INJECT_MIGRATION_FAILURE")
        db.execute("PRAGMA user_version=33")
        db.execute("COMMIT")
    except BaseException:
        db.execute("ROLLBACK")
        raise


def legacy(version: str) -> sqlite3.Connection:
    db = sqlite3.connect(":memory:", isolation_level=None)
    if version == "V47":
        db.execute("CREATE TABLE decisions(target_issue TEXT PRIMARY KEY)")
    elif version == "V48":
        db.execute(
            "CREATE TABLE decisions(target_issue TEXT PRIMARY KEY,"
            "decision TEXT NOT NULL DEFAULT 'HOLD',strategy_cohort TEXT)"
        )
    elif version == "V49":
        db.execute(
            "CREATE TABLE decisions(target_issue TEXT PRIMARY KEY,"
            "decision TEXT NOT NULL DEFAULT 'HOLD',strategy_cohort TEXT,"
            "probability_owner TEXT)"
        )
    else:
        raise ValueError(version)
    db.execute("INSERT INTO decisions(target_issue) VALUES('202607240001')")
    return db


def main() -> int:
    upgraded = {}
    for version in ("V47", "V48", "V49"):
        db = legacy(version)
        migrate(db)
        after = columns(db, "decisions")
        require(set(REQUIRED).issubset(after), "MIGRATION_COLUMNS_MISSING", version)
        require(db.execute("PRAGMA user_version").fetchone()[0] == 33,
                "USER_VERSION_NOT_33", version)
        require(db.execute(
            "SELECT COUNT(*) FROM decisions WHERE target_issue='202607240001'"
        ).fetchone()[0] == 1, "LEGACY_ROW_LOST", version)
        index_names = {
            str(row[1]) for row in db.execute("PRAGMA index_list(decisions)")
        }
        require("idx_v50_decision_cohort" in index_names,
                "MIGRATION_INDEX_MISSING", version)
        upgraded[version] = len(after)

    rollback = legacy("V47")
    before = columns(rollback, "decisions")
    failed = False
    try:
        migrate(rollback, inject_failure=True)
    except RuntimeError:
        failed = True
    require(failed, "MIGRATION_FAULT_NOT_RAISED")
    require(columns(rollback, "decisions") == before,
            "FAILED_MIGRATION_NOT_ROLLED_BACK")
    require(rollback.execute("PRAGMA user_version").fetchone()[0] == 0,
            "FAILED_MIGRATION_CHANGED_USER_VERSION")

    source = read_java("DatabaseMigrationEngine.java")
    for token in (
        "PRAGMA table_info", "ALTER TABLE", "beginTransaction",
        "setTransactionSuccessful", "SchemaContractRegistry.checksum()",
    ):
        require(token in source, "JAVA_MIGRATION_CONTRACT_MISSING", token)
    backup = read_java("DatabaseBackupEngine.java")
    require("restore" in backup.lower(), "DATABASE_BACKUP_RESTORE_MISSING")
    return emit("v50_database_migration", {
        "legacy_shapes_upgraded": upgraded,
        "legacy_rows_preserved": True,
        "index_created": True,
        "fault_rollback_verified": True,
        "target_user_version": 33,
    })


if __name__ == "__main__":
    raise SystemExit(main())

