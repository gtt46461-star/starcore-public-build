#!/usr/bin/env python3
"""SQLite behavioral model for R919 per-draw, per-prediction and post-settlement truth."""

from __future__ import annotations

import hashlib
import sqlite3


def require(condition: bool, code: str) -> None:
    if not condition:
        raise AssertionError(f"R919_SQLITE_FAIL:{code}")


def digest(*parts: object) -> str:
    return hashlib.sha256("|".join(map(str, parts)).encode("utf-8")).hexdigest()


def issue(index: int) -> str:
    return f"20260831{index:04d}"


def schema(db: sqlite3.Connection) -> None:
    db.executescript(
        """
        PRAGMA foreign_keys=ON;
        CREATE TABLE draws(
          issue TEXT PRIMARY KEY,
          n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,n4 INTEGER NOT NULL,
          dirty INTEGER NOT NULL DEFAULT 0,
          draw_hash TEXT NOT NULL UNIQUE
        );
        CREATE TABLE predraw(
          target_issue TEXT PRIMARY KEY REFERENCES draws(issue),
          position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 4),
          k1 INTEGER NOT NULL,k2 INTEGER NOT NULL,k3 INTEGER NOT NULL,
          frozen_at INTEGER NOT NULL,
          trained_until TEXT NOT NULL,
          record_hash TEXT NOT NULL UNIQUE,
          CHECK(trained_until < target_issue)
        );
        CREATE TABLE settlement_task(
          target_issue TEXT PRIMARY KEY REFERENCES draws(issue),
          status TEXT NOT NULL,
          terminal_code TEXT NOT NULL DEFAULT '',
          attempts INTEGER NOT NULL DEFAULT 0,
          settlement_hash TEXT NOT NULL DEFAULT '',
          completed_at INTEGER NOT NULL DEFAULT 0
        );
        CREATE TABLE oos_label(
          target_issue TEXT PRIMARY KEY REFERENCES predraw(target_issue),
          actual INTEGER NOT NULL,
          hit INTEGER NOT NULL CHECK(hit IN(0,1)),
          predraw_hash TEXT NOT NULL,
          draw_hash TEXT NOT NULL,
          record_hash TEXT NOT NULL UNIQUE
        );
        CREATE TABLE legacy_predraw_context(
          target_issue TEXT PRIMARY KEY REFERENCES predraw(target_issue),
          teacher TEXT NOT NULL,
          reasoning TEXT NOT NULL,
          reverse_reasoning TEXT NOT NULL,
          causal TEXT NOT NULL,
          feedback_contract TEXT NOT NULL,
          record_hash TEXT NOT NULL UNIQUE
        );
        CREATE TABLE legacy_postsettlement_context(
          target_issue TEXT PRIMARY KEY REFERENCES predraw(target_issue),
          actual INTEGER NOT NULL,
          hit INTEGER NOT NULL CHECK(hit IN(0,1)),
          attribution TEXT NOT NULL,
          feedback TEXT NOT NULL,
          predraw_hash TEXT NOT NULL,
          draw_hash TEXT NOT NULL,
          record_hash TEXT NOT NULL UNIQUE
        );
        CREATE TABLE algorithm_predraw(
          algorithm_id TEXT NOT NULL,
          target_issue TEXT NOT NULL REFERENCES predraw(target_issue),
          state_before_hash TEXT NOT NULL,
          score REAL NOT NULL,
          hit INTEGER,
          state_after_hash TEXT,
          PRIMARY KEY(algorithm_id,target_issue),
          CHECK(hit IS NULL OR hit IN(0,1))
        );
        CREATE TABLE asset_predraw(
          asset_id TEXT NOT NULL,
          target_issue TEXT NOT NULL REFERENCES predraw(target_issue),
          value_hash TEXT NOT NULL,
          hit INTEGER,
          PRIMARY KEY(asset_id,target_issue),
          CHECK(hit IS NULL OR hit IN(0,1))
        );
        CREATE TABLE lifecycle(
          candidate_id TEXT PRIMARY KEY,
          generation INTEGER NOT NULL,
          future_n INTEGER NOT NULL DEFAULT 0,
          future_hit INTEGER NOT NULL DEFAULT 0,
          status TEXT NOT NULL,
          direct_real_auth INTEGER NOT NULL DEFAULT 0 CHECK(direct_real_auth=0)
        );
        """
    )


