#!/usr/bin/env python3
"""R30 SQL 合同测试（Python 标准库 sqlite3）。

输入：tools/host/SqlContractDumpR30 从 Java 类导出的 SQL JSON（保证执行的是施工代码里的原字符串）。
用法：python3 tools/sql_contract_tests.py sql_contracts.json [--out result.json]
覆盖：
  T01 时间线：先写者赢、跨阶段独立、未知阶段为 NULL
  T02 写锁账本落库：DDL/INSERT/PRUNE/报告查询可执行，NULL 分位数保持 NULL
  T03 冻结触发器：state=0 可写；state>=1 INSERT/UPDATE/DELETE 均 ABORT 且消息含表名；state 回 0 恢复
  T04 R29 回收触发器筛选：LIKE 'r29\\_%' ESCAPE 正确，保留 CP06 自带 r29_legacy_model_no_*，删除 guard/track 类
  T05 R29 guard 触发器在 Root 上会让 CP06 写入失败（证明必须先回收）
  T06 整表复制语义：rowid 分页 INSERT OR REPLACE 幂等、WITHOUT ROWID 按主键分页、内容哈希一致
"""
import hashlib, json, sqlite3, sys

def run(contracts):
    results = []
    def check(name, ok, detail=""):
        results.append({"test": name, "pass": bool(ok), "detail": detail})

    # T01
    db = sqlite3.connect(":memory:")
    db.execute(contracts["timeline_ddl"]); db.execute(contracts["timeline_index"])
    db.execute(contracts["timeline_ensure"], ("I1", "boot-a", 1))
    db.execute(contracts["timeline_ensure"], ("I1", "boot-b", 2))  # 不覆盖
    m = contracts["timeline_mark"]
    db.execute(m["FIRST_OBSERVED"], (1000, 10, "boot-a", 5, "I1"))
    db.execute(m["FIRST_OBSERVED"], (9999, 99, "boot-a", 6, "I1"))  # 第二次无效
    db.execute(m["NEXT_DEADLINE"], (60000, 7, "I1"))
    row = db.execute("SELECT boot_id,wall_first_observed,mono_first_observed,wall_settled,wall_next_deadline FROM r30_issue_timeline WHERE issue='I1'").fetchone()
    check("T01_first_writer_wins", row == ("boot-a", 1000, 10, None, 60000), str(row))
    check("T01_all_stage_sql_valid", all(isinstance(v, str) and v.startswith("UPDATE") for v in m.values()), str(len(m)))

    # T02
    db = sqlite3.connect(":memory:")
    db.execute(contracts["hold_ddl"])
    ins = contracts["hold_insert"]
    db.execute(ins, (1, 100, "R27_LIVE_SETTLE", 0, 10, 5, 500, 500, 350, None, 2, 3, 2, 0))
    db.execute(ins, (1, 100, "R914_DRAW_SAVE_ATOMIC", 1, 10, 2, 5, 5, 5, 50, 80, 0, 0, 0))
    db.execute(ins, (1, 100, "R27_LIVE_SETTLE", 0, 11, 5, 500, 500, 351, None, 2, 3, 2, 0))  # REPLACE 同窗口
    top = db.execute(contracts["hold_report_top"], (0,)).fetchall()
    crit = db.execute(contracts["hold_report_critical"], (0,)).fetchall()
    check("T02_report_top", top == [("R27_LIVE_SETTLE", 11, 351, 500, 3, 2)], str(top))
    check("T02_report_critical", crit == [("R914_DRAW_SAVE_ATOMIC", 10, 50, 80, 5)], str(crit))
    check("T02_null_percentile_kept", db.execute("SELECT wait_p95 FROM r30_writer_hold WHERE critical=0").fetchone()[0] is None)
    db.execute(contracts["hold_prune"], (101,))
    check("T02_prune", db.execute("SELECT count(*) FROM r30_writer_hold").fetchone()[0] == 0)

    # T03
    db = sqlite3.connect(":memory:")
    db.execute("CREATE TABLE r27_demo(id INTEGER PRIMARY KEY, v TEXT)")
    db.execute(contracts["freeze_ddl"]); db.execute(contracts["route_ddl"])
    for k in ("freeze_trigger_insert", "freeze_trigger_update", "freeze_trigger_delete"):
        db.execute(contracts[k])
    db.execute("INSERT INTO r30_research_freeze VALUES(1,0,0)")
    db.execute("INSERT INTO r27_demo VALUES(1,'a')")
    db.execute("UPDATE r30_research_freeze SET state=1")
    blocked = []
    for sql in ("INSERT INTO r27_demo VALUES(2,'b')", "UPDATE r27_demo SET v='z'", "DELETE FROM r27_demo"):
        try:
            db.execute(sql); blocked.append("ALLOWED")
        except sqlite3.DatabaseError as e:
            blocked.append(str(e))
    check("T03_frozen_blocks_all", all(b == "R30_RESEARCH_TABLE_FROZEN:r27_demo" for b in blocked), str(blocked))
    db.execute("UPDATE r30_research_freeze SET state=2")
    try:
        db.execute("INSERT INTO r27_demo VALUES(3,'c')"); ok = False
    except sqlite3.DatabaseError:
        ok = True
    check("T03_published_state_keeps_root_readonly", ok)
    db.execute("UPDATE r30_research_freeze SET state=0")
    db.execute("INSERT INTO r27_demo VALUES(4,'d')")
    check("T03_unfreeze_restores", db.execute("SELECT count(*) FROM r27_demo").fetchone()[0] == 2)

    # T04 + T05
    db = sqlite3.connect(":memory:")
    db.execute("CREATE TABLE r26_task(id TEXT PRIMARY KEY, s TEXT)")
    db.execute("CREATE TABLE r29_source_watermark(table_name TEXT PRIMARY KEY, generation INTEGER)")
    db.execute("CREATE TABLE r29_guard(active INTEGER)"); db.execute("INSERT INTO r29_guard VALUES(1)")
    names = {
        "r29_copy_track_r26_task_insert": "AFTER INSERT ON r26_task BEGIN UPDATE r29_source_watermark SET generation=generation+1; END",
        "r29_critical_input_write_guard_r26_task": "BEFORE INSERT ON r26_task WHEN (SELECT active FROM r29_guard)=1 BEGIN SELECT RAISE(ABORT,'R29_CRITICAL_INPUT_GUARD'); END",
        "r29_legacy_model_no_update": "BEFORE UPDATE ON r26_task BEGIN SELECT RAISE(ABORT,'CP06_KEEP'); END",
        "r29xcopy_track_lookalike": "AFTER DELETE ON r26_task BEGIN SELECT 1; END",
    }
    for n, body in names.items():
        db.execute(f'CREATE TRIGGER "{n}" {body}')
    try:
        db.execute("INSERT INTO r26_task VALUES('a','x')"); cp06_write = "OK"
    except sqlite3.DatabaseError as e:
        cp06_write = str(e)
    check("T05_r29_guard_breaks_cp06_writes", cp06_write == "R29_CRITICAL_INPUT_GUARD", cp06_write)
    listed = [r[0] for r in db.execute("SELECT name FROM sqlite_master WHERE type='trigger' AND name LIKE 'r29\\_%' ESCAPE '\\'")]
    drop = [n for n in listed if n not in contracts["foldback_keep"] and any(n.startswith(p) for p in contracts["foldback_drop_prefixes"])]
    check("T04_escape_excludes_lookalike", "r29xcopy_track_lookalike" not in listed, str(listed))
    check("T04_drop_set", sorted(drop) == ["r29_copy_track_r26_task_insert", "r29_critical_input_write_guard_r26_task"], str(drop))
    for n in drop:
        db.execute(f'DROP TRIGGER IF EXISTS "{n}"')
    db.execute("INSERT INTO r26_task VALUES('a','x')")
    check("T04_after_drop_cp06_write_ok", db.execute("SELECT count(*) FROM r26_task").fetchone()[0] == 1)

    # T06 复制语义（与 TableCopierR30 同一 SQL 形状）
    src = sqlite3.connect(":memory:"); dst = sqlite3.connect(":memory:")
    src.execute("CREATE TABLE r27_a(id INTEGER PRIMARY KEY, v BLOB, f REAL, s TEXT, extra TEXT)")
    src.execute("CREATE TABLE r27_b(k1 TEXT, k2 INTEGER, v TEXT, PRIMARY KEY(k1,k2)) WITHOUT ROWID")
    for i in range(1, 1234):
        src.execute("INSERT INTO r27_a VALUES(?,?,?,?,?)", (i, bytes([i % 256]), i / 7.0, None if i % 5 == 0 else f"s{i}", "x"))
    for i in range(777):
        src.execute("INSERT INTO r27_b VALUES(?,?,?)", (f"k{i % 13}", i, f"v{i}"))
    dst.execute("CREATE TABLE r27_a(id INTEGER PRIMARY KEY, v BLOB, f REAL, s TEXT)")  # 目标缺 extra 列
    dst.execute("INSERT INTO r27_a VALUES(5,'old',0,'old')")
    def copy(table, rowid):
        scols = [r[1] for r in src.execute(f'PRAGMA table_info("{table}")')]
        dcols = [r[1] for r in dst.execute(f'PRAGMA table_info("{table}")')]
        if not dcols:
            dst.execute(src.execute("SELECT sql FROM sqlite_master WHERE name=?", (table,)).fetchone()[0])
            dcols = scols
        common = [c for c in scols if c in dcols]; missing = [c for c in scols if c not in dcols]
        cols = ",".join(f'"{c}"' for c in common)
        ins = f'INSERT OR REPLACE INTO "{table}"({cols}) VALUES({",".join("?" * len(common))})'
        pk = ",".join(f'"{r[1]}"' for r in sorted((r for r in src.execute(f'PRAGMA table_info("{table}")') if r[5] > 0), key=lambda r: r[5]))
        last, offset, n = -2**63, 0, 0
        while True:
            if rowid:
                page = src.execute(f'SELECT rowid,{cols} FROM "{table}" WHERE rowid>? ORDER BY rowid LIMIT 500', (last,)).fetchall()
            else:
                page = src.execute(f'SELECT 0,{cols} FROM "{table}" ORDER BY {pk} LIMIT 500 OFFSET {offset}').fetchall()
            if not page:
                break
            with dst:
                for r in page:
                    dst.execute(ins, r[1:])
            n += len(page); offset += len(page)
            if rowid:
                last = page[-1][0]
            if len(page) < 500:
                break
        return n, missing
    def chash(con, table, cols, order):
        h = hashlib.sha256()
        for row in con.execute(f'SELECT {",".join(cols)} FROM "{table}" ORDER BY {order}'):
            h.update(repr(row).encode())
        return h.hexdigest()
    na, miss = copy("r27_a", True)
    na2, _ = copy("r27_a", True)  # 重做幂等
    nb, _ = copy("r27_b", False)
    check("T06_rowid_copy_count", na == 1233 and dst.execute("SELECT count(*) FROM r27_a").fetchone()[0] == 1233, f"{na}")
    check("T06_rerun_idempotent", na2 == 1233 and dst.execute("SELECT count(*) FROM r27_a").fetchone()[0] == 1233)
    check("T06_missing_column_reported", miss == ["extra"], str(miss))
    check("T06_replace_overwrites_stale_row", dst.execute("SELECT s FROM r27_a WHERE id=5").fetchone()[0] is None)
    check("T06_without_rowid_pk_paging", nb == 777 and chash(src, "r27_b", ["k1", "k2", "v"], "k1,k2") == chash(dst, "r27_b", ["k1", "k2", "v"], "k1,k2"))
    check("T06_content_hash_equal", chash(src, "r27_a", ["id", "v", "f", "s"], "rowid") == chash(dst, "r27_a", ["id", "v", "f", "s"], "rowid"))
    return results

if __name__ == "__main__":
    contracts = json.load(open(sys.argv[1]))
    res = run(contracts)
    out = {"sqlite_version": sqlite3.sqlite_version, "results": res, "pass": all(r["pass"] for r in res)}
    if "--out" in sys.argv:
        json.dump(out, open(sys.argv[sys.argv.index("--out") + 1], "w"), ensure_ascii=False, indent=1)
    for r in res:
        print(("PASS " if r["pass"] else "FAIL ") + r["test"] + ("" if r["pass"] else "  " + r["detail"]))
    print("SUMMARY", sum(r["pass"] for r in res), "/", len(res))
    sys.exit(0 if out["pass"] else 1)
