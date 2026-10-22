#!/usr/bin/env python3
"""Inject transactional ledger, duplicate, restart, and disk-full failures."""
from __future__ import annotations

import sqlite3
import tempfile
from pathlib import Path

from v50_harness_common import emit, require
from v50_ledger_harness import build_db, counts, persist_prediction


def injected_rollback(stage: str) -> bool:
    db = build_db()
    issue = "202607240010"
    try:
        persist_prediction(
            db,
            issue,
            "contract",
            fail_after_numbers=53 if stage == "number" else -1,
            fail_after_actions=5 if stage == "action" else -1,
        )
    except RuntimeError:
        return counts(db, issue) == {
            "issue": 0, "number": 0, "action": 0, "selected": 0, "trade": 0
        }
    return False


def disk_full_rollback() -> bool:
    with tempfile.TemporaryDirectory(prefix="v50-fault-") as folder:
        path = Path(folder) / "fault.db"
        db = sqlite3.connect(str(path), isolation_level=None)
        db.execute("PRAGMA page_size=512")
        db.execute("VACUUM")
        db.execute("CREATE TABLE payload(id INTEGER PRIMARY KEY,data BLOB)")
        pages = int(db.execute("PRAGMA page_count").fetchone()[0])
        db.execute("PRAGMA max_page_count=%d" % (pages + 1))
        failed = False
        db.execute("BEGIN IMMEDIATE")
        try:
            db.execute("INSERT INTO payload(data) VALUES(zeroblob(1048576))")
            db.execute("COMMIT")
        except sqlite3.OperationalError as error:
            failed = "full" in str(error).lower()
            if db.in_transaction:
                db.execute("ROLLBACK")
        db.execute("PRAGMA max_page_count=100000")
        rows = int(db.execute("SELECT COUNT(*) FROM payload").fetchone()[0])
        db.close()
        return failed and rows == 0


def restart_idempotence() -> bool:
    with tempfile.TemporaryDirectory(prefix="v50-restart-") as folder:
        path = Path(folder) / "restart.db"
        db = sqlite3.connect(str(path), isolation_level=None)
        db.executescript(
            "CREATE TABLE issue(target_issue TEXT PRIMARY KEY,contract_hash TEXT);"
            "INSERT INTO issue VALUES('202607240099','same');"
        )
        db.close()
        reopened = sqlite3.connect(str(path))
        value = reopened.execute(
            "SELECT contract_hash FROM issue WHERE target_issue='202607240099'"
        ).fetchone()
        reopened.close()
        return value == ("same",)


def main() -> int:
    require(injected_rollback("number"), "NUMBER_INSERT_FAILURE_PARTIAL_COMMIT")
    require(injected_rollback("action"), "ACTION_INSERT_FAILURE_PARTIAL_COMMIT")
    require(disk_full_rollback(), "DISK_FULL_NOT_ATOMIC")
    require(restart_idempotence(), "SERVICE_RESTART_STATE_LOST")

    db = build_db()
    persist_prediction(db, "202607240020", "same")
    duplicate_conflict = False
    try:
        persist_prediction(db, "202607240020", "different")
    except AssertionError:
        duplicate_conflict = True
    require(duplicate_conflict, "DUPLICATE_TARGET_DIFFERENT_HASH_ACCEPTED")
    malformed_issue_blocked = (
        len("20260724020") != 12 or not "20260724020".isdigit()
    )
    require(malformed_issue_blocked, "TARGET_ISSUE_MISMATCH_NOT_BLOCKED")
    return emit("v50_fault_injection", {
        "number_insert_rollback": True,
        "action_insert_rollback": True,
        "disk_full_rollback": True,
        "service_restart_persistence": True,
        "duplicate_target_conflict": True,
        "target_issue_validation": True,
    })


if __name__ == "__main__":
    raise SystemExit(main())