def seed_draws_and_tasks(db: sqlite3.Connection, count: int) -> None:
    with db:
        for i in range(1, count + 1):
            nums = tuple((i * factor + offset) % 10 + 1 for factor, offset in (
                (3, 1), (5, 2), (7, 3), (9, 4)))
            draw_hash = digest("DRAW", issue(i), *nums)
            db.execute(
                "INSERT INTO draws VALUES(?,?,?,?,?,?,?)",
                (issue(i), *nums, 0, draw_hash),
            )
            db.execute(
                "INSERT INTO settlement_task(target_issue,status) VALUES(?,?)",
                (issue(i), "PENDING"),
            )


def freeze_predictions(db: sqlite3.Connection, predicted: int) -> None:
    with db:
        for i in range(1, predicted + 1):
            target = issue(i)
            trained = issue(0) if i == 1 else issue(i - 1)
            position = i % 4 + 1
            k3 = ((i * 2) % 10 + 1, (i * 3 + 1) % 10 + 1, (i * 5 + 2) % 10 + 1)
            predraw_hash = digest("PREDRAW", target, position, *k3, trained)
            db.execute(
                "INSERT INTO predraw VALUES(?,?,?,?,?,?,?,?)",
                (target, position, *k3, 1_787_000_000_000 + i, trained, predraw_hash),
            )
            teacher_hash = digest("TEACHER", target, predraw_hash)
            db.execute(
                "INSERT INTO legacy_predraw_context VALUES(?,?,?,?,?,?,?)",
                (
                    target,
                    "教师后验与反证均来自开奖前冻结特征",
                    "正向推理矩阵=号码面→置换→固定K3",
                    "反推矩阵=第三边界+熵风险+失败路径",
                    "因果矩阵=机制支持/反证/消融候选",
                    "开奖结果后只允许影响t+1及以后",
                    teacher_hash,
                ),
            )
            for algorithm in ("玄策-记忆分流算法", "天工-迁移耦合算法"):
                state_hash = digest("STATE_BEFORE", algorithm, target)
                score = ((i * 17 + len(algorithm)) % 1000) / 1000.0
                db.execute(
                    "INSERT INTO algorithm_predraw VALUES(?,?,?,?,NULL,NULL)",
                    (algorithm, target, state_hash, score),
                )
            for asset in ("星衍-关系矩阵资产", "知微-多尺度残差资产"):
                db.execute(
                    "INSERT INTO asset_predraw VALUES(?,?,?,NULL)",
                    (asset, target, digest("ASSET", asset, target)),
                )


def exact_number(db: sqlite3.Connection, target: str, position: int) -> int:
    row = db.execute(
        "SELECT n1,n2,n3,n4 FROM draws WHERE issue=? AND dirty=0", (target,)
    ).fetchone()
    require(row is not None, f"DRAW_MISSING:{target}")
    return int(row[position - 1])


def settle_prediction(db: sqlite3.Connection, target: str) -> bool:
    predraw = db.execute(
        "SELECT position,k1,k2,k3,record_hash FROM predraw WHERE target_issue=?", (target,)
    ).fetchone()
    if predraw is None:
        return False
    position, k1, k2, k3, predraw_hash = predraw
    actual = exact_number(db, target, position)
    hit = int(actual in (k1, k2, k3))
    draw_hash = db.execute("SELECT draw_hash FROM draws WHERE issue=?", (target,)).fetchone()[0]
    label_hash = digest("OOS", target, predraw_hash, draw_hash, actual, hit)
    settlement_hash = digest("SETTLEMENT", target, label_hash)
    with db:
        cursor = db.execute(
            "INSERT OR IGNORE INTO oos_label VALUES(?,?,?,?,?,?)",
            (target, actual, hit, predraw_hash, draw_hash, label_hash),
        )
        if cursor.rowcount == 0:
            existing = db.execute(
                "SELECT record_hash FROM oos_label WHERE target_issue=?", (target,)
            ).fetchone()[0]
            require(existing == label_hash, f"OOS_FIRST_WRITE_CONFLICT:{target}")
            return False
        db.execute(
            "UPDATE settlement_task SET status='COMPLETED',attempts=attempts+1,"
            "settlement_hash=?,completed_at=? WHERE target_issue=? AND status='PENDING'",
            (settlement_hash, 1_787_100_000_000, target),
        )
    return True


