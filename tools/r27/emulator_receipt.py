#!/usr/bin/env python3
"""Summarize the database snapshots written by tools/r27/emulator_r27.sh into receipt.json (scope: EMULATOR).

Checks (each reported OK/FAILED with values; rc 0 only when all hold):
  U1  every table present in the baseline snapshot still exists after the R27 upgrade, and the clean draw history and
      the R26 task/progress/evolution rows are not fewer than before (additive migration, nothing dropped);
  U2  r27_migration revision 1 recorded; all r27_* tables exist;
  U3  after rollback to 1713d11 the database still opens, baseline tables are intact and R27 tables are kept;
  U4  after re-upgrade the R27 rows written before the rollback are still there (no evidence deleted).
Observations (never a pass criterion, reported as they are): wait_kind distribution, R27 task errors, research thread
CPU, generations, policies, live shadows, opportunity rows, AtomNet state. On an emulator without the API credential
there may be no new draws; empty R27 research tables are then reported as INSUFFICIENT, not as a failure.
"""
import json, sqlite3, sys
from pathlib import Path

OUT = Path(sys.argv[1])
DB = "binance_k3_nova_one_pro_v57.db"
FAIL = []
R = {"schema": "R27_EMULATOR_RECEIPT_V1", "scope": "EMULATOR_NOT_REAL_DEVICE", "checks": {}, "observations": {}}


def open_snap(name):
    p = OUT / ("db-" + name) / DB
    if not p.is_file() or p.stat().st_size == 0:
        return None
    return sqlite3.connect("file:%s?mode=ro" % p, uri=True)


def tables(db):
    return {r[0] for r in db.execute("SELECT name FROM sqlite_master WHERE type='table'")}


def count(db, t, where=""):
    try:
        return db.execute("SELECT count(*) FROM %s %s" % (t, where)).fetchone()[0]
    except sqlite3.Error as e:
        return "ERROR:" + str(e)


def check(name, ok, value):
    R["checks"][name] = {"ok": bool(ok), "value": value}
    print("R27_EMULATOR|%s|%s|%s" % (name, "OK" if ok else "FAILED", json.dumps(value, ensure_ascii=False)[:600]))
    if not ok:
        FAIL.append(name)


base, r27, rb, re_ = (open_snap(n) for n in ("baseline", "r27", "rollback", "reupgrade"))
check("SNAPSHOTS_PRESENT", all(x is not None for x in (base, r27, rb, re_)), {n: open_snap(n) is not None for n in ("baseline", "r27", "rollback", "reupgrade")})
if base and r27:
    tb, tr = tables(base), tables(r27)
    check("U1_NO_TABLE_DROPPED", tb <= tr, {"baselineTables": len(tb), "afterUpgrade": len(tr), "missing": sorted(tb - tr)})
    keyed = {"draws": "WHERE dirty=0", "r26_task": "", "r26_progress": "", "r26_evolution": ""}
    rows = {t: (count(base, t, w), count(r27, t, w)) for t, w in keyed.items() if t in tb}
    check("U1_ROWS_NOT_FEWER", all(isinstance(a, int) and isinstance(b, int) and b >= a for a, b in rows.values()), rows)
    mig = count(r27, "r27_migration", "WHERE revision=1")
    r27_tables = sorted(t for t in tr if t.startswith("r27_"))
    check("U2_MIGRATION_RECORDED", mig == 1 and len(r27_tables) >= 30, {"r27_migration": mig, "r27Tables": len(r27_tables)})
if rb and r27:
    tb2 = tables(rb)
    check("U3_ROLLBACK_KEEPS_TABLES", tables(base or rb) <= tb2 and all(t in tb2 for t in tables(r27) if t.startswith("r27_")),
          {"tables": len(tb2), "integrity": rb.execute("PRAGMA integrity_check").fetchone()[0]})
if r27 and re_:
    keep = {}
    for t in ("r27_policy", "r27_generation", "r27_lineage", "r27_live_shadow", "r27_eprocess", "r27_atomnet_epoch", "r27_task_error"):
        keep[t] = (count(r27, t), count(re_, t))
    check("U4_R27_EVIDENCE_KEPT_ACROSS_ROLLBACK", all(isinstance(a, int) and isinstance(b, int) and b >= a for a, b in keep.values()), keep)

snap = re_ or r27
if snap:
    obs = R["observations"]
    def q(sql):
        try:
            return [list(r) for r in snap.execute(sql).fetchall()]
        except sqlite3.Error as e:
            return "ERROR:" + str(e)
    obs["waitKinds"] = q("SELECT wait_kind,count(*) FROM r26_task GROUP BY wait_kind ORDER BY 2 DESC")
    obs["blockedReasons"] = q("SELECT task_id,wait_kind,substr(wait_detail,1,120),cursor,steps FROM r26_task ORDER BY task_id")
    obs["criticalDeadlineSessions"] = q("SELECT state,count(*) FROM r26_session GROUP BY state")
    obs["taskErrors"] = q("SELECT task_id,error_class,substr(message,1,160),count(*) FROM r27_task_error GROUP BY task_id,error_class ORDER BY 4 DESC LIMIT 20")
    obs["threadCpu"] = q("SELECT thread_name,cpu_ms,wall_ms,slices FROM r27_thread_cpu ORDER BY thread_name")
    obs["writeWait"] = q("SELECT * FROM r27_write_wait ORDER BY hour_start DESC LIMIT 4")
    obs["generations"] = q("SELECT loop,count(*),max(generation),sum(births),sum(verified) FROM r27_generation GROUP BY loop")
    obs["policiesByState"] = q("SELECT state,count(*) FROM r27_policy GROUP BY state")
    obs["liveShadows"] = q("SELECT status,count(*) FROM r27_live_shadow GROUP BY status")
    obs["opportunity"] = q("SELECT count(*),sum(scheduled),sum(frozen),sum(deadline_missed) FROM r27_opportunity")
    obs["atomnet"] = q("SELECT owner,online_steps,learning_rate,trained_through FROM r27_atomnet_state")
    obs["atomnetEpochs"] = q("SELECT count(*) FROM r27_atomnet_epoch")
    obs["replayDataset"] = q("SELECT dataset_hash,rows,segments,build_ms FROM r27_replay_dataset ORDER BY built_at DESC LIMIT 2")
    obs["drawsClean"] = q("SELECT count(*),min(issue),max(issue) FROM draws WHERE dirty=0")
    obs["abiContinuity"] = q("SELECT result,count(*) FROM r27_abi_continuity GROUP BY result")
    obs["chartCache"] = q("SELECT panel_id,key,length(payload_json) FROM r27_chart_cache ORDER BY panel_id LIMIT 40")
    empty = [k for k in ("generations", "policiesByState") if obs.get(k) in ([], "ERROR")]
    obs["note"] = "INSUFFICIENT: " + ",".join(empty) + " empty on this emulator run" if empty else "R27 research tables populated"
    for k, v in obs.items():
        print("R27_EMULATOR_OBS|%s|%s" % (k, json.dumps(v, ensure_ascii=False)[:1200]))
R["result"] = "ALL_OK" if not FAIL else "FAILED:" + ",".join(FAIL)
(OUT / "receipt.json").write_text(json.dumps(R, ensure_ascii=False, indent=1))
print("R27_EMULATOR|RESULT|" + R["result"])
sys.exit(1 if FAIL else 0)
