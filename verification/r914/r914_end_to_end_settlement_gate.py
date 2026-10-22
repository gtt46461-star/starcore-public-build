#!/usr/bin/env python3
"""Deterministic SQLite proof of R914 per-draw task closure and prediction truth separation."""

import hashlib
import sqlite3


TERMINAL = {
    "COMPLETED",
    "NO_PREDRAW_CANONICAL_TERMINAL",
    "FAILED_TERMINAL",
    "INVALID_DRAW_TERMINAL",
}


def digest(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


db = sqlite3.connect(":memory:")
db.executescript(
    """
    CREATE TABLE draws(issue TEXT PRIMARY KEY, actual INTEGER NOT NULL, dirty INTEGER NOT NULL DEFAULT 0);
    CREATE TABLE shadow(
      target_issue TEXT PRIMARY KEY,
      position INTEGER NOT NULL,
      n1 INTEGER NOT NULL,
      n2 INTEGER NOT NULL,
      n3 INTEGER NOT NULL,
      frozen INTEGER NOT NULL,
      settled INTEGER NOT NULL DEFAULT 0,
      hit_label INTEGER NOT NULL DEFAULT -1
    );
    CREATE TABLE task(
      target_issue TEXT PRIMARY KEY,
      status TEXT NOT NULL,
      attempts INTEGER NOT NULL DEFAULT 0,
      next_attempt INTEGER NOT NULL DEFAULT 0,
      lease_owner TEXT NOT NULL DEFAULT '',
      lease_until INTEGER NOT NULL DEFAULT 0,
      last_error TEXT NOT NULL DEFAULT ''
    );
    CREATE TABLE oos(
      target_issue TEXT PRIMARY KEY,
      hit_label INTEGER NOT NULL,
      record_hash TEXT NOT NULL UNIQUE
    );
    """
)

# Draw and task persistence are one atomic handoff. The first 100 issues have immutable predraw
# canonical evidence; the last ten are legitimate draw tasks without a predraw prediction.
for issue in range(1, 111):
    actual = (issue * 7) % 10 + 1
    with db:
        db.execute("INSERT INTO draws(issue,actual) VALUES(?,?)", (f"{issue:04d}", actual))
        db.execute("INSERT INTO task(target_issue,status) VALUES(?, 'PENDING')", (f"{issue:04d}",))
        if issue <= 100:
            base = (issue * 3) % 10 + 1
            nums = sorted({base, base % 10 + 1, (base + 4) % 10 + 1})
            while len(nums) < 3:
                nums.append(len(nums) + 1)
            db.execute(
                "INSERT INTO shadow(target_issue,position,n1,n2,n3,frozen) VALUES(?,1,?,?,?,1)",
                (f"{issue:04d}", nums[0], nums[1], nums[2]),
            )

open_tasks = db.execute("SELECT COUNT(*) FROM task WHERE status NOT IN (?,?,?,?)", tuple(TERMINAL)).fetchone()[0]
frozen = db.execute("SELECT COUNT(*) FROM shadow WHERE frozen=1").fetchone()[0]
assert frozen == 100 and open_tasks == 110
print(f"R914_NEXT_PREDRAW_NOT_BLOCKED_BY_SETTLEMENT_BACKLOG=PASS|FROZEN={frozen}|OPEN_TASKS={open_tasks}")

# Simulate three restart-corrupted task shapes and one legitimate backoff. Recovery is targeted,
# lease-fenced and never turns a missing predraw into a fabricated prediction label.
db.execute("UPDATE task SET status='RUNNING',lease_owner='dead',lease_until=10 WHERE target_issue='0010'")
db.execute("UPDATE task SET status='NO_PREDRAW_CANONICAL_TERMINAL' WHERE target_issue='0020'")
db.execute("UPDATE task SET status='FAILED_TERMINAL',last_error='SQLITE_BUSY' WHERE target_issue='0030'")
db.execute("UPDATE task SET status='RETRY_WAIT',next_attempt=999999 WHERE target_issue='0040'")
now = 1000
with db:
    orphan = db.execute(
        "UPDATE task SET status='RETRY_WAIT',lease_owner='',lease_until=0,next_attempt=0 "
        "WHERE status='RUNNING' AND lease_until<?",
        (now,),
    ).rowcount
    false_no_predraw = db.execute(
        "UPDATE task SET status='RETRY_WAIT',next_attempt=0 "
        "WHERE status='NO_PREDRAW_CANONICAL_TERMINAL' "
        "AND EXISTS(SELECT 1 FROM shadow s WHERE s.target_issue=task.target_issue AND s.frozen=1)"
    ).rowcount
    retryable = db.execute(
        "UPDATE task SET status='RETRY_WAIT',next_attempt=0 "
        "WHERE status='FAILED_TERMINAL' AND last_error='SQLITE_BUSY'"
    ).rowcount
backoff = db.execute(
    "SELECT COUNT(*) FROM task WHERE target_issue='0040' AND status='RETRY_WAIT' AND next_attempt>?",
    (now,),
).fetchone()[0]
assert (orphan, false_no_predraw, retryable, backoff) == (1, 1, 1, 1)
print("R914_RESTART_RECOVERY=PASS|ORPHAN=1|FALSE_NO_PREDRAW=1|RETRYABLE=1|BACKOFF=1")
db.execute("UPDATE task SET next_attempt=0 WHERE target_issue='0040'")

transient_failed = False
pulses = 0
while True:
    row = db.execute(
        "SELECT target_issue FROM task WHERE status IN ('PENDING','RETRY_WAIT') AND next_attempt<=? "
        "ORDER BY target_issue LIMIT 1",
        (now,),
    ).fetchone()
    if row is None:
        break
    issue = row[0]
    pulses += 1
    with db:
        claimed = db.execute(
            "UPDATE task SET status='RUNNING',attempts=attempts+1,lease_owner='gate',lease_until=? "
            "WHERE target_issue=? AND status IN ('PENDING','RETRY_WAIT')",
            (now + 30, issue),
        ).rowcount
        assert claimed == 1
        # One transient failure proves retry without duplicate label insertion.
        if issue == "0055" and not transient_failed:
            transient_failed = True
            db.execute(
                "UPDATE task SET status='RETRY_WAIT',lease_owner='',lease_until=0,next_attempt=0,last_error='SQLITE_BUSY' "
                "WHERE target_issue=?",
                (issue,),
            )
            continue
        predraw = db.execute(
            "SELECT n1,n2,n3 FROM shadow WHERE target_issue=? AND frozen=1", (issue,)
        ).fetchone()
        if predraw is None:
            db.execute(
                "UPDATE task SET status='NO_PREDRAW_CANONICAL_TERMINAL',lease_owner='',lease_until=0 "
                "WHERE target_issue=?",
                (issue,),
            )
            continue
        actual = db.execute("SELECT actual FROM draws WHERE issue=? AND dirty=0", (issue,)).fetchone()[0]
        hit = int(actual in predraw)
        record_hash = digest(f"R914_OOS|{issue}|{actual}|{hit}")
        db.execute(
            "INSERT OR IGNORE INTO oos(target_issue,hit_label,record_hash) VALUES(?,?,?)",
            (issue, hit, record_hash),
        )
        # Replays must resolve to the same immutable row.
        stored = db.execute("SELECT hit_label,record_hash FROM oos WHERE target_issue=?", (issue,)).fetchone()
        assert stored == (hit, record_hash)
        db.execute("UPDATE shadow SET settled=1,hit_label=? WHERE target_issue=?", (hit, issue))
        db.execute(
            "UPDATE task SET status='COMPLETED',lease_owner='',lease_until=0 WHERE target_issue=?",
            (issue,),
        )

total = db.execute("SELECT COUNT(*) FROM task").fetchone()[0]
terminal = db.execute("SELECT COUNT(*) FROM task WHERE status IN (?,?,?,?)", tuple(TERMINAL)).fetchone()[0]
settled = db.execute("SELECT COUNT(*) FROM shadow WHERE settled=1 AND hit_label IN (0,1)").fetchone()[0]
eligible = db.execute("SELECT COUNT(*) FROM shadow WHERE frozen=1").fetchone()[0]
excluded = db.execute("SELECT COUNT(*) FROM task WHERE status='NO_PREDRAW_CANONICAL_TERMINAL'").fetchone()[0]
fabricated = db.execute("SELECT COUNT(*) FROM oos WHERE target_issue>'0100'").fetchone()[0]
duplicates = db.execute(
    "SELECT COUNT(*) FROM (SELECT target_issue,COUNT(*) n FROM oos GROUP BY target_issue HAVING n<>1)"
).fetchone()[0]
assert total == terminal == 110
assert settled == eligible == 100
assert excluded == 10 and fabricated == 0 and duplicates == 0 and pulses == 111

print(f"R914_DRAW_TASK_CLOSURE=PASS|TERMINAL={terminal}|TOTAL={total}|PULSES={pulses}")
print(f"R914_PREDRAW_SETTLEMENT=PASS|SETTLED={settled}|ELIGIBLE={eligible}")
print(f"R914_NO_PREDRAW_TRUTH=PASS|EXCLUDED={excluded}|FABRICATED_LABELS={fabricated}")
print(f"R914_EXACTLY_ONCE_LABEL=PASS|DUPLICATES={duplicates}|TRANSIENT_RETRY={int(transient_failed)}")
print("R914_END_TO_END_SETTLEMENT_GATE=PASS")