def teacher_and_research_sidecars(db: sqlite3.Connection, target: str) -> bool:
    label = db.execute(
        "SELECT actual,hit,predraw_hash,draw_hash FROM oos_label WHERE target_issue=?", (target,)
    ).fetchone()
    require(label is not None, f"CORE_NOT_COMMITTED:{target}")
    actual, hit, predraw_hash, draw_hash = label
    teacher = db.execute(
        "SELECT teacher,reasoning,reverse_reasoning,causal FROM legacy_predraw_context "
        "WHERE target_issue=?",
        (target,),
    ).fetchone()
    require(teacher is not None, f"PREDRAW_TEACHER_MISSING:{target}")
    attribution = (
        f"{'结构命中' if hit else '结构未中'}|actual={actual}|"
        f"teacher={teacher[0]}|reasoning={teacher[1]}|reverse={teacher[2]}|causal={teacher[3]}"
    )
    feedback = "结算后反哺只更新t+1以后状态；禁止改写本期赛前证据"
    post_hash = digest(
        "R919_LEGACY_POST", target, predraw_hash, draw_hash, actual, hit, attribution, feedback
    )
    with db:
        cursor = db.execute(
            "INSERT OR IGNORE INTO legacy_postsettlement_context VALUES(?,?,?,?,?,?,?,?)",
            (target, actual, hit, attribution, feedback, predraw_hash, draw_hash, post_hash),
        )
        if cursor.rowcount == 0:
            existing = db.execute(
                "SELECT record_hash FROM legacy_postsettlement_context WHERE target_issue=?",
                (target,),
            ).fetchone()[0]
            require(existing == post_hash, f"TEACHER_FIRST_WRITE_CONFLICT:{target}")
            return False
        db.execute(
            "UPDATE algorithm_predraw SET hit=?,state_after_hash=? "
            "WHERE target_issue=? AND hit IS NULL",
            (hit, digest("ALGORITHM_STATE_AFTER", target, hit), target),
        )
        db.execute(
            "UPDATE asset_predraw SET hit=? WHERE target_issue=? AND hit IS NULL",
            (hit, target),
        )
    return True


def classify_no_predraw_pressure_slices(db: sqlite3.Connection) -> int:
    pulses = 0
    while True:
        open_count = db.execute(
            "SELECT COUNT(*) FROM settlement_task WHERE status='PENDING'"
        ).fetchone()[0]
        if open_count == 0:
            break
        with db:
            db.execute(
                "UPDATE settlement_task SET status='NO_PREDRAW_TERMINAL',"
                "terminal_code='R919_AUDITED_NO_IMMUTABLE_PREDRAW',completed_at=? "
                "WHERE target_issue IN(SELECT t.target_issue FROM settlement_task t "
                "LEFT JOIN predraw p ON p.target_issue=t.target_issue "
                "WHERE t.status='PENDING' AND p.target_issue IS NULL "
                "ORDER BY t.target_issue LIMIT 4)",
                (1_787_200_000_000 + pulses,),
            )
        pulses += 1
        require(pulses <= 64, "PRESSURE_CONVERGENCE_TIMEOUT")
    return pulses


def lifecycle_contract(db: sqlite3.Connection) -> None:
    with db:
        db.execute(
            "INSERT INTO lifecycle VALUES('parent-memory-router',7,240,86,'CHALLENGER_FUTURE_OOS',0)"
        )
        db.execute(
            "INSERT INTO lifecycle VALUES('child-regime-memory-router',8,0,0,'IDEA_WAIT_FUTURE_OOS',0)"
        )
    child = db.execute(
        "SELECT future_n,future_hit,status,direct_real_auth FROM lifecycle WHERE candidate_id LIKE 'child%'"
    ).fetchone()
    require(child == (0, 0, "IDEA_WAIT_FUTURE_OOS", 0), "CHILD_EVIDENCE_NOT_RESET")
    try:
        with db:
            db.execute(
                "INSERT INTO lifecycle VALUES('illegal-direct-real',1,0,0,'IDEA',1)"
            )
    except sqlite3.IntegrityError:
        pass
    else:
        raise AssertionError("R919_SQLITE_FAIL:RESEARCH_DIRECT_REAL_ACCEPTED")


