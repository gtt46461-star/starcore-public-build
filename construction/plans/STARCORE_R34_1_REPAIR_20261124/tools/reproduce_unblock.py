"""Recheck the two historical windows and the bounded scheduling/FIFO counterexamples."""
import argparse
import hashlib
import json
import sqlite3
from pathlib import Path

WINDOWS = (
    ("25da5c8aeb21642525ace68ae4ed848f89b80dab3445251ffdb1390f433a283c",
     "c0590d15dc32fc8a3dceea6f57e92d31e56b47c0237f28e04be40a38975ef286"),
    ("95962e0dfbfac61d194c51f7dc13b9b1d0dc5dacd9e44bc03682a69f33490b67",
     "270b7c925a4c20b077b6caffba93febbfdc02703dd11adc350c31c1a36313aeb"),
)
TABLES = {"r27_replay_dataset": 3, "r27_evolution_cursor": 1, "r27_policy": 0, "r27_lineage": 0,
          "r27_replay_score": 0, "r27_generation": 0, "r27_live_shadow": 0, "r30_frozen_confirm": 0}


def require(ok, message):
    if not ok:
        raise ValueError(message)


def report_file(path, sha):
    raw = path.read_bytes()
    require(hashlib.sha256(raw).hexdigest() == sha, "Report member differs: " + path.name)
    return json.loads(raw)


def report(directory, hashes):
    scope = report_file(directory / "DIAGNOSTIC_SCOPE.json", hashes[0])
    runtime = report_file(directory / "R26_REPORT.json", hashes[1])
    tables = {r["name"] if "name" in r else r["table"]: r["rows"] for r in scope["tables"]}
    require(all(tables[k] == v for k,v in TABLES.items()), "Unexpected observed table counts")
    section, = (s for s in runtime["sections"] if s["sectionId"] == "CONTINUOUS_RUNTIME")
    selected = {r["task_id"]: r for r in section["metrics"] if isinstance(r,dict) and "task_id" in r}
    for name,dependency,cursor,steps in (("r27-evolution","r27-replay-dataset",13,4),
                                        ("r27-replay-dataset","WRITER_HOLD_FAULT_BACKLOG",3597847,3)):
        row = selected[name]
        require(row["cursor"] == cursor and row["steps"] == steps and row["status"] == "BLOCKED"
                and dependency in row["blocked_reason"], "Different observed stop: " + name)
    return {"snapshot":scope["snapshotSha256"], "counts":TABLES,
            "steps":{"r27-evolution":4,"r27-replay-dataset":3}}


def controls():
    # This is a model of the exact 122 data-due predicate, with freezeWaiters=0 and backlog>=24.
    last_service = 0
    dataset_windows = 0
    for start in range(6000,60001,6000):
        require(start-last_service >= 5000, "Competitor did not receive its preregistered slot")
        last_service = start  # the other research task made a real successful commit
        dataset_windows += int(start+1-last_service >= 5000)
    require(dataset_windows == 0, "Unexpected dataset service")
    # The FIFO query is the SELECT used by FrozenConfirmationStoreR30.nextPending at the 122 commit.
    select = "SELECT packet_json FROM r30_frozen_confirm WHERE state='PENDING' ORDER BY created_at,task_key LIMIT 1"
    with sqlite3.connect(":memory:") as db:
        db.execute("CREATE TABLE r30_frozen_confirm(task_key TEXT PRIMARY KEY, created_at INTEGER, state TEXT, packet_json TEXT)")
        db.executemany("INSERT INTO r30_frozen_confirm VALUES(?,?,'PENDING',?)",
                       [("old",1,"{"),("later",2,'{"entry":"second"}')])
        blocked = 0
        for _ in range(3):
            try:
                json.loads(db.execute(select).fetchone()[0])
            except json.JSONDecodeError:
                blocked += 1
        require(blocked == 3, "Oldest malformed packet was not selected on each retry")
    return {"model_competitor_commits":10, "model_dataset_admissions":dataset_windows,
            "sqlite_retries_selecting_same_invalid_json":blocked,
            "boundary":"Bounded counterexamples, no Android execution or natural quality evidence."}


if __name__ == "__main__":
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--first", required=True, type=Path)
    ap.add_argument("--second", required=True, type=Path)
    args = ap.parse_args()
    print(json.dumps({"reports":[report(args.first,WINDOWS[0]), report(args.second,WINDOWS[1])],
                      "controls":controls()},ensure_ascii=False,indent=2))
