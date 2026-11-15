#!/usr/bin/env python3
"""Exercise the V50 1/10/100 ledger contract with real SQLite constraints."""
from __future__ import annotations

import sqlite3

from v50_harness_common import emit, require


SCHEMA = """
PRAGMA foreign_keys=ON;
CREATE TABLE issue (
  target_issue TEXT PRIMARY KEY,
  contract_hash TEXT NOT NULL,
  settled INTEGER NOT NULL DEFAULT 0 CHECK(settled IN (0,1))
);
CREATE TABLE number_event (
  target_issue TEXT NOT NULL REFERENCES issue(target_issue),
  position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 10),
  number INTEGER NOT NULL CHECK(number BETWEEN 1 AND 10),
  predicted_p REAL NOT NULL CHECK(predicted_p BETWEEN 0 AND 1),
  actual_label INTEGER NOT NULL DEFAULT -1 CHECK(actual_label IN (-1,0,1)),
  settled INTEGER NOT NULL DEFAULT 0 CHECK(settled IN (0,1)),
  PRIMARY KEY(target_issue,position,number)
);
CREATE TABLE position_action (
  target_issue TEXT NOT NULL REFERENCES issue(target_issue),
  position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 10),
  n1 INTEGER NOT NULL, n2 INTEGER NOT NULL, n3 INTEGER NOT NULL,
  actual_hit INTEGER NOT NULL DEFAULT -1 CHECK(actual_hit IN (-1,0,1)),
  settled INTEGER NOT NULL DEFAULT 0 CHECK(settled IN (0,1)),
  CHECK(n1 BETWEEN 1 AND 10 AND n2 BETWEEN 1 AND 10 AND n3 BETWEEN 1 AND 10),
  CHECK(n1<>n2 AND n1<>n3 AND n2<>n3),
  PRIMARY KEY(target_issue,position)
);
CREATE TABLE selected_decision (
  target_issue TEXT PRIMARY KEY REFERENCES issue(target_issue),
  decision_id TEXT NOT NULL UNIQUE,
  position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 10),
  n1 INTEGER NOT NULL, n2 INTEGER NOT NULL, n3 INTEGER NOT NULL,
  action TEXT NOT NULL CHECK(action IN ('BET','SHADOW','HOLD')),
  settled INTEGER NOT NULL DEFAULT 0 CHECK(settled IN (0,1)),
  CHECK(n1<>n2 AND n1<>n3 AND n2<>n3)
);
CREATE TABLE authorization (
  decision_id TEXT PRIMARY KEY REFERENCES selected_decision(decision_id),
  target_issue TEXT NOT NULL UNIQUE REFERENCES issue(target_issue),
  contract_hash TEXT NOT NULL,
  owner TEXT NOT NULL
);
CREATE TABLE trade (
  target_issue TEXT PRIMARY KEY REFERENCES issue(target_issue),
  decision_id TEXT NOT NULL UNIQUE REFERENCES selected_decision(decision_id),
  action TEXT NOT NULL CHECK(action='BET')
);
"""


def build_db() -> sqlite3.Connection:
    db = sqlite3.connect(":memory:", isolation_level=None)
    db.executescript(SCHEMA)
    return db


def counts(db: sqlite3.Connection, issue: str) -> dict[str, int]:
    return {
        "issue": db.execute(
            "SELECT COUNT(*) FROM issue WHERE target_issue=?", (issue,)
        ).fetchone()[0],
        "number": db.execute(
            "SELECT COUNT(*) FROM number_event WHERE target_issue=?", (issue,)
        ).fetchone()[0],
        "action": db.execute(
            "SELECT COUNT(*) FROM position_action WHERE target_issue=?", (issue,)
        ).fetchone()[0],
        "selected": db.execute(
            "SELECT COUNT(*) FROM selected_decision WHERE target_issue=?", (issue,)
        ).fetchone()[0],
        "trade": db.execute(
            "SELECT COUNT(*) FROM trade WHERE target_issue=?", (issue,)
        ).fetchone()[0],
    }


def reconcile(db: sqlite3.Connection, issue: str) -> dict[str, int]:
    value = counts(db, issue)
    require(value["issue"] == 1, "ISSUE_LEDGER_COUNT", str(value))
    require(value["number"] == 100, "NUMBER_LEDGER_COUNT", str(value))
    require(value["action"] == 10, "ACTION_LEDGER_COUNT", str(value))
    require(value["selected"] in (0, 1), "SELECTED_LEDGER_COUNT", str(value))
    require(value["trade"] in (0, 1), "TRADE_LEDGER_COUNT", str(value))
    sums = db.execute(
        "SELECT position,SUM(predicted_p) FROM number_event "
        "WHERE target_issue=? GROUP BY position ORDER BY position",
        (issue,),
    ).fetchall()
    require(len(sums) == 10, "NUMBER_POSITION_COUNT")
    require(all(abs(float(total) - 1.0) <= 1.0e-12 for _, total in sums),
            "NUMBER_PROBABILITY_NOT_NORMALIZED")
    return value