def main() -> None:
    db = sqlite3.connect(":memory:")
    schema(db)
    seed_draws_and_tasks(db, 277)
    freeze_predictions(db, 200)

    settled = 0
    teacher = 0
    for i in range(1, 201):
        target = issue(i)
        settled += int(settle_prediction(db, target))
        teacher += int(teacher_and_research_sidecars(db, target))
        require(not settle_prediction(db, target), f"DUPLICATE_CORE_ACCEPTED:{target}")
        require(not teacher_and_research_sidecars(db, target),
                f"DUPLICATE_TEACHER_ACCEPTED:{target}")

    # A derivative failure cannot undo the already committed core label/task.
    victim = issue(200)
    before = db.execute(
        "SELECT status,settlement_hash FROM settlement_task WHERE target_issue=?", (victim,)
    ).fetchone()
    try:
        with db:
            db.execute(
                "UPDATE legacy_postsettlement_context SET record_hash='tampered' WHERE target_issue=?",
                (victim,),
            )
            raise RuntimeError("INJECTED_SIDECAR_FAILURE")
    except RuntimeError:
        pass
    after = db.execute(
        "SELECT status,settlement_hash FROM settlement_task WHERE target_issue=?", (victim,)
    ).fetchone()
    require(before == after and before[0] == "COMPLETED", "CORE_ROLLED_BACK_BY_SIDECAR")

    pulses = classify_no_predraw_pressure_slices(db)
    lifecycle_contract(db)

    task_total, task_terminal, task_open = db.execute(
        "SELECT COUNT(*),SUM(status IN('COMPLETED','NO_PREDRAW_TERMINAL')),"
        "SUM(status='PENDING') FROM settlement_task"
    ).fetchone()
    eligible = db.execute("SELECT COUNT(*) FROM predraw").fetchone()[0]
    oos = db.execute("SELECT COUNT(*) FROM oos_label").fetchone()[0]
    post = db.execute("SELECT COUNT(*) FROM legacy_postsettlement_context").fetchone()[0]
    algorithm_rows, algorithm_settled = db.execute(
        "SELECT COUNT(*),SUM(hit IN(0,1)) FROM algorithm_predraw"
    ).fetchone()
    asset_rows, asset_settled = db.execute(
        "SELECT COUNT(*),SUM(hit IN(0,1)) FROM asset_predraw"
    ).fetchone()
    no_predraw = db.execute(
        "SELECT COUNT(*) FROM settlement_task WHERE status='NO_PREDRAW_TERMINAL'"
    ).fetchone()[0]

    require((settled, teacher) == (200, 200), "PER_PREDICTION_SETTLEMENT_COUNTS")
    require((task_total, task_terminal, task_open) == (277, 277, 0), "DRAW_TASK_CLOSURE")
    require((eligible, oos, post, no_predraw) == (200, 200, 200, 77), "TWO_DENOMINATORS")
    require((algorithm_rows, algorithm_settled) == (400, 400), "ALGORITHM_EVERY_DRAW")
    require((asset_rows, asset_settled) == (400, 400), "ASSET_EVERY_DRAW")
    require(pulses == 20, "PRESSURE_SLICE_PULSES")

    print("R919_DRAW_TASK_CLOSURE=PASS|TERMINAL=277|TOTAL=277|OPEN=0")
    print("R919_PREDICTION_SETTLEMENT=PASS|SETTLED=200|ELIGIBLE=200|DUPLICATES=0")
    print("R919_TEACHER_POSTSETTLEMENT=PASS|PREDRAW=200|POSTDRAW=200|EXACTLY_ONCE=1|T_PLUS_1=1")
    print("R919_RESEARCH_PER_DRAW=PASS|ALGORITHM=400/400|ASSET=400/400|CHILD_FUTURE_N=0")
    print("R919_NO_PREDRAW_CONVERGENCE=PASS|EXCLUDED=77|PRESSURE_SLICE=4|PULSES=20")
    print("R919_SIDECAR_ISOLATION=PASS|CORE_SURVIVES_DERIVATIVE_FAILURE=1")
    print("R919_SQLITE_TRUTH_CLOSURE=PASS")


if __name__ == "__main__":
    main()
