#!/usr/bin/env python3
"""Deterministic B09 SQLite fault harness.

This is a contract model for the Java wiring guarded by r926_batch2_gate.sh.  It proves the required
storage semantics under duplicate callbacks and crash points: artifact, LEARNED event and transaction
commit marker are all-or-nothing, and one issue cannot silently accept a conflicting model state.
"""
from __future__ import annotations

import hashlib
import json
import sqlite3
import tempfile
import threading
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "reports" / "r926" / "batch2" / "B09_LEARNING_TX_FAULT_HARNESS.json"


def sha(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def learning_identity(issue: str, contract: str, label_version: str, input_hash: str) -> str:
    return sha(f"LEARNING|Binance1-PK10|{issue}|{contract}|{label_version}|{input_hash}")


def schema(db: sqlite3.Connection) -> None:
    db.executescript(
        """
        PRAGMA foreign_keys=ON;
        CREATE TABLE tx_audit(
          idempotency_key TEXT PRIMARY KEY,
          issue TEXT NOT NULL,
          status TEXT NOT NULL CHECK(status='COMMITTED')
        );
        CREATE TABLE artifact(
          issue TEXT PRIMARY KEY,
          artifact_hash TEXT NOT NULL CHECK(length(artifact_hash)=64),
          pre_state_hash TEXT NOT NULL CHECK(length(pre_state_hash)=64),
          trained_until TEXT NOT NULL,
          CHECK(issue=trained_until)
        );
        CREATE TABLE learner_cursor(
          id INTEGER PRIMARY KEY CHECK(id=1),
          trained_until TEXT NOT NULL DEFAULT ''
        );
        INSERT INTO learner_cursor(id,trained_until) VALUES(1,'');
        CREATE TABLE event(
          idempotency_key TEXT PRIMARY KEY,
          issue TEXT NOT NULL,
          event_type TEXT NOT NULL CHECK(event_type='LEARNED'),
          learning_identity TEXT NOT NULL UNIQUE,
          pre_state_hash TEXT NOT NULL CHECK(length(pre_state_hash)=64),
          post_state_hash TEXT NOT NULL CHECK(length(post_state_hash)=64),
          trained_until TEXT NOT NULL,
          CHECK(issue=trained_until)
        );
        """
    )


def counts(db: sqlite3.Connection) -> dict[str, int]:
    return {
        name: db.execute(f"SELECT COUNT(*) FROM {name}").fetchone()[0]
        for name in ("artifact", "event", "tx_audit")
    }


def learn(
    db: sqlite3.Connection,
    issue: str,
    identity: str,
    pre_hash: str,
    post_hash: str,
    fail_at: str = "",
) -> str:
    if any(len(x) != 64 for x in (identity, pre_hash, post_hash)):
        return "REJECT_HASH"
    if db.execute(
        "SELECT 1 FROM tx_audit WHERE idempotency_key=? AND status='COMMITTED'", (identity,)
    ).fetchone():
        return "REPLAY"
    try:
        db.execute("BEGIN IMMEDIATE")
        # Re-check under the SQLite write lock. The outer lookup is only a fast path; without this
        # fence two workers can both observe "not committed" and the loser hits a UNIQUE error.
        if db.execute(
            "SELECT 1 FROM tx_audit WHERE idempotency_key=? AND status='COMMITTED'", (identity,)
        ).fetchone():
            db.rollback()
            return "REPLAY"
        trained_until = db.execute("SELECT trained_until FROM learner_cursor WHERE id=1").fetchone()[0]
        if trained_until and issue < trained_until:
            raise RuntimeError("OUT_OF_ORDER_TRAINING_CURSOR")
        row = db.execute("SELECT artifact_hash FROM artifact WHERE issue=?", (issue,)).fetchone()
        if row is None:
            db.execute(
                "INSERT INTO artifact(issue,artifact_hash,pre_state_hash,trained_until) VALUES(?,?,?,?)",
                (issue, post_hash, pre_hash, issue),
            )
        elif row[0] != post_hash:
            raise RuntimeError("ARTIFACT_CONFLICT")
        if fail_at == "AFTER_ARTIFACT":
            raise RuntimeError("FAULT_AFTER_ARTIFACT")

        event_key = f"LEARNED|{issue}|ResearchLearner|{identity}"
        db.execute(
            "INSERT OR IGNORE INTO event(idempotency_key,issue,event_type,learning_identity,pre_state_hash,post_state_hash,trained_until)"
            " VALUES(?,?,?,?,?,?,?)",
            (event_key, issue, "LEARNED", identity, pre_hash, post_hash, issue),
        )
        learned = db.execute(
            "SELECT pre_state_hash,post_state_hash,trained_until FROM event WHERE learning_identity=?",
            (identity,),
        ).fetchone()
        if learned != (pre_hash, post_hash, issue):
            raise RuntimeError("LEARNED_CONFLICT")
        if fail_at == "AFTER_LEARNED":
            raise RuntimeError("FAULT_AFTER_LEARNED")

        db.execute("UPDATE learner_cursor SET trained_until=? WHERE id=1", (issue,))
        db.execute(
            "INSERT INTO tx_audit(idempotency_key,issue,status) VALUES(?,?, 'COMMITTED')",
            (identity, issue),
        )
        if fail_at == "BEFORE_COMMIT":
            raise RuntimeError("FAULT_BEFORE_COMMIT")
        db.commit()
        return "COMMITTED"
    except Exception as exc:  # deterministic rollback is what this harness validates
        db.rollback()
        return f"ROLLED_BACK:{type(exc).__name__}:{exc}"


def isolated() -> sqlite3.Connection:
    db = sqlite3.connect(":memory:", isolation_level=None)
    schema(db)
    return db


def main() -> None:
    cases = []
    issue = "202609090001"
    pre = sha("pre")
    post = sha("post")
    identity = learning_identity(issue, "FTRL_R811_V2", "STRICT_OOS_V1", sha("input"))

    db = isolated()
    first = learn(db, issue, identity, pre, post)
    replays = [learn(db, issue, identity, pre, post) for _ in range(10)]
    c = counts(db)
    ok = first == "COMMITTED" and all(x == "REPLAY" for x in replays) and c == {
        "artifact": 1,
        "event": 1,
        "tx_audit": 1,
    }
    cases.append({"case": "DUPLICATE_CALLBACK_X10", "pass": ok, "counts": c, "results": [first] + replays})
    db.close()

    for point in ("AFTER_ARTIFACT", "AFTER_LEARNED", "BEFORE_COMMIT"):
        db = isolated()
        result = learn(db, issue, identity, pre, post, point)
        c = counts(db)
        ok = result.startswith("ROLLED_BACK:") and c == {"artifact": 0, "event": 0, "tx_audit": 0}
        cases.append({"case": f"FAULT_{point}", "pass": ok, "counts": c, "result": result})
        db.close()

    db = isolated()
    first = learn(db, issue, identity, pre, post)
    conflict_identity = learning_identity(issue, "FTRL_R811_V2", "STRICT_OOS_V2", sha("input2"))
    conflict = learn(db, issue, conflict_identity, pre, sha("different-post"))
    c = counts(db)
    ok = first == "COMMITTED" and conflict.startswith("ROLLED_BACK:") and c == {
        "artifact": 1,
        "event": 1,
        "tx_audit": 1,
    }
    cases.append({"case": "SAME_ISSUE_CONFLICTING_MODEL", "pass": ok, "counts": c, "result": conflict})
    db.close()

    db = isolated()
    newer_issue = "202609090010"
    newer_identity = learning_identity(newer_issue, "FTRL_R811_V2", "STRICT_OOS_V1", sha("newer-input"))
    newer = learn(db, newer_issue, newer_identity, pre, sha("newer-post"))
    older_issue = "202609090009"
    older_identity = learning_identity(older_issue, "FTRL_R811_V2", "STRICT_OOS_V1", sha("older-input"))
    older = learn(db, older_issue, older_identity, sha("newer-post"), sha("older-post"))
    c = counts(db)
    cases.append({
        "case": "OUT_OF_ORDER_CURSOR_REFUSED",
        "pass": newer == "COMMITTED" and "OUT_OF_ORDER_TRAINING_CURSOR" in older and c == {"artifact": 1, "event": 1, "tx_audit": 1},
        "counts": c,
        "newer": newer,
        "older": older,
    })
    db.close()

    # Two independent SQLite connections race the same idempotency key. BEGIN IMMEDIATE serializes
    # the writers; exactly one commits and the other observes durable replay after the lock.
    with tempfile.TemporaryDirectory(prefix="r926-b09-race-") as td:
        path = Path(td) / "race.db"
        init = sqlite3.connect(path, isolation_level=None, timeout=5.0)
        schema(init)
        init.close()
        results: list[str] = []
        result_lock = threading.Lock()
        barrier = threading.Barrier(2)

        def worker() -> None:
            conn = sqlite3.connect(path, isolation_level=None, timeout=5.0)
            try:
                barrier.wait(timeout=5.0)
                value = learn(conn, issue, identity, pre, post)
                with result_lock:
                    results.append(value)
            finally:
                conn.close()

        threads = [threading.Thread(target=worker), threading.Thread(target=worker)]
        for t in threads:
            t.start()
        for t in threads:
            t.join(10.0)
        check = sqlite3.connect(path, isolation_level=None)
        c = counts(check)
        check.close()
        race_ok = len(results) == 2 and sorted(results) == ["COMMITTED", "REPLAY"] and c == {"artifact": 1, "event": 1, "tx_audit": 1}
        cases.append({"case": "DOUBLE_WORKER_SQLITE_RACE", "pass": race_ok, "counts": c, "results": sorted(results)})

    changed = learning_identity(issue, "FTRL_R811_V2", "STRICT_OOS_V2", sha("input"))
    cases.append({"case": "LABEL_VERSION_IN_IDENTITY", "pass": changed != identity, "v1": identity, "v2": changed})

    passed = all(x["pass"] for x in cases)
    report = {
        "contract": "B09_EXACTLY_ONCE_SQLITE_TRANSACTION_V1",
        "result": "PASS" if passed else "FAIL",
        "cases": cases,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    if not passed:
        print("R926_B09_LEARNING_TX_HARNESS=FAIL")
        raise SystemExit(91)
    print("R926_B09_LEARNING_TX_HARNESS=PASS|cases=%d" % len(cases))


if __name__ == "__main__":
    main()
