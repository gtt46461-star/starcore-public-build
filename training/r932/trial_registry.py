"""Append-only durable experiment-attempt ledger for R932 research governance.

Every invocation is retained, including failures and interrupted/retried attempts.  Trial identity is
content based; paths, filenames and archive state are never part of the key.  SQLite WAL + IMMEDIATE
transactions make concurrent writers serialize without dropping either attempt.
"""
from __future__ import annotations
import hashlib
import json
import os
import sqlite3
import time
import uuid
from pathlib import Path

SCHEMA = "R932_TRIAL_ATTEMPT_REGISTRY_V1"


def _canonical(value) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False)


def _sha(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def _db(root: Path) -> Path:
    return Path(root) / "trial_registry_r932.sqlite3"


def _schema(connection: sqlite3.Connection) -> None:
    connection.execute("PRAGMA journal_mode=WAL")
    connection.execute("PRAGMA synchronous=FULL")
    connection.execute(
        "CREATE TABLE IF NOT EXISTS trial("
        "trial_id TEXT PRIMARY KEY,identity_json TEXT NOT NULL,identity_hash TEXT NOT NULL,"
        "created_at_ns INTEGER NOT NULL)"
    )
    connection.execute(
        "CREATE TABLE IF NOT EXISTS attempt("
        "attempt_id TEXT PRIMARY KEY,trial_id TEXT NOT NULL,status TEXT NOT NULL,"
        "output_hash TEXT NOT NULL,detail TEXT NOT NULL,started_at_ns INTEGER NOT NULL,"
        "writer_pid INTEGER NOT NULL,record_hash TEXT NOT NULL,"
        "FOREIGN KEY(trial_id) REFERENCES trial(trial_id))"
    )
    connection.execute("CREATE INDEX IF NOT EXISTS idx_r932_trial_attempt ON attempt(trial_id,started_at_ns,attempt_id)")
    # History is evidence.  Ordinary SQL cannot rewrite or delete an attempt/trial after commit.
    connection.execute(
        "CREATE TRIGGER IF NOT EXISTS r932_trial_immutable_update BEFORE UPDATE ON trial "
        "BEGIN SELECT RAISE(ABORT,'R932_TRIAL_IMMUTABLE'); END"
    )
    connection.execute(
        "CREATE TRIGGER IF NOT EXISTS r932_trial_immutable_delete BEFORE DELETE ON trial "
        "BEGIN SELECT RAISE(ABORT,'R932_TRIAL_IMMUTABLE'); END"
    )
    connection.execute(
        "CREATE TRIGGER IF NOT EXISTS r932_attempt_immutable_update BEFORE UPDATE ON attempt "
        "BEGIN SELECT RAISE(ABORT,'R932_ATTEMPT_IMMUTABLE'); END"
    )
    connection.execute(
        "CREATE TRIGGER IF NOT EXISTS r932_attempt_immutable_delete BEFORE DELETE ON attempt "
        "BEGIN SELECT RAISE(ABORT,'R932_ATTEMPT_IMMUTABLE'); END"
    )


def record(root: Path, trial_id: str, identity: dict, status: str, output_hash: str = "", detail: str = "") -> str:
    root = Path(root)
    root.mkdir(parents=True, exist_ok=True)
    identity_json = _canonical(identity)
    identity_hash = _sha(identity_json)
    if trial_id != identity_hash:
        raise ValueError("R932_TRIAL_IDENTITY_HASH_MISMATCH")
    if not status:
        raise ValueError("R932_TRIAL_STATUS_REQUIRED")
    if output_hash and (len(output_hash) != 64 or any(c not in "0123456789abcdef" for c in output_hash)):
        raise ValueError("R932_TRIAL_OUTPUT_HASH_INVALID")
    started = time.time_ns()
    # UUID is attempt identity only. Statistical/trial identity stays content-addressed above.
    attempt_id = _sha("|".join((trial_id, str(started), str(os.getpid()), uuid.uuid4().hex)))
    record_hash = _sha("|".join((SCHEMA, attempt_id, trial_id, status, output_hash, detail, str(started), str(os.getpid()))))
    connection = sqlite3.connect(_db(root), timeout=30, isolation_level=None)
    try:
        _schema(connection)
        connection.execute("BEGIN IMMEDIATE")
        prior = connection.execute("SELECT identity_json,identity_hash FROM trial WHERE trial_id=?", (trial_id,)).fetchone()
        if prior is None:
            connection.execute("INSERT INTO trial VALUES(?,?,?,?)", (trial_id, identity_json, identity_hash, started))
        elif prior[0] != identity_json or prior[1] != identity_hash:
            raise ValueError("R932_TRIAL_ID_COLLISION")
        connection.execute(
            "INSERT INTO attempt VALUES(?,?,?,?,?,?,?,?)",
            (attempt_id, trial_id, status, output_hash, detail, started, os.getpid(), record_hash),
        )
        connection.commit()
        return attempt_id
    except BaseException:
        if connection.in_transaction:
            connection.rollback()
        raise
    finally:
        connection.close()


def attempts(root: Path, trial_id: str):
    connection = sqlite3.connect(_db(Path(root)), timeout=30)
    try:
        _schema(connection)
        return connection.execute(
            "SELECT attempt_id,status,output_hash,detail,started_at_ns,writer_pid,record_hash "
            "FROM attempt WHERE trial_id=? ORDER BY started_at_ns,attempt_id",
            (trial_id,),
        ).fetchall()
    finally:
        connection.close()