def persist_prediction(
    db: sqlite3.Connection,
    issue: str,
    contract_hash: str,
    action: str = "SHADOW",
    fail_after_numbers: int = -1,
    fail_after_actions: int = -1,
) -> bool:
    existing = db.execute(
        "SELECT contract_hash,settled FROM issue WHERE target_issue=?", (issue,)
    ).fetchone()
    if existing is not None:
        require(int(existing[1]) == 0, "SETTLED_PREDICTION_OVERWRITE")
        require(str(existing[0]) == contract_hash, "PREDICTION_HASH_CONFLICT")
        reconcile(db, issue)
        return True
    db.execute("BEGIN IMMEDIATE")
    try:
        db.execute("INSERT INTO issue VALUES(?,?,0)", (issue, contract_hash))
        inserted = 0
        for position in range(1, 11):
            for number in range(1, 11):
                db.execute(
                    "INSERT INTO number_event VALUES(?,?,?,?,?,?)",
                    (issue, position, number, 0.1, -1, 0),
                )
                inserted += 1
                if inserted == fail_after_numbers:
                    raise RuntimeError("INJECT_NUMBER_INSERT_FAILURE")
        for position in range(1, 11):
            db.execute(
                "INSERT INTO position_action VALUES(?,?,?,?,?,?,?)",
                (issue, position, 1, 2, 3, -1, 0),
            )
            if position == fail_after_actions:
                raise RuntimeError("INJECT_ACTION_INSERT_FAILURE")
        decision_id = "D-" + issue
        db.execute(
            "INSERT INTO selected_decision VALUES(?,?,?,?,?,?,?,0)",
            (issue, decision_id, 1, 1, 2, 3, action),
        )
        db.execute(
            "INSERT INTO authorization VALUES(?,?,?,?)",
            (decision_id, issue, contract_hash, "DecisionAuthorizationEngine"),
        )
        if action == "BET":
            db.execute("INSERT INTO trade VALUES(?,?,?)",
                       (issue, decision_id, "BET"))
        reconcile(db, issue)
        db.execute("COMMIT")
        return False
    except BaseException:
        db.execute("ROLLBACK")
        raise


def settle(db: sqlite3.Connection, issue: str) -> None:
    db.execute("BEGIN IMMEDIATE")
    try:
        for position in range(1, 11):
            actual = position
            db.execute(
                "UPDATE number_event SET actual_label=(number=?),settled=1 "
                "WHERE target_issue=? AND position=?",
                (actual, issue, position),
            )
            db.execute(
                "UPDATE position_action SET actual_hit=(? IN (n1,n2,n3)),settled=1 "
                "WHERE target_issue=? AND position=?",
                (actual, issue, position),
            )
        db.execute(
            "UPDATE selected_decision SET settled=1 WHERE target_issue=?", (issue,)
        )
        db.execute("UPDATE issue SET settled=1 WHERE target_issue=?", (issue,))
        db.execute("COMMIT")
    except BaseException:
        db.execute("ROLLBACK")
        raise


def main() -> int:
    db = build_db()
    issue = "202607240001"
    idempotent = persist_prediction(db, issue, "contract-a")
    require(idempotent is False, "FIRST_WRITE_MARKED_IDEMPOTENT")
    require(persist_prediction(db, issue, "contract-a") is True,
            "SAME_HASH_NOT_IDEMPOTENT")
    conflict_blocked = False
    try:
        persist_prediction(db, issue, "contract-b")
    except AssertionError as error:
        conflict_blocked = "PREDICTION_HASH_CONFLICT" in str(error)
    require(conflict_blocked, "DIFFERENT_HASH_OVERWRITE_ALLOWED")
    settle(db, issue)
    settled = db.execute(
        "SELECT COUNT(*),SUM(actual_label) FROM number_event "
        "WHERE target_issue=? AND settled=1", (issue,)
    ).fetchone()
    require(settled == (100, 10), "NUMBER_SETTLEMENT_NOT_100_WITH_10_LABELS",
            str(settled))
    action_settled = db.execute(
        "SELECT COUNT(*),SUM(actual_hit) FROM position_action "
        "WHERE target_issue=? AND settled=1", (issue,)
    ).fetchone()
    require(action_settled == (10, 3), "ACTION_SETTLEMENT_NOT_TEN", str(action_settled))
    overwrite_blocked = False
    try:
        persist_prediction(db, issue, "contract-a")
    except AssertionError as error:
        overwrite_blocked = "SETTLED_PREDICTION_OVERWRITE" in str(error)
    require(overwrite_blocked, "SETTLED_PREDICTION_OVERWRITE_ALLOWED")
    return emit("v50_ledger", {
        "prediction_counts": counts(db, issue),
        "settled_number_rows": settled[0],
        "positive_number_labels": settled[1],
        "settled_action_rows": action_settled[0],
        "idempotent_same_hash": True,
        "different_hash_blocked": True,
        "settled_overwrite_blocked": True,
    })


if __name__ == "__main__":
    raise SystemExit(main())

