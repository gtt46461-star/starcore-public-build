#!/usr/bin/env python3
"""R30-B01-04 从手机导出的数据库文件生成后台长事务清单。
用法：adb exec-out run-as com.starcore.research.r13 cat databases/binance_k3_nova_one_pro_v57.db > db.sqlite
      python3 tools/r30/offenders.py db.sqlite construction/r30/B01_OFFENDERS.json [--hours 24]
（包名按实际安装包；WAL 未合并时同时拷 -wal 文件到同目录）"""
import json, sqlite3, sys, time
db, out = sys.argv[1], sys.argv[2]
hours = float(sys.argv[sys.argv.index("--hours") + 1]) if "--hours" in sys.argv else 24
con = sqlite3.connect(f"file:{db}?mode=ro", uri=True)
since = int(time.time() * 1000 - hours * 3600_000)
rows = con.execute("SELECT operation,sum(count),max(hold_max),max(hold_p95),sum(over_budget),sum(over_hard) FROM r30_writer_hold WHERE critical=0 AND window_end>=? GROUP BY operation HAVING sum(over_budget)>0 OR max(hold_p95)>50 ORDER BY sum(over_hard) DESC,sum(over_budget) DESC", (since,)).fetchall()
crit = con.execute("SELECT operation,sum(count),max(wait_p95),max(wait_max) FROM r30_writer_hold WHERE critical=1 AND window_end>=? GROUP BY operation ORDER BY max(wait_p95) DESC", (since,)).fetchall()
res = {"hours": hours, "background": [dict(zip(["operation", "count", "holdMax", "holdP95", "overBudget50", "overHard200"], r)) for r in rows],
       "critical": [dict(zip(["operation", "count", "waitP95", "waitMax"], r)) for r in crit]}
json.dump(res, open(out, "w"), ensure_ascii=False, indent=1)
print(f"background_offenders={len(rows)} critical_ops={len(crit)}")
